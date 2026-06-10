package com.timeapp.view.calendar;

import com.timeapp.controller.DashboardController;
import com.timeapp.view.IconLabel;
import javafx.geometry.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.shape.Rectangle;

import java.time.format.*;
import java.util.Locale;

/**
 * Page 2 -- Day Schedule View.
 *
 * Shows only the CalendarEvents for the selected date, read from
 * ctrl.getCalendarDayEvents() (an ObservableList rebuilt by the controller
 * whenever the selected date changes or a new event is added).
 *
 * Layout per event row:
 *   10:00        |  Team Sprint Planning
 *   - 11:00      |  Zoom - link in calendar
 *                ^
 *             blue vertical line (1.5px, color matches card-strip-blue)
 */
public class CalendarDayPane extends VBox {

    private static final DateTimeFormatter TIME_FMT  =
            DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter HEADER_FMT =
            DateTimeFormatter.ofPattern("EEE, MMM d", Locale.ENGLISH);

    private final DashboardController ctrl;
    private final Label               dateLbl   = new Label();
    private final VBox                eventList = new VBox(0);

    public CalendarDayPane(DashboardController ctrl) {
        this.ctrl = ctrl;
        getStyleClass().add("page");
        setFillWidth(true);

        getChildren().addAll(buildHeader(), buildScrollArea());

        // Observe live event list -- auto-rebuild when controller updates it
        ctrl.getCalendarDayEvents().addListener(
            (javafx.collections.ListChangeListener<com.timeapp.domain.model.CalendarEvent>)
                c -> rebuildEventList());

        // Update header date label when selected date changes
        ctrl.selectedCalendarDateProperty().addListener((obs, o, n) -> {
            if (n != null) dateLbl.setText(n.format(HEADER_FMT));
        });

        if (ctrl.getSelectedCalendarDate() != null)
            dateLbl.setText(ctrl.getSelectedCalendarDate().format(HEADER_FMT));

        rebuildEventList();
    }

    // ── Header bar ────────────────────────────────────────────────────────────

    private HBox buildHeader() {
        HBox bar = new HBox(12);
        bar.getStyleClass().add("tt-panel-header");
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(16, 20, 16, 20));

        // Back to month view
        Label backIcon = IconLabel.of(IconLabel.CHEVRON_LEFT, 18, "topbar-icon");
        Button backBtn = new Button();
        backBtn.setGraphic(backIcon);
        backBtn.getStyleClass().add("icon-btn");
        backBtn.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        backBtn.setOnAction(e -> ctrl.backToCalendarMonth());
        Tooltip.install(backBtn, new Tooltip("Back to Calendar"));

        // Date label
        dateLbl.getStyleClass().add("tt-panel-title");
        HBox.setHgrow(dateLbl, Priority.ALWAYS);
        dateLbl.setAlignment(Pos.CENTER);
        dateLbl.setMaxWidth(Double.MAX_VALUE);

        // Add schedule for this specific date
        Label plusIcon = IconLabel.of(IconLabel.PLUS, 18, "tt-save-icon");
        Button addBtn  = new Button();
        addBtn.setGraphic(plusIcon);
        addBtn.getStyleClass().addAll("icon-btn", "tt-save-btn");
        addBtn.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        addBtn.setOnAction(e -> ctrl.onAddScheduleForDate(ctrl.getSelectedCalendarDate()));
        Tooltip.install(addBtn, new Tooltip("Add Schedule"));

        bar.getChildren().addAll(backBtn, dateLbl, addBtn);
        return bar;
    }

    // ── Event list area ───────────────────────────────────────────────────────

    private ScrollPane buildScrollArea() {
        eventList.setFillWidth(true);

        ScrollPane sp = new ScrollPane(eventList);
        sp.setFitToWidth(true);
        sp.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        sp.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        sp.getStyleClass().add("timeline-scroll");
        VBox.setVgrow(sp, Priority.ALWAYS);
        return sp;
    }

    private void rebuildEventList() {
        eventList.getChildren().clear();
        var events = ctrl.getCalendarDayEvents();

        if (events.isEmpty()) {
            Label empty = new Label("No schedules for this day");
            empty.getStyleClass().add("calendar-day-empty");
            empty.setAlignment(Pos.CENTER);
            empty.setMaxWidth(Double.MAX_VALUE);
            empty.setPadding(new Insets(48, 0, 0, 0));
            eventList.getChildren().add(empty);
            return;
        }

        for (com.timeapp.domain.model.CalendarEvent event : events) {
            eventList.getChildren().add(buildEventRow(event));
            Region sep = new Region();
            sep.getStyleClass().add("tt-form-separator");
            sep.setPrefHeight(1); sep.setMaxHeight(1);
            eventList.getChildren().add(sep);
        }
    }

    // ── Single event row ──────────────────────────────────────────────────────

    private HBox buildEventRow(com.timeapp.domain.model.CalendarEvent event) {
        HBox row = new HBox(0);
        row.getStyleClass().add("calendar-day-event-row");
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(16, 20, 16, 20));

        // Time column  "10:00\n- 11:00"
        String startStr = event.getStartTime() != null
            ? event.getStartTime().toLocalTime().format(TIME_FMT) : "--:--";
        String endStr = event.getEndTime() != null
            ? event.getEndTime().toLocalTime().format(TIME_FMT) : "--:--";

        Label timeLbl = new Label(startStr + "\n- " + endStr);
        timeLbl.getStyleClass().add("calendar-day-event-time");
        timeLbl.setPrefWidth(80); timeLbl.setMinWidth(80);
        timeLbl.setAlignment(Pos.CENTER_RIGHT);

        // Vertical accent line
        Rectangle line = new Rectangle(1.5, 36);
        line.getStyleClass().add("calendar-day-event-line");
        HBox lineWrapper = new HBox(line);
        lineWrapper.setAlignment(Pos.CENTER);
        lineWrapper.setPadding(new Insets(0, 14, 0, 14));

        // Title + optional location
        VBox content = new VBox(2);
        content.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(content, Priority.ALWAYS);

        Label titleLbl = new Label(event.getTitle() != null ? event.getTitle() : "");
        titleLbl.getStyleClass().add("calendar-day-event-title");
        titleLbl.setWrapText(true);
        content.getChildren().add(titleLbl);

        if (event.getLocation() != null && !event.getLocation().isBlank()) {
            Label locLbl = new Label(event.getLocation());
            locLbl.getStyleClass().add("calendar-day-event-location");
            locLbl.setWrapText(true);
            content.getChildren().add(locLbl);
        }

        row.getChildren().addAll(timeLbl, lineWrapper, content);
        row.setOnMouseEntered(e -> row.getStyleClass().add("calendar-day-event-row-hover"));
        row.setOnMouseExited(e  -> row.getStyleClass().remove("calendar-day-event-row-hover"));
        return row;
    }
}
