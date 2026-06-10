package com.timeapp.view.calendar;

import com.timeapp.controller.DashboardController;
import com.timeapp.view.FabButton;
import com.timeapp.view.IconLabel;
import javafx.animation.*;
import javafx.geometry.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.shape.*;
import javafx.util.Duration;

import java.time.*;
import java.time.format.*;
import java.util.Locale;

/**
 * Page 1 -- Calendar Month View.
 *
 * Shows a 7-column month grid with:
 *   - Today circle (reuses .day-circle-today from WeekStripBar)
 *   - Event dots (blue) for days that have CalendarEvents in the service
 *   - Click on any day -> slides in CalendarDayPane from the right
 *
 * The month grid rebuilds when:
 *   - calendarDisplayMonthProperty changes (prev/next/backToThisMonth)
 *   - calendarDataVersionProperty increments (after saving a new event)
 */
public class CalendarMainPane extends AnchorPane {

    private static final double W       = 390;
    private static final String[] DAYS  = {"Sun","Mon","Tue","Wed","Thu","Fri","Sat"};
    private static final DateTimeFormatter MONTH_FMT =
            DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH);

    private final DashboardController ctrl;
    private final GridPane            monthGrid = new GridPane();
    private       Label               monthLabel;
    private final CalendarDayPane     dayPane;

    public CalendarMainPane(DashboardController ctrl) {
        this.ctrl = ctrl;
        getStyleClass().add("page");

        // Base content (always visible)
        BorderPane mainContent = buildMainContent();
        AnchorPane.setTopAnchor(mainContent,    0.0);
        AnchorPane.setBottomAnchor(mainContent, 0.0);
        AnchorPane.setLeftAnchor(mainContent,   0.0);
        AnchorPane.setRightAnchor(mainContent,  0.0);
        getChildren().add(mainContent);

        // Day detail sub-panel (starts off-screen right)
        dayPane = new CalendarDayPane(ctrl);
        dayPane.setVisible(false);
        dayPane.setManaged(false);
        dayPane.setTranslateX(W);
        AnchorPane.setTopAnchor(dayPane,    0.0);
        AnchorPane.setBottomAnchor(dayPane, 0.0);
        AnchorPane.setLeftAnchor(dayPane,   0.0);
        AnchorPane.setRightAnchor(dayPane,  0.0);
        getChildren().add(dayPane);

        // FAB -- reuses FabButton (same as Timeline: Add Schedule + Add To-do)
        FabButton fab     = new FabButton(ctrl);
        VBox      fabNode = fab.getNode();
        fabNode.setPickOnBounds(false);
        AnchorPane.setRightAnchor(fabNode,  20.0);
        AnchorPane.setBottomAnchor(fabNode, 36.0);
        getChildren().add(fabNode);

        // Wiring
        ctrl.calendarActivePaneProperty().addListener((obs, from, to) ->
            slideDayPane("DAY".equals(to)));

        ctrl.calendarDisplayMonthProperty().addListener((obs, o, n) -> {
            if (monthLabel != null) monthLabel.setText(n.format(MONTH_FMT));
            rebuildGrid();
        });

        ctrl.calendarDataVersionProperty().addListener((obs, o, n) -> rebuildGrid());

        rebuildGrid();
    }

    // ── Main content ──────────────────────────────────────────────────────────

    private BorderPane buildMainContent() {
        BorderPane bp = new BorderPane();

        VBox top = new VBox();
        top.getChildren().addAll(buildTopBar(), buildWeekHeaders());
        bp.setTop(top);

        for (int c = 0; c < 7; c++) {
            ColumnConstraints cc = new ColumnConstraints();
            cc.setPercentWidth(100.0 / 7);
            cc.setHalignment(HPos.CENTER);
            monthGrid.getColumnConstraints().add(cc);
        }
        monthGrid.getStyleClass().add("calendar-grid");

        ScrollPane sp = new ScrollPane(monthGrid);
        sp.setFitToWidth(true);
        sp.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        sp.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        sp.getStyleClass().add("timeline-scroll");
        bp.setCenter(sp);

        return bp;
    }

    private HBox buildTopBar() {
        HBox bar = new HBox(0);
        bar.getStyleClass().add("top-bar");
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(16, 20, 12, 20));

        Button hamburger = iconBtn(IconLabel.of(IconLabel.BARS, 18, "topbar-icon"));
        hamburger.setOnAction(e -> ctrl.toggleSidebar());

        Button prevBtn = iconBtn(IconLabel.of(IconLabel.CHEVRON_LEFT, 16, "topbar-icon"));
        prevBtn.setOnAction(e -> ctrl.calendarPrevMonth());

        monthLabel = new Label(ctrl.getCalendarDisplayMonth().format(MONTH_FMT));
        monthLabel.getStyleClass().add("calendar-month-label");
        HBox.setHgrow(monthLabel, Priority.ALWAYS);
        monthLabel.setMaxWidth(Double.MAX_VALUE);
        monthLabel.setAlignment(Pos.CENTER);

        Button nextBtn = iconBtn(IconLabel.of(IconLabel.CHEVRON_RIGHT, 16, "topbar-icon"));
        nextBtn.setOnAction(e -> ctrl.calendarNextMonth());

        Button todayBtn = iconBtn(IconLabel.of(IconLabel.ROTATE_RIGHT, 18, "topbar-icon"));
        todayBtn.setOnAction(e -> ctrl.calendarBackToThisMonth());
        Tooltip.install(todayBtn, new Tooltip("Back to This Month"));

        bar.getChildren().addAll(hamburger, prevBtn, monthLabel, nextBtn, todayBtn);
        return bar;
    }

    private HBox buildWeekHeaders() {
        HBox strip = new HBox(0);
        strip.getStyleClass().add("calendar-week-header");
        strip.setAlignment(Pos.CENTER);
        for (String day : DAYS) {
            Label lbl = new Label(day);
            lbl.getStyleClass().add("calendar-week-header-label");
            lbl.setAlignment(Pos.CENTER);
            lbl.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(lbl, Priority.ALWAYS);
            strip.getChildren().add(lbl);
        }
        return strip;
    }

    // ── Month grid ────────────────────────────────────────────────────────────

    private void rebuildGrid() {
        monthGrid.getChildren().clear();
        monthGrid.getRowConstraints().clear();

        YearMonth ym          = ctrl.getCalendarDisplayMonth();
        LocalDate firstOfMonth = ym.atDay(1);
        int       startOffset  = firstOfMonth.getDayOfWeek().getValue() % 7;
        int       daysInMonth  = ym.lengthOfMonth();
        int       rows         = (int) Math.ceil((startOffset + daysInMonth) / 7.0);

        for (int r = 0; r < rows; r++) {
            RowConstraints rc = new RowConstraints();
            rc.setMinHeight(68); rc.setPrefHeight(72);
            monthGrid.getRowConstraints().add(rc);
        }

        LocalDate today  = LocalDate.now();
        YearMonth prevYM = ym.minusMonths(1);
        YearMonth nextYM = ym.plusMonths(1);
        int prevDays      = prevYM.lengthOfMonth();

        // Previous month overflow
        for (int i = 0; i < startOffset; i++) {
            int dayNum = prevDays - startOffset + 1 + i;
            monthGrid.add(buildCell(prevYM.atDay(dayNum), dayNum, false, true), i, 0);
        }

        // Current month
        for (int day = 1; day <= daysInMonth; day++) {
            LocalDate date = ym.atDay(day);
            int pos = startOffset + day - 1;
            monthGrid.add(buildCell(date, day, date.equals(today), false), pos % 7, pos / 7);
        }

        // Next month overflow
        int nextDay = 1;
        for (int pos = startOffset + daysInMonth; pos < rows * 7; pos++) {
            LocalDate date = nextYM.atDay(nextDay);
            monthGrid.add(buildCell(date, nextDay, false, true), pos % 7, pos / 7);
            nextDay++;
        }
    }

    private StackPane buildCell(LocalDate date, int dayNum,
                                 boolean isToday, boolean isOverflow) {
        StackPane cell = new StackPane();
        cell.getStyleClass().add("calendar-day-cell");
        cell.setPrefHeight(72);
        cell.setMaxWidth(Double.MAX_VALUE);

        VBox content = new VBox(2);
        content.setAlignment(Pos.TOP_CENTER);
        content.setPadding(new Insets(8, 0, 4, 0));

        Label dayLabel = new Label(String.valueOf(dayNum));

        if (isToday) {
            // Reuse day-circle-today class (same red as WeekStripBar today circle)
            Circle circle = new Circle(17);
            circle.getStyleClass().add("day-circle-today");
            dayLabel.getStyleClass().add("day-num-highlighted");
            StackPane badge = new StackPane(circle, dayLabel);
            badge.setAlignment(Pos.CENTER);
            badge.setMaxWidth(34); badge.setPrefWidth(34);
            content.getChildren().add(badge);
        } else {
            dayLabel.getStyleClass().add(isOverflow ? "calendar-overflow-day-num" : "calendar-day-num");
            content.getChildren().add(dayLabel);
        }

        // Event dot
        if (!isOverflow && ctrl.hasCalendarEventsOnDate(date)) {
            Circle dot = new Circle(3.5);
            dot.getStyleClass().add("calendar-event-dot");
            content.getChildren().add(dot);
        }

        cell.getChildren().add(content);

        if (isOverflow) {
            cell.setOpacity(0.38);
            cell.setCursor(javafx.scene.Cursor.HAND);
            cell.setOnMouseClicked(e -> {
                ctrl.calendarDisplayMonthProperty().set(YearMonth.from(date));
                ctrl.showCalendarDayView(date);
            });
        } else {
            cell.setCursor(javafx.scene.Cursor.HAND);
            cell.setOnMouseClicked(e -> ctrl.showCalendarDayView(date));
            cell.setOnMouseEntered(e -> cell.getStyleClass().add("calendar-day-cell-hover"));
            cell.setOnMouseExited(e ->  cell.getStyleClass().remove("calendar-day-cell-hover"));
        }

        return cell;
    }

    // ── Sub-panel slide ───────────────────────────────────────────────────────

    private void slideDayPane(boolean entering) {
        if (entering) {
            dayPane.setVisible(true); dayPane.setManaged(true);
            dayPane.setTranslateX(W);
            new Timeline(new KeyFrame(Duration.millis(280),
                new KeyValue(dayPane.translateXProperty(), 0, Interpolator.EASE_BOTH))).play();
        } else {
            Timeline tl = new Timeline(new KeyFrame(Duration.millis(280),
                new KeyValue(dayPane.translateXProperty(), W, Interpolator.EASE_BOTH)));
            tl.setOnFinished(e -> { dayPane.setVisible(false); dayPane.setManaged(false); });
            tl.play();
        }
    }

    private Button iconBtn(Label icon) {
        Button btn = new Button();
        btn.setGraphic(icon);
        btn.getStyleClass().add("icon-btn");
        btn.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        return btn;
    }
}
