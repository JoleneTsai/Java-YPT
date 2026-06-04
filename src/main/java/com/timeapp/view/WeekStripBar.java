package com.timeapp.view;

import com.timeapp.controller.DashboardController;
import javafx.beans.value.ChangeListener;
import javafx.geometry.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.shape.*;

import java.time.*;
import java.time.format.*;
import java.util.Locale;

/**
 * A horizontal strip showing 7 days (Sun–Sat) for the current week.
 * Left/Right arrow buttons move between weeks.
 * The active day is highlighted with a circular accent.
 */
public class WeekStripBar {

    private static final DateTimeFormatter DAY_NAME = DateTimeFormatter.ofPattern("EEE", Locale.ENGLISH);
    private static final DateTimeFormatter DAY_NUM  = DateTimeFormatter.ofPattern("d");

    private final HBox root;
    private final DashboardController ctrl;

    private final Label yearLbl;
    private final Label monthLbl;

    public WeekStripBar(DashboardController ctrl) {
        this.ctrl = ctrl;

        root = new HBox();
        root.getStyleClass().add("week-strip");
        root.setAlignment(Pos.CENTER_LEFT);

        this.yearLbl = new Label();
        this.monthLbl = new Label();

        this.yearLbl.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #8E8E93;");
        this.monthLbl.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #111111;");

        build();

        // Rebuild when week changes
        ChangeListener<LocalDate> rebuild = (obs, o, n) -> build();
        ctrl.weekStartProperty().addListener(rebuild);
        ctrl.selectedDayProperty().addListener(rebuild);
    }

    public HBox getNode() { return root; }

    // ── Build / rebuild ───────────────────────────────────────────────────────

    private void build() {
        root.getChildren().clear();

        DateTimeFormatter yearFmt = DateTimeFormatter.ofPattern("yyyy", Locale.ENGLISH);
        DateTimeFormatter monthFmt = DateTimeFormatter.ofPattern("MMM", Locale.ENGLISH);

        LocalDate selectedDay = ctrl.getSelectedDay();
        yearLbl.setText(selectedDay.format(yearFmt));
        monthLbl.setText(selectedDay.format(monthFmt));

        VBox leftHeader = new VBox(2, yearLbl, monthLbl);
        leftHeader.setAlignment(Pos.CENTER);
        leftHeader.setMinWidth(50);
        leftHeader.setMaxWidth(50);
        leftHeader.setPadding(new Insets(0, 4, 0, 4));

        root.getChildren().add(leftHeader);
        
        // ← Prev week button
        Button prev = arrowButton("‹");
        prev.setOnAction(e -> ctrl.prevWeek());

        // Day cells
        HBox days = new HBox(0);
        days.setAlignment(Pos.CENTER);
        HBox.setHgrow(days, Priority.ALWAYS);
        days.setFillHeight(true);

        LocalDate weekStart = ctrl.getWeekStart();
        LocalDate today     = LocalDate.now();
        LocalDate selected  = ctrl.getSelectedDay();

        for (int i = 0; i < 7; i++) {
            LocalDate day = weekStart.plusDays(i);
            days.getChildren().add(buildDayCell(day, today, selected));
        }

        // → Next week button
        Button next = arrowButton("›");
        next.setOnAction(e -> ctrl.nextWeek());

        root.getChildren().addAll(prev, days, next);
    }

    // ── Day cell ──────────────────────────────────────────────────────────────

    private VBox buildDayCell(LocalDate day, LocalDate today, LocalDate selected) {
        boolean isToday    = day.equals(today);
        boolean isSelected = day.equals(selected);

        Label nameLbl = new Label(day.format(DAY_NAME).toUpperCase());
        nameLbl.getStyleClass().add("day-name");

        Label numLbl = new Label(day.format(DAY_NUM));
        numLbl.getStyleClass().add("day-num");

        // Number container: plain or circled
        StackPane numContainer = new StackPane(numLbl);
        numContainer.setPrefSize(34, 34);
        numContainer.setMinSize(34, 34);

        if (isSelected || isToday) {
            Circle circle = new Circle(15);
            circle.getStyleClass().add(isToday ? "day-circle-today" : "day-circle-selected");
            numContainer.getChildren().add(0, circle); // behind label
            numLbl.getStyleClass().add("day-num-highlighted");
        }

        VBox cell = new VBox(2, nameLbl, numContainer);
        cell.setAlignment(Pos.CENTER);
        cell.setPadding(new Insets(6, 0, 6, 0));
        cell.getStyleClass().add("day-cell");
        HBox.setHgrow(cell, Priority.ALWAYS);

        // Dim days outside the selected week that belong to an adjacent month
        if (day.getMonth() != ctrl.getSelectedDay().getMonth()) {
            cell.setOpacity(0.45);
        }

        // Click → select day
        cell.setOnMouseClicked(e -> ctrl.selectDay(day));

        return cell;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Button arrowButton(String text) {
        Button btn = new Button(text);
        btn.getStyleClass().add("week-arrow-btn");
        return btn;
    }
}
