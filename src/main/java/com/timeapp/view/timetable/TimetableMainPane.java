package com.timeapp.view.timetable;

import com.timeapp.controller.DashboardController;
import com.timeapp.ui.model.*;
import com.timeapp.view.IconLabel;
import javafx.animation.*;
import javafx.collections.ListChangeListener;
import javafx.geometry.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.shape.*;
import javafx.util.Duration;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Panel 1 ??Timetable Main View.
 *
 * Acts as the sub-navigation hub for the timetable section.
 * Owns an internal StackPane that holds all three sub-panels (Panels 2/3/4)
 * and slides between them in response to ctrl.activeTimetablePaneProperty().
 *
 * Sub-panels start off-screen (translateX = +W) and are animated into view.
 * When returning to MAIN they slide back out and are hidden.
 *
 * ?? Forward-reference fix ?????????????????????????????????????????????????????
 * In buildTimetableFab(), both mini-buttons are declared BEFORE their lambda
 * actions are wired, eliminating the "cannot find symbol" compile error that
 * occurs when a lambda captures a local variable not yet in scope.
 * ?????????????????????????????????????????????????????????????????????????????
 */
public class TimetableMainPane extends AnchorPane {

    private static final double W = 390;
    private static final DateTimeFormatter DAY_ABBR =
            DateTimeFormatter.ofPattern("EEE", Locale.ENGLISH).withLocale(Locale.ENGLISH);

    private final DashboardController ctrl;

    // Sub-panels (Panels 2, 3, 4)
    private final StackPane           subStack;
    private final TimetableAddClassPane addClassPane;
    private final TimetableListPane     listPane;
    private final TimetableCreatePane   createPane;

    // Today's day-of-week index (Sun = 0, Mon = 1, ??Sat = 6)
    private final int activeDayIndex = LocalDate.now().getDayOfWeek().getValue() % 7;

    // FAB state
    private final Button[] mainFabRef  = new Button[1];
    private boolean         ttFabExpanded = false;
    private ListChangeListener<TimetableClassRecord> activeClassListListener;

    public TimetableMainPane(DashboardController ctrl) {
        this.ctrl = ctrl;
        getStyleClass().add("page");

        // ?? Base content (always visible) ?????????????????????????????????????
        BorderPane mainContent = buildMainContent();
        AnchorPane.setTopAnchor(mainContent,    0.0);
        AnchorPane.setBottomAnchor(mainContent, 0.0);
        AnchorPane.setLeftAnchor(mainContent,   0.0);
        AnchorPane.setRightAnchor(mainContent,  0.0);
        getChildren().add(mainContent);

        // ?? Sub-panels ????????????????????????????????????????????????????????
        addClassPane = new TimetableAddClassPane(ctrl);
        listPane     = new TimetableListPane(ctrl);
        createPane   = new TimetableCreatePane(ctrl);

        // All start hidden and translated off-screen to the right
        for (Region pane : new Region[]{addClassPane, listPane, createPane}) {
            pane.setVisible(false);
            pane.setManaged(false);
            pane.setTranslateX(W);
        }

        subStack = new StackPane(addClassPane, listPane, createPane);
        subStack.setPrefWidth(W);
        subStack.setPickOnBounds(false);  // don't block clicks on main content when hidden

        AnchorPane.setTopAnchor(subStack,    0.0);
        AnchorPane.setBottomAnchor(subStack, 0.0);
        AnchorPane.setLeftAnchor(subStack,   0.0);
        AnchorPane.setRightAnchor(subStack,  0.0);
        getChildren().add(subStack);

        // ?? FAB ???????????????????????????????????????????????????????????????
        VBox fabNode = buildTimetableFab();
        AnchorPane.setRightAnchor(fabNode,  20.0);
        AnchorPane.setBottomAnchor(fabNode, 36.0);
        getChildren().add(fabNode);

        // ?? Sub-panel routing ?????????????????????????????????????????????????
        ctrl.activeTimetablePaneProperty().addListener((obs, from, to) ->
            slideToPane(from, to));
    }

    // ????????????????????????????????????????????????????????????????????????????
    // Main content (top bar + week header + schedule grid)
    // ????????????????????????????????????????????????????????????????????????????

    private BorderPane buildMainContent() {
        BorderPane bp = new BorderPane();
        bp.setTop(buildTopSection());
        bp.setCenter(buildScheduleGrid());
        return bp;
    }

    // ?? Top bar ???????????????????????????????????????????????????????????????

    private VBox buildTopSection() {
        VBox top = new VBox();

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

        Label nameLbl = new Label();
        nameLbl.getStyleClass().add("tt-header-name");
        HBox.setHgrow(nameLbl, Priority.ALWAYS);
        nameLbl.setMaxWidth(Double.MAX_VALUE);
        nameLbl.setAlignment(Pos.CENTER_LEFT);

        Runnable updateName = () -> {
            nameLbl.textProperty().unbind();
            TimeTable tt = ctrl.getActiveTimetable();
            if (tt != null) {
                nameLbl.textProperty().bind(tt.titleProperty());
            } else {
                nameLbl.setText("No Timetable");
            }
        };
        updateName.run();
        ctrl.activeTimetableProperty().addListener((obs, o, n) -> updateName.run());

        bar.getChildren().addAll(hamburger, nameLbl);
        top.getChildren().add(bar);
        top.getChildren().add(buildWeekHeader());
        return top;
    }

    // ?? Week header (SUN MON ??SAT) ???????????????????????????????????????????

    private HBox buildWeekHeader() {
        HBox strip = new HBox(0);
        strip.getStyleClass().add("tt-week-strip");
        strip.setAlignment(Pos.CENTER);

        String[] days = {"SUN", "MON", "TUE", "WED", "THU", "FRI", "SAT"};

        // Left gutter spacer ??same width as the time-label column in the grid
        Region gutter = new Region();
        gutter.setPrefWidth(56);
        gutter.setMinWidth(56);
        strip.getChildren().add(gutter);

        for (int i = 0; i < 7; i++) {
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

    // ?? Schedule grid ?????????????????????????????????????????????????????????

    private ScrollPane buildScheduleGrid() {
        final int    START_H = 7;
        final int    END_H   = 22;
        final double ROW_H   = 56;
        final double COL_W   = (W - 56) / 7.0;
        final double TOTAL_H = (END_H - START_H + 1) * ROW_H + 16;

        AnchorPane canvas = new AnchorPane();
        canvas.getStyleClass().add("tt-grid-canvas");
        canvas.setPrefHeight(TOTAL_H);
        canvas.setPrefWidth(W);

        // Hour labels + horizontal dividers
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

        // Vertical column separators
        for (int d = 0; d <= 7; d++) {
            double x = 56 + d * COL_W;
            Line vLine = new Line(x, 8, x, TOTAL_H - 8);
            vLine.getStyleClass().add(
                d == activeDayIndex || d == activeDayIndex + 1
                    ? "tt-grid-vline-today" : "tt-grid-vline");
            canvas.getChildren().add(vLine);
        }

        // Initial card render
        renderClassCards(canvas, START_H, ROW_H, COL_W);

        bindClassListRenderer(canvas, START_H, ROW_H, COL_W);

        ScrollPane sp = new ScrollPane(canvas);
        sp.setFitToWidth(true);
        sp.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        sp.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        sp.getStyleClass().add("timeline-scroll");
        sp.setVvalue(0.1);
        return sp;
    }

    private void bindClassListRenderer(AnchorPane canvas, int startH,
                                       double rowH, double colW) {
        Runnable render = () -> {
            canvas.getChildren().removeIf(n -> Boolean.TRUE.equals(n.getUserData()));
            renderClassCards(canvas, startH, rowH, colW);
        };

        activeClassListListener = change -> render.run();
        TimeTable current = ctrl.getActiveTimetable();
        if (current != null) {
            current.getClasses().addListener(activeClassListListener);
        }

        ctrl.activeTimetableProperty().addListener((obs, oldTt, newTt) -> {
            if (oldTt != null && activeClassListListener != null) {
                oldTt.getClasses().removeListener(activeClassListListener);
            }
            activeClassListListener = change -> render.run();
            if (newTt != null) {
                newTt.getClasses().addListener(activeClassListListener);
            }
            render.run();
        });
    }

    private void renderClassCards(AnchorPane canvas, int startH,
                                   double rowH, double colW) {
        TimeTable tt = ctrl.getActiveTimetable();
        if (tt == null) return;

        for (TimetableClassRecord rec : tt.getClasses()) {
            TimetableClassRecord.AccentColor color = rec.getAccentColor();
            for (TimetableClassRecord.ClassTimeSlot slot : rec.getTimeSlots()) {
                int    dayCol = slot.getDayOfWeek().getValue() % 7;
                double x = 56 + dayCol * colW + 2;
                double y = (slot.getStartTime().getHour() - startH
                            + slot.getStartTime().getMinute() / 60.0) * rowH + 8;
                double h = Math.max(
                    (slot.getEndTime().getHour()  - slot.getStartTime().getHour()
                     + (slot.getEndTime().getMinute() - slot.getStartTime().getMinute()) / 60.0)
                    * rowH - 4, 32);

                VBox card = buildGridCard(rec, color, colW - 4, h);
                card.setUserData(Boolean.TRUE);   // marker for removal on re-render
                attachClassMenu(card, rec);
                AnchorPane.setTopAnchor(card,  y);
                AnchorPane.setLeftAnchor(card, x);
                canvas.getChildren().add(card);
            }
        }
    }

    private void attachClassMenu(VBox card, TimetableClassRecord record) {
        MenuItem editItem = new MenuItem("Edit Class");
        editItem.setOnAction(e -> ctrl.editClass(record));
        MenuItem deleteItem = new MenuItem("Delete Class");
        deleteItem.setOnAction(e -> ctrl.deleteClass(record));
        ContextMenu menu = new ContextMenu(editItem, deleteItem);

        card.setOnMouseClicked(e -> {
            menu.show(card, e.getScreenX(), e.getScreenY());
            e.consume();
        });
        card.setOnContextMenuRequested(e -> {
            menu.show(card, e.getScreenX(), e.getScreenY());
            e.consume();
        });
    }

    private VBox buildGridCard(TimetableClassRecord rec,
                                TimetableClassRecord.AccentColor color,
                                double w, double h) {
        VBox card = new VBox(2);
        card.getStyleClass().add("tt-grid-card");
        card.setStyle(
            "-fx-background-color: " + color.bg   + ";" +
            "-fx-border-color: "     + color.strip + ";" +
            "-fx-border-radius: 6;" +
            "-fx-background-radius: 6;" +
            "-fx-padding: 4 6 4 6;"
        );
        card.setPrefWidth(w);
        card.setPrefHeight(h);
        card.setMaxHeight(h);

        Label subjectLbl = new Label(rec.getSubject());
        subjectLbl.setWrapText(true);
        subjectLbl.setStyle("-fx-text-fill: " + color.strip
            + "; -fx-font-size: 10px; -fx-font-weight: bold;");

        Label roomLbl = new Label(rec.getClassroom());
        roomLbl.setStyle("-fx-text-fill: #666666; -fx-font-size: 9px;");

        card.getChildren().addAll(subjectLbl, roomLbl);
        return card;
    }

    // ????????????????????????????????????????????????????????????????????????????
    // Timetable FAB  (speed-dial: "TimeTable" + "Add Class")
    //
    // KEY FIX: both mini-buttons (btnTimeTable, btnAddClass) are declared
    // BEFORE their setOnAction lambdas so both variables are in scope for
    // both lambdas.  Declaring one after the other's lambda references it
    // causes a Java compile-time "cannot find symbol" error.
    // ????????????????????????????????????????????????????????????????????????????

    private VBox buildTimetableFab() {
        // ?? Declare BOTH buttons first ????????????????????????????????????????
        Button btnTimeTable = ttMiniFab(IconLabel.CALENDAR, "TimeTable");
        Button btnAddClass  = ttMiniFab(IconLabel.PLUS,     "Add Class");

        btnTimeTable.setVisible(false);
        btnTimeTable.setManaged(false);
        btnAddClass.setVisible(false);
        btnAddClass.setManaged(false);

        // ?? Wire actions (both variables now in scope for both lambdas) ???????
        btnTimeTable.setOnAction(e -> {
            collapseTtFab(btnTimeTable, btnAddClass, mainFabRef[0]);
            ctrl.showTimetableList();
        });
        btnAddClass.setOnAction(e -> {
            collapseTtFab(btnTimeTable, btnAddClass, mainFabRef[0]);
            ctrl.showAddClassPane();
        });

        // ?? Main FAB ??????????????????????????????????????????????????????????
        Label plusIcon = IconLabel.of(IconLabel.PLUS, 20, "fab-main-icon");
        Button mainFab = new Button();
        mainFab.setGraphic(plusIcon);
        mainFab.getStyleClass().add("fab-main");
        mainFab.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        mainFabRef[0] = mainFab;
        mainFab.setOnAction(e ->
            toggleTtFab(btnTimeTable, btnAddClass, plusIcon, mainFab));

        VBox root = new VBox(8);
        root.setAlignment(Pos.BOTTOM_RIGHT);
        root.setPickOnBounds(false);
        root.getChildren().addAll(btnTimeTable, btnAddClass, mainFab);
        return root;
    }

    private void toggleTtFab(Button b1, Button b2, Label icon, Button main) {
        ttFabExpanded = !ttFabExpanded;
        if (ttFabExpanded) {
            icon.setText(IconLabel.XMARK);
            icon.setStyle("-fx-font-family: '" + com.timeapp.MainApp.FA_SOLID_FAMILY
                + "'; -fx-font-size: 20px;");
            main.getStyleClass().add("fab-main-active");
            showTtMini(b1, 0);
            showTtMini(b2, 80);
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
        hideTtMini(b1);
        hideTtMini(b2);
    }

    private void showTtMini(Button btn, int delay) {
        btn.setVisible(true);
        btn.setManaged(true);
        btn.setOpacity(0);
        btn.setScaleX(0.6);
        btn.setScaleY(0.6);
        new Timeline(
            new KeyFrame(Duration.millis(delay),
                new KeyValue(btn.opacityProperty(), 0)),
            new KeyFrame(Duration.millis(delay + 180),
                new KeyValue(btn.opacityProperty(), 1,   Interpolator.EASE_OUT),
                new KeyValue(btn.scaleXProperty(),  1,   Interpolator.EASE_OUT),
                new KeyValue(btn.scaleYProperty(),  1,   Interpolator.EASE_OUT))
        ).play();
    }

    private void hideTtMini(Button btn) {
        Timeline tl = new Timeline(new KeyFrame(Duration.millis(140),
            new KeyValue(btn.opacityProperty(), 0,   Interpolator.EASE_IN),
            new KeyValue(btn.scaleXProperty(),  0.6, Interpolator.EASE_IN),
            new KeyValue(btn.scaleYProperty(),  0.6, Interpolator.EASE_IN)));
        tl.setOnFinished(e -> { btn.setVisible(false); btn.setManaged(false); });
        tl.play();
    }

    private Button ttMiniFab(String codepoint, String text) {
        HBox c = new HBox(8);
        c.setAlignment(Pos.CENTER_RIGHT);
        Label textLbl = new Label(text);
        textLbl.getStyleClass().add("fab-mini-label");
        c.getChildren().addAll(textLbl, IconLabel.of(codepoint, 14, "fab-mini-icon"));

        Button btn = new Button();
        btn.setGraphic(c);
        btn.getStyleClass().add("fab-mini");
        btn.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        btn.setPickOnBounds(false);
        return btn;
    }

    // ????????????????????????????????????????????????????????????????????????????
    // Sub-panel sliding animation
    // ????????????????????????????????????????????????????????????????????????????

    private void slideToPane(String from, String to) {
        Region enter = paneFor(to);
        Region leave = paneFor(from);
        if (enter == leave) return;

        // Returning to MAIN ??slide the current sub-panel back out
        if (enter == null) {
            if (leave == null) return;
            leave.setVisible(true);
            leave.setManaged(true);
            Timeline tl = new Timeline(new KeyFrame(Duration.millis(280),
                new KeyValue(leave.translateXProperty(), -W, Interpolator.EASE_BOTH)));
            tl.setOnFinished(e -> {
                leave.setVisible(false);
                leave.setManaged(false);
                leave.setTranslateX(W);   // reset for next time
            });
            tl.play();
            return;
        }

        // Navigating to a sub-panel
        boolean forward = isForward(from, to);
        double  dir     = forward ? W : -W;

        enter.setTranslateX(dir);
        enter.setVisible(true);
        enter.setManaged(true);

        if (leave == null) {
            // Coming from MAIN ??just slide the new panel in
            new Timeline(new KeyFrame(Duration.millis(280),
                new KeyValue(enter.translateXProperty(), 0, Interpolator.EASE_BOTH))
            ).play();
            return;
        }

        // Sub-panel to sub-panel (e.g. LIST ??CREATE)
        Timeline tl = new Timeline(new KeyFrame(Duration.millis(280),
            new KeyValue(enter.translateXProperty(), 0,    Interpolator.EASE_BOTH),
            new KeyValue(leave.translateXProperty(), -dir, Interpolator.EASE_BOTH)));
        tl.setOnFinished(e -> {
            leave.setVisible(false);
            leave.setManaged(false);
        });
        tl.play();
    }

    /** True when navigating deeper (higher panel index = further from MAIN). */
    private boolean isForward(String from, String to) {
        return paneIndex(to) > paneIndex(from);
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
            default       -> null;   // "MAIN" maps to null ??no sub-panel overlay
        };
    }
}
