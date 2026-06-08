package com.timeapp.view;

import com.timeapp.model.*;
import javafx.collections.*;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.shape.*;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Scrollable vertical timeline showing hour markers and event cards.
 *
 * ── Change from previous version ─────────────────────────────────────────────
 * TimeEntry.getStartTime() / getEndTime() now return LocalDateTime.
 * All calls that previously passed LocalTime to timeToY() and the
 * formatter now extract the LocalTime component via .toLocalTime().
 * No layout math changes — ROW_H, GUTTER, PADDING are unchanged.
 * ─────────────────────────────────────────────────────────────────────────────
 */
public class TimelinePane {

    public static final double ROW_H   = 64;
    public static final double GUTTER  = 56;
    public static final double PADDING = 8;
    private static final int   START_H = 7;
    private static final int   END_H   = 23;

    private static final DateTimeFormatter T_FMT =
            DateTimeFormatter.ofPattern("HH:mm");

    private final ScrollPane scrollPane;
    private final AnchorPane canvas;
    private final double     totalWidth;

    public TimelinePane(double totalWidth, ObservableList<TimeEntry> entries) {
        this.totalWidth = totalWidth;

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
                Line half = new Line(GUTTER, y + ROW_H / 2, totalWidth - 8, y + ROW_H / 2);
                half.getStyleClass().add("half-hour-line");
                half.setUserData("hour-row");
                canvas.getChildren().add(half);
            }
            canvas.getChildren().addAll(lbl, line);
        }
    }

    // ── Entry rendering ───────────────────────────────────────────────────────

    private void renderEntries(List<TimeEntry> entries) {
        for (TimeEntry entry : entries) {
            javafx.scene.Node node = switch (entry.getType()) {
                case TODO            -> buildTodoNode((TodoEntry) entry);
                case CALENDAR_EVENT  -> buildCalendarCard((CalendarEvent) entry);
                case TIMETABLE_CLASS -> buildTimetableCard((TimetableClass) entry);
            };

            // Use toLocalTime() to extract time component from LocalDateTime
            double y = timeToY(entry.getStartTime().toLocalTime());
            double h = Math.max(minutesToPx(entry.getDurationMinutes()), 44);
            double x = GUTTER + 4;
            double w = totalWidth - x - 8;

            AnchorPane.setLeftAnchor(node, x);
            AnchorPane.setTopAnchor(node, y);
            if (node instanceof Region r) {
                r.setPrefWidth(w);
                r.setPrefHeight(h);
                r.setMaxHeight(h);
            }
            canvas.getChildren().add(node);
        }
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

        // Format LocalDateTime → "HH:mm"
        Label timeLabel = new Label(todo.getStartTime().toLocalTime().format(T_FMT));
        timeLabel.getStyleClass().add("todo-time");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        box.getChildren().addAll(cb, title, spacer, timeLabel);
        return box;
    }

    // ── Calendar event card ───────────────────────────────────────────────────

    private javafx.scene.Node buildCalendarCard(CalendarEvent ev) {
        VBox card = new VBox(3);
        card.getStyleClass().addAll("event-card", "event-card-blue");
        card.setPadding(new Insets(8, 10, 8, 12));

        // Format LocalDateTime → "HH:mm"
        Label timeRange = new Label(
            ev.getStartTime().toLocalTime().format(T_FMT)
            + " – " + ev.getEndTime().toLocalTime().format(T_FMT));
        timeRange.getStyleClass().add("card-time-range");

        Label title = new Label(ev.getTitle());
        title.getStyleClass().add("card-title");
        title.setWrapText(true);

        HBox locRow = new HBox(4);
        locRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        Label pin = new Label("📍");
        pin.setStyle("-fx-font-size: 10px;");
        Label loc = new Label(ev.getLocation() != null ? ev.getLocation() : "");
        loc.getStyleClass().add("card-location");
        locRow.getChildren().addAll(pin, loc);

        card.getChildren().addAll(timeRange, title, locRow);

        Rectangle strip = new Rectangle(4, 0);
        strip.getStyleClass().add("card-strip-blue");
        strip.heightProperty().bind(card.heightProperty());

        HBox outer = new HBox();
        outer.getStyleClass().add("card-outer");
        outer.getChildren().addAll(strip, card);
        HBox.setHgrow(card, Priority.ALWAYS);
        return outer;
    }

    // ── Timetable class card ──────────────────────────────────────────────────

    private javafx.scene.Node buildTimetableCard(TimetableClass cls) {
        VBox card = new VBox(3);
        card.getStyleClass().addAll("event-card", "event-card-purple");
        card.setPadding(new Insets(8, 10, 8, 12));

        // Format LocalDateTime → "HH:mm"
        Label timeRange = new Label(
            cls.getStartTime().toLocalTime().format(T_FMT)
            + " – " + cls.getEndTime().toLocalTime().format(T_FMT));
        timeRange.getStyleClass().add("card-time-range");

        Label title = new Label(cls.getTitle());
        title.getStyleClass().add("card-title");
        title.setWrapText(true);

        HBox locRow = new HBox(4);
        locRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        Label pin = new Label("🏫");
        pin.setStyle("-fx-font-size: 10px;");
        Label loc = new Label(cls.getRoom() != null ? cls.getRoom() : "");
        loc.getStyleClass().add("card-location");
        locRow.getChildren().addAll(pin, loc);

        HBox profRow = new HBox(4);
        profRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        Label personIcon = new Label("👤");
        personIcon.setStyle("-fx-font-size: 10px;");
        Label prof = new Label(cls.getTeacher() != null ? cls.getTeacher() : "");
        prof.getStyleClass().add("card-professor");
        profRow.getChildren().addAll(personIcon, prof);

        card.getChildren().addAll(timeRange, title, locRow, profRow);

        Rectangle strip = new Rectangle(4, 0);
        strip.getStyleClass().add("card-strip-purple");
        strip.heightProperty().bind(card.heightProperty());

        HBox outer = new HBox();
        outer.getStyleClass().add("card-outer");
        outer.getChildren().addAll(strip, card);
        HBox.setHgrow(card, Priority.ALWAYS);
        return outer;
    }

    // ── Conversion helpers ────────────────────────────────────────────────────

    /** Converts LocalTime to a Y-pixel position on the canvas. */
    private double timeToY(LocalTime t) {
        double hours = t.getHour() + t.getMinute() / 60.0;
        return PADDING + (hours - START_H) * ROW_H;
    }

    private double minutesToPx(long minutes) {
        return minutes / 60.0 * ROW_H;
    }
}
