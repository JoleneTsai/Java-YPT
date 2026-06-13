package com.timeapp.view;

import com.timeapp.controller.DashboardController;
import com.timeapp.view.calendar.CalendarPane;
import com.timeapp.view.timeline.TimelineAddSchedulePane;
import com.timeapp.view.timeline.TimelineAddTodoPane;
import com.timeapp.view.timetable.TimetableMainPane;
import javafx.animation.*;
import javafx.geometry.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Root view — assembles the complete layer stack and owns the
 * top-level section-switching mechanism.
 *
 * ── Layer stack (AnchorPane z-order, bottom → top) ────────────────────────────
 *   [z=0]  StackPane  contentStack  — holds TimelineSection & TimetableSection
 *   [z=1]  Rectangle  dimOverlay    — shown when sidebar is open
 *   [z=2]  VBox       sidebarNode   — slides in from left
 *   [z=3]  VBox       fabNode       — fixed bottom-right (timeline only)
 *
 * ── Panel switching ───────────────────────────────────────────────────────────
 *   ctrl.activePanelProperty() drives a cross-fade + slide transition:
 *     "TIMELINE"  → timelineSection  (BorderPane: topbar + weekstrip + timeline)
 *     "TIMETABLE" → TimetableMainPane (self-contained; owns sub-panel routing)
 *
 *   Both nodes live in the StackPane; the inactive one is set to
 *   visible=false + managed=false after the transition so it receives no events.
 * ─────────────────────────────────────────────────────────────────────────────
 */
public class DashboardView {

    private static final DateTimeFormatter HEADER_FMT =
            DateTimeFormatter.ofPattern("EEE, MMM d", Locale.ENGLISH);

    private final AnchorPane root;
    private final DashboardController ctrl;

    // Content sections
    private StackPane              timelineSection;
    private BorderPane             timelineMainPane;
    private TimelineAddSchedulePane timelineAddSchedulePane;
    private TimelineAddTodoPane     timelineAddTodoPane;
    private CalendarPane           calendarSection;
    private TimetableMainPane      timetableSection;

    public DashboardView() {
        ctrl = new DashboardController();
        root = new AnchorPane();
        root.getStyleClass().add("app-root");
        build();
    }

    public AnchorPane          getRoot()       { return root; }
    public DashboardController getController() { return ctrl; }

    // ── Build ─────────────────────────────────────────────────────────────────

    private void build() {

        // ── [z=0]  Content stack ──────────────────────────────────────────────
        StackPane contentStack = new StackPane();
        AnchorPane.setTopAnchor(contentStack,    0.0);
        AnchorPane.setBottomAnchor(contentStack, 0.0);
        AnchorPane.setLeftAnchor(contentStack,   0.0);
        AnchorPane.setRightAnchor(contentStack,  0.0);

        timelineSection  = buildTimelineSection();

        timetableSection = new TimetableMainPane(ctrl);
        timetableSection.setVisible(false);
        timetableSection.setManaged(false);

        calendarSection = new CalendarPane(ctrl);
        calendarSection.setVisible(false);
        calendarSection.setManaged(false);

        contentStack.getChildren().addAll(timelineSection, timetableSection, calendarSection);
        root.getChildren().add(contentStack);

        // ── [z=1]  Dim overlay ────────────────────────────────────────────────
        Rectangle overlay = new Rectangle();
        overlay.setFill(Color.rgb(0, 0, 0, 0.35));
        overlay.widthProperty().bind(root.widthProperty());
        overlay.heightProperty().bind(root.heightProperty());
        overlay.setVisible(false);
        overlay.setOnMouseClicked(e -> ctrl.closeSidebar());

        ctrl.sidebarOpenProperty().addListener((obs, o, open) -> {
            overlay.setVisible(open);
            if (open) ctrl.collapseFab();
        });

        AnchorPane.setTopAnchor(overlay,  0.0);
        AnchorPane.setLeftAnchor(overlay, 0.0);
        root.getChildren().add(overlay);

        // ── [z=2]  Sidebar ────────────────────────────────────────────────────
        SidebarDrawer sidebar    = new SidebarDrawer(ctrl);
        VBox          sidebarNode = sidebar.getNode();

        Rectangle clip = new Rectangle();
        clip.widthProperty().bind(sidebarNode.widthProperty());
        clip.heightProperty().bind(sidebarNode.heightProperty());
        sidebarNode.setClip(clip);

        AnchorPane.setTopAnchor(sidebarNode,    0.0);
        AnchorPane.setBottomAnchor(sidebarNode, 0.0);
        AnchorPane.setLeftAnchor(sidebarNode,   0.0);
        root.getChildren().add(sidebarNode);

        // ── [z=3]  FAB (Timeline view only) ──────────────────────────────────
        FabButton fab     = new FabButton(ctrl);
        VBox      fabNode = fab.getNode();
        AnchorPane.setRightAnchor(fabNode,  20.0);
        AnchorPane.setBottomAnchor(fabNode, 36.0);

        // Show the main FAB only on the Timeline main screen.
        Runnable updateFabVisibility = () -> fabNode.setVisible(
            "TIMELINE".equals(ctrl.getActivePanel())
                && "MAIN".equals(ctrl.getActiveTimelinePane())
        );
        ctrl.activePanelProperty().addListener((obs, o, panel) -> updateFabVisibility.run());
        ctrl.activeTimelinePaneProperty().addListener((obs, o, pane) -> updateFabVisibility.run());
        updateFabVisibility.run();

        root.getChildren().add(fabNode);

        // Dismiss FAB on background click in timeline section
        timelineSection.setOnMouseClicked(e -> ctrl.collapseFab());

        // ── Panel-switch wiring ───────────────────────────────────────────────
        ctrl.activePanelProperty().addListener((obs, oldPanel, newPanel) ->
            animatePanelSwitch(oldPanel, newPanel));
    }

    // ── Timeline section ──────────────────────────────────────────────────────

    private StackPane buildTimelineSection() {
        StackPane stack = new StackPane();

        BorderPane page = new BorderPane();
        page.getStyleClass().add("page");

        VBox topSection = new VBox();
        topSection.getChildren().addAll(buildTopBar(), buildWeekStrip());
        page.setTop(topSection);

        TimelinePane timeline = new TimelinePane(390, ctrl.getDayEntries());
        page.setCenter(timeline.getNode());

        timelineMainPane = page;
        timelineAddSchedulePane = new TimelineAddSchedulePane(ctrl);
        timelineAddTodoPane = new TimelineAddTodoPane(ctrl);

        timelineAddSchedulePane.setVisible(false);
        timelineAddSchedulePane.setManaged(false);
        timelineAddTodoPane.setVisible(false);
        timelineAddTodoPane.setManaged(false);

        stack.getChildren().addAll(page, timelineAddSchedulePane, timelineAddTodoPane);
        ctrl.activeTimelinePaneProperty().addListener((obs, oldPane, newPane) ->
            animateTimelinePaneSwitch(oldPane, newPane));

        return stack;
    }

    private HBox buildTopBar() {
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
        Tooltip.install(hamburger, new Tooltip("Menu"));

        Label dateLbl = new Label(LocalDate.now().format(HEADER_FMT));
        dateLbl.getStyleClass().add("header-date");
        ctrl.selectedDayProperty().addListener((obs, o, n) ->
            dateLbl.setText(n.format(HEADER_FMT)));
        HBox.setHgrow(dateLbl, Priority.ALWAYS);
        dateLbl.setAlignment(Pos.CENTER_LEFT);
        dateLbl.setMaxWidth(Double.MAX_VALUE);

        Label rotateIcon = IconLabel.of(IconLabel.ROTATE_RIGHT, 18, "topbar-icon");
        Button todayBtn = new Button();
        todayBtn.setGraphic(rotateIcon);
        todayBtn.getStyleClass().add("icon-btn");
        todayBtn.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        todayBtn.setOnAction(e -> ctrl.backToToday());
        Tooltip.install(todayBtn, new Tooltip("Back to Today"));

        bar.getChildren().addAll(hamburger, dateLbl, todayBtn);
        return bar;
    }

    private HBox buildWeekStrip() {
        return new WeekStripBar(ctrl).getNode();
    }

    private void animateTimelinePaneSwitch(String leaving, String entering) {
        Region enterNode = timelinePaneFor(entering);
        Region leaveNode = timelinePaneFor(leaving);

        if (enterNode == leaveNode) {
            return;
        }

        enterNode.setVisible(true);
        enterNode.setManaged(true);

        if ("MAIN".equals(entering)) {
            enterNode.setOpacity(1);
            enterNode.setTranslateX(0);
            Timeline tl = new Timeline(
                new KeyFrame(Duration.millis(220),
                    new KeyValue(leaveNode.opacityProperty(), 0, Interpolator.EASE_BOTH),
                    new KeyValue(leaveNode.translateXProperty(), 32, Interpolator.EASE_BOTH)
                )
            );
            tl.setOnFinished(e -> {
                leaveNode.setVisible(false);
                leaveNode.setManaged(false);
                leaveNode.setOpacity(1);
                leaveNode.setTranslateX(0);
            });
            tl.play();
            return;
        }

        enterNode.setOpacity(0);
        enterNode.setTranslateX(32);
        Timeline tl = new Timeline(
            new KeyFrame(Duration.millis(260),
                new KeyValue(enterNode.opacityProperty(), 1, Interpolator.EASE_BOTH),
                new KeyValue(enterNode.translateXProperty(), 0, Interpolator.EASE_BOTH)
            )
        );
        tl.setOnFinished(e -> {
            if (leaveNode != timelineMainPane) {
                leaveNode.setVisible(false);
                leaveNode.setManaged(false);
            }
            leaveNode.setOpacity(1);
            leaveNode.setTranslateX(0);
        });
        tl.play();
    }

    private Region timelinePaneFor(String pane) {
        return switch (pane) {
            case "ADD_SCHEDULE" -> timelineAddSchedulePane;
            case "ADD_TODO" -> timelineAddTodoPane;
            default -> timelineMainPane;
        };
    }

    // ── Panel-switch animation ────────────────────────────────────────────────

    /**
     * Cross-fades between timelineSection and timetableSection.
     *
     * Entering panel slides in from the right (translateX: +40 → 0).
     * Leaving panel fades out in place.
     * After the animation the leaving panel is hidden (visible=false,
     * managed=false) so it receives no mouse events.
     */
    private void animatePanelSwitch(String leaving, String entering) {
        Region enterNode = topLevelPaneFor(entering);
        Region leaveNode = topLevelPaneFor(leaving);

        if (enterNode == leaveNode) return;

        enterNode.setVisible(true);
        enterNode.setManaged(true);
        enterNode.setOpacity(0);
        enterNode.setTranslateX(32);

        Timeline tl = new Timeline(
            new KeyFrame(Duration.millis(260),
                new KeyValue(enterNode.opacityProperty(),    1, Interpolator.EASE_BOTH),
                new KeyValue(enterNode.translateXProperty(), 0, Interpolator.EASE_BOTH),
                new KeyValue(leaveNode.opacityProperty(),    0, Interpolator.EASE_BOTH)
            )
        );
        tl.setOnFinished(e -> {
            leaveNode.setVisible(false);
            leaveNode.setManaged(false);
            leaveNode.setOpacity(1);   // reset for next transition
            leaveNode.setTranslateX(0);
        });
        tl.play();
    }

    private Region topLevelPaneFor(String panel) {
        return switch (panel) {
            case "TIMETABLE" -> timetableSection;
            case "CALENDAR" -> calendarSection;
            default -> timelineSection;
        };
    }
}
