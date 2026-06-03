package com.timeapp.view.timetable;

import com.timeapp.controller.DashboardController;
import com.timeapp.model.*;
import com.timeapp.view.IconLabel;
import javafx.animation.*;
import javafx.geometry.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.shape.*;
import javafx.util.Duration;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Panel 1 — Timetable Main View.
 *
 * Acts as the sub-navigation hub for the timetable section.
 * It owns an internal StackPane that holds all four sub-panels and
 * slides between them in response to {@code ctrl.activeTimetablePaneProperty()}.
 *
 * Layout (self = AnchorPane):
 *   ┌─────────────────────────────────────────┐
 *   │  Top bar:  [☰]  "113-2 Semester Spring" │
 *   │  Week strip (SUN MON TUE WED THU FRI SAT│
 *   ├─────────────────────────────────────────┤
 *   │  Schedule grid (ScrollPane)             │
 *   │   07:00 │  ┌──────────────────────┐     │
 *   │   08:00 │  │ Algorithms  Rm301    │     │
 *   │   09:00 │  └──────────────────────┘     │
 *   │   …                                     │
 *   └─────────────────────────────────────────┘
 *                                    [+] FAB
 *
 * Sub-panel slide:
 *   activeTimetablePaneProperty drives translateX animation
 *   on the sub-panel StackPane children.
 */
public class TimetableMainPane extends AnchorPane {

    private static final double W = 390;
    private static final DateTimeFormatter DAY_ABBR =
            DateTimeFormatter.ofPattern("EEE", Locale.ENGLISH).withLocale(Locale.ENGLISH);

    private final DashboardController ctrl;

    // Sub-panels
    private final StackPane subStack;
    private TimetableAddClassPane addClassPane;
    private TimetableListPane     listPane;
    private TimetableCreatePane   createPane;

    // Active day highlight
    private int activeDayIndex = LocalDate.now().getDayOfWeek().getValue() % 7; // Sun=0

    public TimetableMainPane(DashboardController ctrl) {
        this.ctrl = ctrl;
        getStyleClass().add("page");

        // ── Main content (top bar + week strip + schedule grid) ───────────────
        BorderPane mainContent = buildMainContent();
        AnchorPane.setTopAnchor(mainContent, 0.0);
        AnchorPane.setBottomAnchor(mainContent, 0.0);
        AnchorPane.setLeftAnchor(mainContent, 0.0);
        AnchorPane.setRightAnchor(mainContent, 0.0);
        getChildren().add(mainContent);

        // ── Sub-panel stack ───────────────────────────────────────────────────
        addClassPane = new TimetableAddClassPane(ctrl);
        listPane     = new TimetableListPane(ctrl);
        createPane   = new TimetableCreatePane(ctrl);

        subStack = new StackPane(addClassPane, listPane, createPane);
        subStack.setPrefWidth(W);
        // All sub-panels start off-screen to the right
        addClassPane.setTranslateX(W);
        listPane.setTranslateX(W);
        createPane.setTranslateX(W);

        AnchorPane.setTopAnchor(subStack, 0.0);
        AnchorPane.setBottomAnchor(subStack, 0.0);
        AnchorPane.setLeftAnchor(subStack, 0.0);
        AnchorPane.setRightAnchor(subStack, 0.0);
        getChildren().add(subStack);

        // ── FAB ───────────────────────────────────────────────────────────────
        VBox fabNode = buildTimetableFab();
        AnchorPane.setRightAnchor(fabNode, 20.0);
        AnchorPane.setBottomAnchor(fabNode, 36.0);
        getChildren().add(fabNode);

        // ── Sub-panel routing ─────────────────────────────────────────────────
        ctrl.activeTimetablePaneProperty().addListener((obs, from, to) ->
            slideToPane(from, to));
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Main content
    // ═══════════════════════════════════════════════════════════════════════════

    private BorderPane buildMainContent() {
        BorderPane bp = new BorderPane();
        bp.setTop(buildTopSection());
        bp.setCenter(buildScheduleGrid());
        return bp;
    }

    // ── Top bar ───────────────────────────────────────────────────────────────

    private VBox buildTopSection() {
        VBox top = new VBox();

        // ── Top bar row ───────────────────────────────────────────────────────
        HBox bar = new HBox(12);
        bar.getStyleClass().add("top-bar");
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(16, 20, 12, 20));

        Label hamburgerIcon = IconLabel.of(IconLabel.BARS, 18, "topbar-icon");
        Button hamburger = new Button();
        hamburger.setGraphic(hamburgerIcon);
        hamburger.getStyleClass().add("icon-btn");
        hamburger.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        hamburger.setOnAction(e -> ctrl.toggleSidebar());

        // Timetable name label — tracks active timetable
        Label nameLbl = new Label();
        nameLbl.getStyleClass().add("tt-header-name");
        HBox.setHgrow(nameLbl, Priority.ALWAYS);
        nameLbl.setMaxWidth(Double.MAX_VALUE);
        nameLbl.setAlignment(Pos.CENTER_LEFT);

        // Bind to activeTimetable
        Runnable updateName = () -> {
            TimeTable tt = ctrl.getActiveTimetable();
            nameLbl.setText(tt != null ? tt.getTitle() : "No Timetable");
        };
        updateName.run();
        ctrl.activeTimetableProperty().addListener((obs, o, n) -> updateName.run());

        bar.getChildren().addAll(hamburger, nameLbl);
        top.getChildren().add(bar);

        // ── Week strip (day labels only — no date numbers in timetable view) ──
        top.getChildren().add(buildWeekHeader());

        return top;
    }

    private HBox buildWeekHeader() {
        HBox strip = new HBox(0);
        strip.getStyleClass().add("tt-week-strip");
        strip.setAlignment(Pos.CENTER);

        String[] days = {"SUN", "MON", "TUE", "WED", "THU", "FRI", "SAT"};

        // Left gutter spacer matching schedule grid's time column
        Region gutter = new Region();
        gutter.setPrefWidth(56);
        gutter.setMinWidth(56);
        strip.getChildren().add(gutter);

        for (int i = 0; i < 7; i++) {
            final int idx = i;
            boolean isToday = (i == activeDayIndex);

            Label lbl = new Label(days[i]);
            lbl.getStyleClass().add(isToday ? "tt-day-label-active" : "tt-day-label");
            lbl.setAlignment(Pos.CENTER);
            lbl.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(lbl, Priority.ALWAYS);

            strip.getChildren().add(lbl);
        }
        return strip;
    }

    // ── Schedule grid ─────────────────────────────────────────────────────────

    /**
     * Builds a scrollable week-grid:
     *   Columns: [time label (56px)] [SUN] [MON] [TUE] [WED] [THU] [FRI] [SAT]
     *   Rows:    one per hour from 07:00 to 22:00
     *   Cards:   overlaid at the correct row/column using AnchorPane positioning
     */
    private ScrollPane buildScheduleGrid() {
        final int START_H = 7;
        final int END_H   = 22;
        final double ROW_H    = 56;
        final double COL_W    = (W - 56) / 7.0;
        final double TOTAL_H  = (END_H - START_H + 1) * ROW_H + 16;

        AnchorPane canvas = new AnchorPane();
        canvas.getStyleClass().add("tt-grid-canvas");
        canvas.setPrefHeight(TOTAL_H);
        canvas.setPrefWidth(W);

        // ── Hour rows & labels ────────────────────────────────────────────────
        for (int h = START_H; h <= END_H; h++) {
            double y = (h - START_H) * ROW_H + 8;

            Label hourLbl = new Label(String.format("%02d:00", h));
            hourLbl.getStyleClass().add("hour-label");
            hourLbl.setPrefWidth(52);
            hourLbl.setAlignment(Pos.CENTER_RIGHT);
            AnchorPane.setTopAnchor(hourLbl, y - 8);
            AnchorPane.setLeftAnchor(hourLbl, 0.0);
            canvas.getChildren().add(hourLbl);

            Line hLine = new Line(56, y, W, y);
            hLine.getStyleClass().add("tt-grid-hline");
            canvas.getChildren().add(hLine);
        }

        // ── Vertical column separators ────────────────────────────────────────
        for (int d = 0; d <= 7; d++) {
            double x = 56 + d * COL_W;
            Line vLine = new Line(x, 8, x, TOTAL_H - 8);
            vLine.getStyleClass().add(d == activeDayIndex || d == activeDayIndex + 1
                ? "tt-grid-vline-today" : "tt-grid-vline");
            canvas.getChildren().add(vLine);
        }

        // ── Class cards ───────────────────────────────────────────────────────
        renderClassCards(canvas, START_H, ROW_H, COL_W);

        // Observe class list changes to re-render
        TimeTable tt = ctrl.getActiveTimetable();
        if (tt != null) {
            tt.getClasses().addListener((javafx.collections.ListChangeListener<TimetableClassRecord>) c -> {
                canvas.getChildren().removeIf(n -> Boolean.TRUE.equals(n.getUserData()));
                renderClassCards(canvas, START_H, ROW_H, COL_W);
            });
        }
        ctrl.activeTimetableProperty().addListener((obs, o, newTt) -> {
            canvas.getChildren().removeIf(n -> Boolean.TRUE.equals(n.getUserData()));
            if (newTt != null) {
                renderClassCards(canvas, START_H, ROW_H, COL_W);
            }
        });

        ScrollPane sp = new ScrollPane(canvas);
        sp.setFitToWidth(true);
        sp.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        sp.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        sp.getStyleClass().add("timeline-scroll");
        sp.setVvalue(0.1);
        return sp;
    }

    private void renderClassCards(AnchorPane canvas, int startH,
                                   double rowH, double colW) {
        TimeTable tt = ctrl.getActiveTimetable();
        if (tt == null) return;

        for (TimetableClassRecord rec : tt.getClasses()) {
            TimetableClassRecord.AccentColor color = rec.getAccentColor();
            for (TimetableClassRecord.ClassTimeSlot slot : rec.getTimeSlots()) {
                int dayCol = slot.getDayOfWeek().getValue() % 7; // Sun=0
                double x = 56 + dayCol * colW + 2;
                double y = (slot.getStartTime().getHour() - startH
                            + slot.getStartTime().getMinute() / 60.0) * rowH + 8;
                double h = Math.max(
                    (slot.getEndTime().getHour() - slot.getStartTime().getHour()
                     + (slot.getEndTime().getMinute() - slot.getStartTime().getMinute()) / 60.0)
                    * rowH - 4, 32);

                VBox card = buildGridCard(rec, slot, color, colW - 4, h);
                card.setUserData(Boolean.TRUE); // mark for removal on re-render
                AnchorPane.setTopAnchor(card, y);
                AnchorPane.setLeftAnchor(card, x);
                canvas.getChildren().add(card);
            }
        }
    }

    private VBox buildGridCard(TimetableClassRecord rec,
                                TimetableClassRecord.ClassTimeSlot slot,
                                TimetableClassRecord.AccentColor color,
                                double w, double h) {
        VBox card = new VBox(2);
        card.getStyleClass().add("tt-grid-card");
        card.setStyle(
            "-fx-background-color: " + color.bg + ";" +
            "-fx-border-color: " + color.strip + ";" +
            "-fx-border-radius: 6;" +
            "-fx-background-radius: 6;" +
            "-fx-padding: 4 6 4 6;"
        );
        card.setPrefWidth(w);
        card.setPrefHeight(h);
        card.setMaxHeight(h);

        Label subjectLbl = new Label(rec.getSubject());
        subjectLbl.getStyleClass().add("tt-card-subject");
        subjectLbl.setWrapText(true);
        subjectLbl.setStyle("-fx-text-fill: " + color.strip + "; -fx-font-size: 10px; -fx-font-weight: bold;");

        Label roomLbl = new Label(rec.getClassroom());
        roomLbl.setStyle("-fx-text-fill: #666666; -fx-font-size: 9px;");

        card.getChildren().addAll(subjectLbl, roomLbl);
        return card;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Timetable FAB (own speed-dial: "TimeTable" + "Add Class")
    // ═══════════════════════════════════════════════════════════════════════════

    private VBox buildTimetableFab() {
        // Mini option: "TimeTable"
        Button btnTimeTable = ttMiniFab(IconLabel.CALENDAR, "TimeTable");
        btnTimeTable.setVisible(false);
        btnTimeTable.setManaged(false);

        // Mini option: "Add Class"
        Button btnAddClass = ttMiniFab(IconLabel.PLUS, "Add Class");
        btnAddClass.setVisible(false);
        btnAddClass.setManaged(false);

        btnTimeTable.setOnAction(e -> { collapseTtFab(btnTimeTable, btnAddClass, mainFabRef[0]); ctrl.showTimetableList(); });
        btnAddClass.setOnAction(e -> { collapseTtFab(btnTimeTable, btnAddClass, mainFabRef[0]); ctrl.showAddClassPane(); });

        // Main FAB
        Label plusIcon = IconLabel.of(IconLabel.PLUS, 20, "fab-main-icon");
        Button mainFab = new Button();
        mainFab.setGraphic(plusIcon);
        mainFab.getStyleClass().add("fab-main");
        mainFab.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);

        mainFabRef[0] = mainFab;
        mainFab.setOnAction(e -> toggleTtFab(btnTimeTable, btnAddClass, plusIcon, mainFab));

        VBox root = new VBox(8);
        root.setAlignment(Pos.BOTTOM_RIGHT);
        root.getChildren().addAll(btnTimeTable, btnAddClass, mainFab);
        return root;
    }

    // Store main fab reference for use in lambda
    private final Button[] mainFabRef = new Button[1];
    private boolean ttFabExpanded = false;

    private void toggleTtFab(Button b1, Button b2, Label icon, Button main) {
        ttFabExpanded = !ttFabExpanded;
        if (ttFabExpanded) {
            icon.setText(IconLabel.XMARK);
            icon.setStyle("-fx-font-family: '" + com.timeapp.MainApp.FA_SOLID_FAMILY
                + "'; -fx-font-size: 20px;");
            main.getStyleClass().add("fab-main-active");
            showTtMini(b1, 0); showTtMini(b2, 80);
        } else {
            collapseTtFab(b1, b2, main);
        }
    }

    private void collapseTtFab(Button b1, Button b2, Button main) {
        ttFabExpanded = false;
        if (main != null) {
            main.getStyleClass().remove("fab-main-active");
            Label icon = (Label) main.getGraphic();
            if (icon != null) {
                icon.setText(IconLabel.PLUS);
                icon.setStyle("-fx-font-family: '" + com.timeapp.MainApp.FA_SOLID_FAMILY
                    + "'; -fx-font-size: 20px;");
            }
        }
        hideTtMini(b1); hideTtMini(b2);
    }

    private void showTtMini(Button btn, int delay) {
        btn.setVisible(true); btn.setManaged(true);
        btn.setOpacity(0); btn.setScaleX(0.6); btn.setScaleY(0.6);
        new Timeline(
            new KeyFrame(Duration.millis(delay),
                new KeyValue(btn.opacityProperty(), 0)),
            new KeyFrame(Duration.millis(delay + 180),
                new KeyValue(btn.opacityProperty(), 1, Interpolator.EASE_OUT),
                new KeyValue(btn.scaleXProperty(), 1, Interpolator.EASE_OUT),
                new KeyValue(btn.scaleYProperty(), 1, Interpolator.EASE_OUT))
        ).play();
    }

    private void hideTtMini(Button btn) {
        Timeline tl = new Timeline(new KeyFrame(Duration.millis(140),
            new KeyValue(btn.opacityProperty(), 0, Interpolator.EASE_IN),
            new KeyValue(btn.scaleXProperty(), 0.6, Interpolator.EASE_IN),
            new KeyValue(btn.scaleYProperty(), 0.6, Interpolator.EASE_IN)));
        tl.setOnFinished(e -> { btn.setVisible(false); btn.setManaged(false); });
        tl.play();
    }

    private Button ttMiniFab(String codepoint, String text) {
        HBox c = new HBox(8);
        c.setAlignment(Pos.CENTER_RIGHT);
        c.getChildren().addAll(new Label(text) {{ getStyleClass().add("fab-mini-label"); }},
                               IconLabel.of(codepoint, 14, "fab-mini-icon"));
        Button btn = new Button();
        btn.setGraphic(c);
        btn.getStyleClass().add("fab-mini");
        btn.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        return btn;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Sub-panel sliding animation
    // ═══════════════════════════════════════════════════════════════════════════

    private void slideToPane(String from, String to) {
        Region enter = paneFor(to);
        Region leave = paneFor(from);
        if (enter == null || enter == leave) return;

        boolean toRight = isForward(from, to); // true = entering slides from right
        double dir = toRight ? W : -W;

        enter.setTranslateX(dir);
        enter.setVisible(true);
        enter.setManaged(true);

        new Timeline(
            new KeyFrame(Duration.millis(280),
                new KeyValue(enter.translateXProperty(), 0,    Interpolator.EASE_BOTH),
                new KeyValue(leave.translateXProperty(), -dir, Interpolator.EASE_BOTH))
        ).play();
    }

    /** "MAIN" is the root; ADD/LIST/CREATE are forward from it */
    private boolean isForward(String from, String to) {
        int fromIdx = paneIndex(from);
        int toIdx   = paneIndex(to);
        return toIdx > fromIdx;
    }

    private int paneIndex(String name) {
        return switch (name) {
            case "MAIN"   -> 0;
            case "ADD"    -> 1;
            case "LIST"   -> 2;
            case "CREATE" -> 3;
            default       -> 0;
        };
    }

    private Region paneFor(String name) {
        return switch (name) {
            case "ADD"    -> addClassPane;
            case "LIST"   -> listPane;
            case "CREATE" -> createPane;
            default       -> null; // "MAIN" = base content, not a sub-panel
        };
    }
}
