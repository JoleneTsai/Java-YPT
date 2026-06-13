package com.timeapp.view.calendar;

import com.timeapp.controller.DashboardController;
import com.timeapp.domain.model.ToDoTask;
import com.timeapp.view.IconLabel;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.RowConstraints;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.Month;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Month calendar page with year/month controls and daily item summary.
 */
public class CalendarPane extends BorderPane {

    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("EEE, MMM d", Locale.ENGLISH);
    private static final DateTimeFormatter TIME_FMT =
            DateTimeFormatter.ofPattern("HH:mm");

    private final DashboardController ctrl;
    private final ComboBox<Integer> yearBox = new ComboBox<>();
    private final ComboBox<Month> monthBox = new ComboBox<>();
    private final GridPane monthGrid = new GridPane();
    private final VBox detailList = new VBox(8);
    private final Label selectedDateLabel = new Label();

    private YearMonth displayedMonth = YearMonth.from(LocalDate.now());
    private boolean updatingControls;

    public CalendarPane(DashboardController ctrl) {
        this.ctrl = ctrl;
        getStyleClass().add("page");

        buildMonthControls();
        setTop(buildHeader());
        setCenter(buildContent());

        ctrl.selectedDayProperty().addListener((obs, oldDate, newDate) -> {
            if (newDate != null) {
                displayedMonth = YearMonth.from(newDate);
                syncControls();
                refresh();
            }
        });
        ctrl.dataRevisionProperty().addListener((obs, oldValue, newValue) -> refresh());

        syncControls();
        refresh();
    }

    private VBox buildHeader() {
        HBox topBar = new HBox(12);
        topBar.getStyleClass().add("top-bar");
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.setPadding(new Insets(16, 20, 12, 20));

        Label hamburgerIcon = IconLabel.of(IconLabel.BARS, 18, "topbar-icon");
        Button menuBtn = new Button();
        menuBtn.setGraphic(hamburgerIcon);
        menuBtn.getStyleClass().add("icon-btn");
        menuBtn.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        menuBtn.setOnAction(e -> ctrl.toggleSidebar());
        Tooltip.install(menuBtn, new Tooltip("Menu"));

        Label title = new Label("Calendar");
        title.getStyleClass().add("header-date");
        HBox.setHgrow(title, Priority.ALWAYS);
        title.setMaxWidth(Double.MAX_VALUE);

        Label todayIcon = IconLabel.of(IconLabel.ROTATE_RIGHT, 18, "topbar-icon");
        Button todayBtn = new Button();
        todayBtn.setGraphic(todayIcon);
        todayBtn.getStyleClass().add("icon-btn");
        todayBtn.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        todayBtn.setOnAction(e -> {
            ctrl.backToToday();
            displayedMonth = YearMonth.from(LocalDate.now());
            syncControls();
            refresh();
        });
        Tooltip.install(todayBtn, new Tooltip("Back to Today"));

        topBar.getChildren().addAll(menuBtn, title, todayBtn);

        HBox controls = new HBox(10);
        controls.getStyleClass().add("calendar-controls");
        controls.setAlignment(Pos.CENTER_LEFT);
        controls.setPadding(new Insets(0, 20, 12, 20));
        controls.getChildren().addAll(yearBox, monthBox);
        HBox.setHgrow(monthBox, Priority.ALWAYS);

        return new VBox(topBar, controls);
    }

    private ScrollPane buildContent() {
        VBox content = new VBox(14);
        content.setPadding(new Insets(0, 20, 28, 20));
        content.getStyleClass().add("calendar-page-body");

        GridPane weekHeader = buildWeekHeader();
        monthGrid.getStyleClass().add("calendar-grid");
        monthGrid.setHgap(6);
        monthGrid.setVgap(6);

        for (int i = 0; i < 7; i++) {
            ColumnConstraints col = new ColumnConstraints();
            col.setPercentWidth(100.0 / 7.0);
            monthGrid.getColumnConstraints().add(col);
        }
        for (int i = 0; i < 6; i++) {
            RowConstraints row = new RowConstraints();
            row.setPercentHeight(100.0 / 6.0);
            monthGrid.getRowConstraints().add(row);
        }

        VBox details = new VBox(10);
        details.getStyleClass().add("calendar-detail-panel");
        selectedDateLabel.getStyleClass().add("calendar-detail-title");
        detailList.getStyleClass().add("calendar-detail-list");
        details.getChildren().addAll(selectedDateLabel, detailList);

        content.getChildren().addAll(weekHeader, monthGrid, details);

        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scroll.getStyleClass().add("timeline-scroll");
        return scroll;
    }

    private GridPane buildWeekHeader() {
        GridPane header = new GridPane();
        header.getStyleClass().add("calendar-week-header");
        header.setHgap(6);
        for (int i = 0; i < 7; i++) {
            ColumnConstraints col = new ColumnConstraints();
            col.setPercentWidth(100.0 / 7.0);
            header.getColumnConstraints().add(col);
        }

        DayOfWeek[] days = {
            DayOfWeek.SUNDAY, DayOfWeek.MONDAY, DayOfWeek.TUESDAY,
            DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY,
            DayOfWeek.SATURDAY
        };
        for (int i = 0; i < days.length; i++) {
            Label label = new Label(days[i].getDisplayName(TextStyle.SHORT, Locale.ENGLISH).toUpperCase(Locale.ENGLISH));
            label.getStyleClass().add("calendar-week-label");
            label.setMaxWidth(Double.MAX_VALUE);
            label.setAlignment(Pos.CENTER);
            header.add(label, i, 0);
        }
        return header;
    }

    private void buildMonthControls() {
        int currentYear = LocalDate.now().getYear();
        for (int year = currentYear - 5; year <= currentYear + 5; year++) {
            yearBox.getItems().add(year);
        }
        yearBox.getStyleClass().add("calendar-combo");

        monthBox.getItems().addAll(Month.values());
        monthBox.getStyleClass().add("calendar-combo");
        monthBox.setMaxWidth(Double.MAX_VALUE);
        monthBox.setCellFactory(list -> monthCell());
        monthBox.setButtonCell(monthCell());

        yearBox.valueProperty().addListener((obs, oldYear, newYear) -> {
            if (!updatingControls && newYear != null) {
                displayedMonth = YearMonth.of(newYear, displayedMonth.getMonth());
                refresh();
            }
        });
        monthBox.valueProperty().addListener((obs, oldMonth, newMonth) -> {
            if (!updatingControls && newMonth != null) {
                displayedMonth = YearMonth.of(displayedMonth.getYear(), newMonth);
                refresh();
            }
        });
    }

    private ListCell<Month> monthCell() {
        return new ListCell<>() {
            @Override protected void updateItem(Month month, boolean empty) {
                super.updateItem(month, empty);
                setText(empty || month == null
                    ? ""
                    : month.getDisplayName(TextStyle.FULL, Locale.ENGLISH));
            }
        };
    }

    private void syncControls() {
        updatingControls = true;
        if (!yearBox.getItems().contains(displayedMonth.getYear())) {
            yearBox.getItems().add(displayedMonth.getYear());
            yearBox.getItems().sort(Integer::compareTo);
        }
        yearBox.setValue(displayedMonth.getYear());
        monthBox.setValue(displayedMonth.getMonth());
        updatingControls = false;
    }

    private void refresh() {
        Set<LocalDate> todoDates = ctrl.getIncompleteTodoDates();
        LocalDate selected = ctrl.getSelectedDay();
        monthGrid.getChildren().clear();

        LocalDate firstVisible = firstVisibleDate(displayedMonth);
        LocalDate today = LocalDate.now();

        for (int index = 0; index < 42; index++) {
            LocalDate date = firstVisible.plusDays(index);
            VBox cell = buildDayCell(date, selected, today, todoDates.contains(date));
            monthGrid.add(cell, index % 7, index / 7);
        }

        refreshDetails(selected);
    }

    private LocalDate firstVisibleDate(YearMonth month) {
        LocalDate first = month.atDay(1);
        int offset = first.getDayOfWeek().getValue() % 7;
        return first.minusDays(offset);
    }

    private VBox buildDayCell(LocalDate date, LocalDate selected, LocalDate today, boolean hasTodo) {
        VBox cell = new VBox(4);
        cell.getStyleClass().add("calendar-day-cell");
        cell.setAlignment(Pos.TOP_CENTER);
        cell.setPadding(new Insets(7, 4, 5, 4));
        cell.setMinHeight(48);
        cell.setMaxWidth(Double.MAX_VALUE);

        if (!YearMonth.from(date).equals(displayedMonth)) {
            cell.getStyleClass().add("calendar-day-outside");
        }
        if (date.equals(today)) {
            cell.getStyleClass().add("calendar-day-today");
        }
        if (date.equals(selected)) {
            cell.getStyleClass().add("calendar-day-selected");
        }

        Label dayNumber = new Label(Integer.toString(date.getDayOfMonth()));
        dayNumber.getStyleClass().add("calendar-day-number");

        Region dotSlot = new Region();
        dotSlot.setMinHeight(7);
        dotSlot.setPrefHeight(7);
        if (hasTodo) {
            Circle dot = new Circle(3.5);
            dot.getStyleClass().add("calendar-todo-dot");
            cell.getChildren().addAll(dayNumber, dot);
        } else {
            cell.getChildren().addAll(dayNumber, dotSlot);
        }

        cell.setOnMouseClicked(e -> ctrl.selectDay(date));
        return cell;
    }

    private void refreshDetails(LocalDate date) {
        detailList.getChildren().clear();
        if (date == null) {
            selectedDateLabel.setText("No date selected");
            return;
        }

        selectedDateLabel.setText(date.format(DATE_FMT));
        List<ToDoTask> todos = ctrl.getIncompleteTodosOnDate(date);
        List<com.timeapp.domain.model.CalendarEvent> events = ctrl.getCalendarEventsOnDate(date);

        if (todos.isEmpty() && events.isEmpty()) {
            Label empty = new Label("No items for this date.");
            empty.getStyleClass().add("calendar-empty-text");
            detailList.getChildren().add(empty);
            return;
        }

        for (ToDoTask todo : todos) {
            detailList.getChildren().add(detailRow(
                "To-Do",
                todo.getStartTime().format(TIME_FMT),
                todo.getTitle(),
                "calendar-detail-row-todo"
            ));
        }
        for (com.timeapp.domain.model.CalendarEvent event : events) {
            String time = event.getStartTime() != null ? event.getStartTime().toLocalTime().format(TIME_FMT) : "";
            detailList.getChildren().add(detailRow(
                "Schedule",
                time,
                event.getTitle(),
                "calendar-detail-row-event"
            ));
        }
    }

    private HBox detailRow(String kind, String time, String title, String styleClass) {
        HBox row = new HBox(10);
        row.getStyleClass().addAll("calendar-detail-row", styleClass);
        row.setAlignment(Pos.CENTER_LEFT);

        Label timeLabel = new Label(time);
        timeLabel.getStyleClass().add("calendar-detail-time");
        timeLabel.setMinWidth(46);

        VBox text = new VBox(2);
        Label kindLabel = new Label(kind);
        kindLabel.getStyleClass().add("calendar-detail-kind");
        Label titleLabel = new Label(title == null || title.isBlank() ? kind : title);
        titleLabel.getStyleClass().add("calendar-detail-item-title");
        titleLabel.setWrapText(true);
        text.getChildren().addAll(kindLabel, titleLabel);
        HBox.setHgrow(text, Priority.ALWAYS);

        row.getChildren().addAll(timeLabel, text);
        return row;
    }
}
