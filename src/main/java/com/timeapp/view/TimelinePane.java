package com.timeapp.view;

import com.timeapp.controller.DashboardController;
import com.timeapp.ui.model.*;
import javafx.collections.*;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.shape.*;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * Scrollable vertical timeline showing hour markers and event cards.
 *
 * FIX (v10):
 *   - Time range separator changed from en-dash (U+2013) to " - " (ASCII hyphen).
 *     The en-dash was saved as "??" in the source file due to a Windows encoding
 *     issue and rendered as question marks at runtime.
 *   - Location / professor / pin emoji labels (which showed as "?") replaced by
 *     FontAwesome glyph icons via IconLabel.of(). FA glyphs are always available
 *     because fa-solid-900.ttf is loaded programmatically in MainApp.start().
 *
 * Layout:
 *   ScrollPane -> AnchorPane (canvas)
 *     Hour labels + divider lines   (userData = "hour-row")
 *     TimeEntry cards               (positioned by timeToY())
 *
 * Pixel math:
 *   ROW_H   = 64 px per 60 minutes
 *   GUTTER  = 56 px (left column for hour labels)
 *   PADDING =  8 px (top gap before first label)
 *   START_H =  7    (07:00 is the first visible hour)
 */
public class TimelinePane {

    public static final double ROW_H   = 64;
    public static final double GUTTER  = 56;
    public static final double PADDING = 8;
    private static final double CARD_GAP = 6;
    private static final double TODO_WIDTH = 150;
    private static final double TODO_HEIGHT = 30;
    private static final double TODO_STACK_OFFSET = 34;
    private static final int   START_H = 7;
    private static final int   END_H   = 23;

    private static final DateTimeFormatter T_FMT =
            DateTimeFormatter.ofPattern("HH:mm");

    private final ScrollPane scrollPane;
    private final AnchorPane canvas;
    private final double     totalWidth;
    private final DashboardController ctrl;
    private final List<javafx.scene.Node> todoNodes = new ArrayList<>();
    private javafx.scene.Node elevatedEventNode;

    public TimelinePane(double totalWidth, ObservableList<TimeEntry> entries) {
        this(totalWidth, entries, null);
    }

    public TimelinePane(double totalWidth, ObservableList<TimeEntry> entries,
                        DashboardController ctrl) {
        this.totalWidth = totalWidth;
        this.ctrl = ctrl;

        canvas = new AnchorPane();
        canvas.getStyleClass().add("timeline-canvas");
        canvas.setPrefWidth(totalWidth);
        canvas.setPrefHeight(PADDING + (END_H - START_H + 1) * ROW_H + 32);

        buildHourGrid();
        renderEntries(entries);

        entries.addListener((ListChangeListener<TimeEntry>) c -> {
            canvas.getChildren().removeIf(n -> !"hour-row".equals(n.getUserData()));
            renderEntries(entries);
        });

        scrollPane = new ScrollPane(canvas);
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.getStyleClass().add("timeline-scroll");
        scrollPane.setVvalue(0.1);
    }

    public ScrollPane getNode() { return scrollPane; }

    // ── Hour grid ─────────────────────────────────────────────────────────────

    private void buildHourGrid() {
        for (int h = START_H; h <= END_H; h++) {
            double y = PADDING + (h - START_H) * ROW_H;

            Label lbl = new Label(String.format("%02d:00", h));
            lbl.getStyleClass().add("hour-label");
            lbl.setPrefWidth(GUTTER - 4);
            lbl.setLayoutX(4);
            lbl.setLayoutY(y - 8);
            lbl.setUserData("hour-row");

            Line line = new Line(GUTTER, y, totalWidth - 8, y);
            line.getStyleClass().add("hour-line");
            line.setUserData("hour-row");

            if (h < END_H) {
                Line halfLine = new Line(GUTTER, y + ROW_H / 2, totalWidth - 8, y + ROW_H / 2);
                halfLine.getStyleClass().add("half-hour-line");
                halfLine.setUserData("hour-row");
                canvas.getChildren().add(halfLine);
            }

            canvas.getChildren().addAll(lbl, line);
        }
    }

    // ── Entry rendering ───────────────────────────────────────────────────────

    private void renderEntries(List<TimeEntry> entries) {
        todoNodes.clear();
        elevatedEventNode = null;

        List<TimeEntry> events = new ArrayList<>();
        List<TimeEntry> todos = new ArrayList<>();

        for (TimeEntry entry : entries) {
            if (entry.getType() == TimeEntry.Type.TODO) {
                todos.add(entry);
            } else {
                events.add(entry);
            }
        }

        renderEventEntries(events);
        renderTodoEntries(todos);
    }

    private void renderEventEntries(List<TimeEntry> entries) {
        Map<TimeEntry, Placement> placements = computeOverlapPlacements(entries);

        for (TimeEntry entry : entries) {
            javafx.scene.Node node = switch (entry.getType()) {
                case CALENDAR_EVENT  -> buildCalendarCard((CalendarEvent) entry);
                case TIMETABLE_CLASS -> buildTimetableCard((TimetableClass) entry);
                default -> throw new IllegalStateException("Unexpected event type: " + entry.getType());
            };

            Placement placement = placements.getOrDefault(entry, new Placement(0, 1));
            double y = timeToY(entry.getStartTime());
            double h = Math.max(minutesToPx(entry.getDurationMinutes()), 44);
            double baseX = GUTTER + 4;
            double availableWidth = totalWidth - baseX - 8;
            double columnWidth = (availableWidth - CARD_GAP * (placement.columnCount - 1))
                    / placement.columnCount;
            double x = baseX + placement.column * (columnWidth + CARD_GAP);
            double w = Math.max(columnWidth, 44);

            AnchorPane.setLeftAnchor(node, x);
            AnchorPane.setTopAnchor(node, y);
            installCardInteractions(node, entry);
            if (node instanceof Region r) {
                r.setPrefWidth(w);
                r.setPrefHeight(h);
                r.setMaxHeight(h);
            }
            canvas.getChildren().add(node);
        }
    }

    private void renderTodoEntries(List<TimeEntry> todos) {
        todos.sort(Comparator
            .comparing(TimeEntry::getStartTime)
            .thenComparing(TimeEntry::getTitle, Comparator.nullsLast(String::compareTo)));

        Map<LocalTime, Integer> stackedAtTime = new java.util.HashMap<>();
        for (TimeEntry entry : todos) {
            javafx.scene.Node node = buildTodoNode((TodoEntry) entry);
            double y = timeToY(entry.getStartTime());
            int stackIndex = stackedAtTime.getOrDefault(entry.getStartTime(), 0);
            stackedAtTime.put(entry.getStartTime(), stackIndex + 1);

            double w = Math.min(TODO_WIDTH, totalWidth - GUTTER - 16);
            double x = totalWidth - w - 18;

            AnchorPane.setLeftAnchor(node, x);
            AnchorPane.setTopAnchor(node, y + stackIndex * TODO_STACK_OFFSET);
            installCardInteractions(node, entry);
            if (node instanceof Region r) {
                r.setPrefWidth(w);
                r.setPrefHeight(TODO_HEIGHT);
                r.setMaxHeight(TODO_HEIGHT);
            }
            canvas.getChildren().add(node);
            todoNodes.add(node);
        }
    }

    private Map<TimeEntry, Placement> computeOverlapPlacements(List<TimeEntry> entries) {
        List<TimeEntry> sorted = new ArrayList<>(entries);
        sorted.sort(Comparator
            .comparing(TimeEntry::getStartTime)
            .thenComparing(TimeEntry::getEndTime));

        Map<TimeEntry, Placement> placements = new IdentityHashMap<>();
        List<TimeEntry> group = new ArrayList<>();
        LocalTime groupEnd = null;

        for (TimeEntry entry : sorted) {
            if (group.isEmpty()) {
                group.add(entry);
                groupEnd = entry.getEndTime();
                continue;
            }

            if (entry.getStartTime().isBefore(groupEnd)) {
                group.add(entry);
                if (entry.getEndTime().isAfter(groupEnd)) {
                    groupEnd = entry.getEndTime();
                }
            } else {
                assignGroupPlacements(group, placements);
                group.clear();
                group.add(entry);
                groupEnd = entry.getEndTime();
            }
        }

        if (!group.isEmpty()) {
            assignGroupPlacements(group, placements);
        }
        return placements;
    }

    private void assignGroupPlacements(List<TimeEntry> group,
                                       Map<TimeEntry, Placement> placements) {
        List<LocalTime> columnEnds = new ArrayList<>();
        Map<TimeEntry, Integer> assignedColumns = new IdentityHashMap<>();

        for (TimeEntry entry : group) {
            int column = firstAvailableColumn(entry, columnEnds);
            if (column == columnEnds.size()) {
                columnEnds.add(entry.getEndTime());
            } else {
                columnEnds.set(column, entry.getEndTime());
            }
            assignedColumns.put(entry, column);
        }

        int columnCount = Math.max(columnEnds.size(), 1);
        for (Map.Entry<TimeEntry, Integer> assignment : assignedColumns.entrySet()) {
            placements.put(assignment.getKey(),
                new Placement(assignment.getValue(), columnCount));
        }
    }

    private int firstAvailableColumn(TimeEntry entry, List<LocalTime> columnEnds) {
        for (int i = 0; i < columnEnds.size(); i++) {
            if (!entry.getStartTime().isBefore(columnEnds.get(i))) {
                return i;
            }
        }
        return columnEnds.size();
    }

    private static final class Placement {
        final int column;
        final int columnCount;

        Placement(int column, int columnCount) {
            this.column = column;
            this.columnCount = columnCount;
        }
    }

    private void installCardInteractions(javafx.scene.Node node, TimeEntry entry) {
        node.setOnMouseClicked(e -> {
            if (entry.getType() == TimeEntry.Type.TODO) {
                node.toFront();
                elevatedEventNode = null;
            } else if (elevatedEventNode == node) {
                bringTodosToFront();
                elevatedEventNode = null;
            } else {
                node.toFront();
                elevatedEventNode = node;
            }
        });

        ContextMenu menu = contextMenuFor(entry);
        if (menu != null) {
            node.setOnContextMenuRequested(e -> {
                node.toFront();
                elevatedEventNode = entry.getType() == TimeEntry.Type.TODO ? null : node;
                menu.show(node, e.getScreenX(), e.getScreenY());
                e.consume();
            });
        }
    }

    private void bringTodosToFront() {
        for (javafx.scene.Node todoNode : todoNodes) {
            todoNode.toFront();
        }
    }

    private ContextMenu contextMenuFor(TimeEntry entry) {
        if (ctrl == null) {
            return null;
        }

        if (entry instanceof TodoEntry todo && todo.getSourceTask() != null) {
            MenuItem edit = new MenuItem("Edit To-Do");
            edit.setOnAction(e -> ctrl.editTodo(todo.getSourceTask()));
            MenuItem delete = new MenuItem("Delete To-Do");
            delete.setOnAction(e -> ctrl.deleteTodo(todo.getSourceTask()));
            return new ContextMenu(edit, delete);
        }

        if (entry instanceof CalendarEvent event && event.getSourceEvent() != null) {
            MenuItem edit = new MenuItem("Edit Schedule");
            edit.setOnAction(e -> ctrl.editSchedule(event.getSourceEvent()));
            MenuItem delete = new MenuItem("Delete Schedule");
            delete.setOnAction(e -> ctrl.deleteSchedule(event.getSourceEvent()));
            return new ContextMenu(edit, delete);
        }

        return null;
    }

    // ── Todo node ─────────────────────────────────────────────────────────────

    private javafx.scene.Node buildTodoNode(TodoEntry todo) {
        HBox box = new HBox(10);
        box.getStyleClass().add("todo-row");
        box.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        CheckBox cb = new CheckBox();
        cb.getStyleClass().add("todo-checkbox");
        cb.selectedProperty().bindBidirectional(todo.completedProperty());

        Label title = new Label(todo.getTitle());
        title.getStyleClass().add("todo-title");
        title.setWrapText(true);
        todo.completedProperty().addListener((obs, o, n) ->
            title.setStyle(n ? "-fx-strikethrough: true; -fx-opacity: 0.45;" : ""));

        HBox.setHgrow(title, Priority.ALWAYS);
        box.getChildren().addAll(cb, title);
        return box;
    }

    // ── Calendar event card ───────────────────────────────────────────────────

    private javafx.scene.Node buildCalendarCard(CalendarEvent ev) {
        VBox card = new VBox(3);
        card.getStyleClass().addAll("event-card", "event-card-blue");
        card.setPadding(new Insets(8, 10, 8, 12));

        // FIX: use ASCII " - " (hyphen) — en-dash caused "??" rendering on Windows
        Label timeRange = new Label(
            ev.getStartTime().format(T_FMT) + " - " + ev.getEndTime().format(T_FMT));
        timeRange.getStyleClass().add("card-time-range");

        Label title = new Label(ev.getTitle());
        title.getStyleClass().add("card-title");
        title.setWrapText(true);

        // FIX: FA glyph icon replaces emoji "??" (map-marker \uf041)
        HBox locRow = buildIconRow(
            IconLabel.of(IconLabel.MAP_MARKER, 10, "card-icon"),
            ev.getLocation(),
            "card-location");

        card.getChildren().addAll(timeRange, title, locRow);

        Rectangle strip = new Rectangle(4, 0);
        strip.getStyleClass().add("card-strip-blue");
        strip.heightProperty().bind(card.heightProperty());

        HBox outer = new HBox();
        outer.getStyleClass().add("card-outer");
        outer.getChildren().addAll(strip, card);
        HBox.setHgrow(card, Priority.ALWAYS);
        outer.setStyle("-fx-background-radius: 10; -fx-background-color: transparent;");
        return outer;
    }

    // ── Timetable class card ──────────────────────────────────────────────────

    private javafx.scene.Node buildTimetableCard(TimetableClass cls) {
        VBox card = new VBox(3);
        card.getStyleClass().addAll("event-card", "event-card-purple");
        card.setPadding(new Insets(8, 10, 8, 12));

        // FIX: use ASCII " - " (hyphen)
        Label timeRange = new Label(
            cls.getStartTime().format(T_FMT) + " - " + cls.getEndTime().format(T_FMT));
        timeRange.getStyleClass().add("card-time-range");

        Label title = new Label(cls.getTitle());
        title.getStyleClass().add("card-title");
        title.setWrapText(true);

        // FIX: FA glyph icons replace broken emoji "?" characters
        HBox locRow  = buildIconRow(
            IconLabel.of(IconLabel.MAP_MARKER, 10, "card-icon"),
            cls.getLocation(), "card-location");
        HBox profRow = buildIconRow(
            IconLabel.of(IconLabel.USER,       10, "card-icon"),
            cls.getProfessor(), "card-professor");

        card.getChildren().addAll(timeRange, title, locRow, profRow);

        Rectangle strip = new Rectangle(4, 0);
        strip.getStyleClass().add("card-strip-purple");
        strip.heightProperty().bind(card.heightProperty());

        HBox outer = new HBox();
        outer.getStyleClass().add("card-outer");
        outer.getChildren().addAll(strip, card);
        HBox.setHgrow(card, Priority.ALWAYS);
        outer.setStyle("-fx-background-radius: 10; -fx-background-color: transparent;");
        return outer;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /** Builds an icon + text row. iconLabel should come from IconLabel.of(). */
    private HBox buildIconRow(Label iconLabel, String text, String textStyleClass) {
        HBox row = new HBox(4);
        row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        Label textLbl = new Label(text != null ? text : "");
        textLbl.getStyleClass().add(textStyleClass);
        row.getChildren().addAll(iconLabel, textLbl);
        return row;
    }

    private double timeToY(LocalTime t) {
        double hours = t.getHour() + t.getMinute() / 60.0;
        return PADDING + (hours - START_H) * ROW_H;
    }

    private double minutesToPx(long minutes) {
        return minutes / 60.0 * ROW_H;
    }
}
