package com.timeapp.view.timeline;

import com.timeapp.controller.DashboardController;
import com.timeapp.view.IconLabel;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * Timeline sub-panel for creating a calendar event from the main FAB.
 */
public class TimelineAddSchedulePane extends VBox {

    private final DashboardController ctrl;

    private final TextField titleField = formField("e.g. Team meeting");
    private final TextField locationField = formField("e.g. Library 3F");
    private final TextArea descriptionArea = new TextArea();
    private final DatePicker datePicker = new DatePicker();
    private final ComboBox<LocalTime> startBox = buildTimeCombo();
    private final ComboBox<LocalTime> endBox = buildTimeCombo();
    private final Label titleError = errorLabel();
    private final Label timeError = errorLabel();

    public TimelineAddSchedulePane(DashboardController ctrl) {
        this.ctrl = ctrl;
        getStyleClass().add("page");
        setFillWidth(true);

        descriptionArea.setPromptText("Optional notes");
        descriptionArea.setPrefRowCount(4);
        descriptionArea.getStyleClass().add("tt-text-area");

        getChildren().addAll(buildHeader(), buildForm());

        ctrl.activeTimelinePaneProperty().addListener((obs, oldPane, newPane) -> {
            if ("ADD_SCHEDULE".equals(newPane)) {
                resetForOpen();
            }
        });
    }

    private HBox buildHeader() {
        HBox bar = new HBox(12);
        bar.getStyleClass().add("tt-panel-header");
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(16, 20, 16, 20));

        Label backIcon = IconLabel.of(IconLabel.CHEVRON_LEFT, 18, "topbar-icon");
        Button backBtn = new Button();
        backBtn.setGraphic(backIcon);
        backBtn.getStyleClass().add("icon-btn");
        backBtn.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        backBtn.setOnAction(e -> ctrl.backToTimelineMain());

        Label title = new Label("Add Schedule");
        title.getStyleClass().add("tt-panel-title");
        HBox.setHgrow(title, Priority.ALWAYS);
        title.setAlignment(Pos.CENTER);
        title.setMaxWidth(Double.MAX_VALUE);

        Label saveIcon = IconLabel.of(IconLabel.CHECK, 16, "tt-save-icon");
        Button saveBtn = new Button();
        saveBtn.setGraphic(saveIcon);
        saveBtn.getStyleClass().addAll("icon-btn", "tt-save-btn");
        saveBtn.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        saveBtn.setOnAction(e -> onSave());
        Tooltip.install(saveBtn, new Tooltip("Save Schedule"));

        bar.getChildren().addAll(backBtn, title, saveBtn);
        return bar;
    }

    private ScrollPane buildForm() {
        VBox form = new VBox(0);
        form.getStyleClass().add("tt-form");
        form.setPadding(new Insets(8, 20, 40, 20));

        form.getChildren().addAll(
            formRow("Title", titleField),
            titleError,
            separator(),
            dateRow("Date", datePicker),
            separator(),
            timeRow(),
            timeError,
            separator(),
            formRow("Location", locationField),
            separator(),
            textAreaRow("Notes", descriptionArea),
            separator()
        );

        ScrollPane sp = new ScrollPane(form);
        sp.setFitToWidth(true);
        sp.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        sp.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        sp.getStyleClass().add("timeline-scroll");
        VBox.setVgrow(sp, Priority.ALWAYS);
        return sp;
    }

    private HBox formRow(String labelText, TextField field) {
        Label label = formLabel(labelText);
        HBox row = new HBox(12, label, field);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(14, 0, 14, 0));
        HBox.setHgrow(field, Priority.ALWAYS);
        return row;
    }

    private HBox dateRow(String labelText, DatePicker picker) {
        Label label = formLabel(labelText);
        picker.setEditable(false);
        picker.setMaxWidth(Double.MAX_VALUE);
        picker.getStyleClass().add("tt-date-picker");

        HBox row = new HBox(12, label, picker);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(14, 0, 14, 0));
        HBox.setHgrow(picker, Priority.ALWAYS);
        return row;
    }

    private HBox timeRow() {
        Label label = formLabel("Time");
        Label dash = new Label("-");
        dash.getStyleClass().add("tt-form-label");

        HBox times = new HBox(8, startBox, dash, endBox);
        times.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(times, Priority.ALWAYS);

        HBox row = new HBox(12, label, times);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(14, 0, 14, 0));
        return row;
    }

    private HBox textAreaRow(String labelText, TextArea area) {
        Label label = formLabel(labelText);
        area.setMaxWidth(Double.MAX_VALUE);

        HBox row = new HBox(12, label, area);
        row.setAlignment(Pos.TOP_LEFT);
        row.setPadding(new Insets(14, 0, 14, 0));
        HBox.setHgrow(area, Priority.ALWAYS);
        return row;
    }

    private Label formLabel(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("tt-form-label");
        label.setPrefWidth(86);
        label.setMinWidth(86);
        return label;
    }

    private Region separator() {
        Region r = new Region();
        r.getStyleClass().add("tt-form-separator");
        r.setPrefHeight(1);
        r.setMaxHeight(1);
        return r;
    }

    private void onSave() {
        boolean valid = true;
        String title = titleField.getText().trim();
        LocalDate date = datePicker.getValue();
        LocalTime start = startBox.getValue();
        LocalTime end = endBox.getValue();

        if (title.isEmpty()) {
            titleError.setText("Please enter a schedule title.");
            titleError.setVisible(true);
            titleError.setManaged(true);
            titleField.getStyleClass().add("tt-field-error");
            valid = false;
        } else {
            titleError.setVisible(false);
            titleError.setManaged(false);
            titleField.getStyleClass().remove("tt-field-error");
        }

        if (date == null || start == null || end == null) {
            timeError.setText("Please select date, start time, and end time.");
            timeError.setVisible(true);
            timeError.setManaged(true);
            valid = false;
        } else if (!end.isAfter(start)) {
            timeError.setText("End time must be after start time.");
            timeError.setVisible(true);
            timeError.setManaged(true);
            valid = false;
        } else {
            timeError.setVisible(false);
            timeError.setManaged(false);
        }

        if (!valid) {
            return;
        }

        ctrl.saveCalendarEvent(new com.timeapp.domain.model.CalendarEvent(
            title,
            LocalDateTime.of(date, start),
            LocalDateTime.of(date, end),
            locationField.getText().trim(),
            descriptionArea.getText().trim(),
            "#4A9EFF",
            "schedule"
        ));
    }

    private void resetForOpen() {
        titleField.clear();
        locationField.clear();
        descriptionArea.clear();
        datePicker.setValue(ctrl.getSelectedDay());
        startBox.setValue(LocalTime.of(9, 0));
        endBox.setValue(LocalTime.of(10, 0));
        titleError.setVisible(false);
        titleError.setManaged(false);
        timeError.setVisible(false);
        timeError.setManaged(false);
        titleField.getStyleClass().remove("tt-field-error");
    }

    private static TextField formField(String prompt) {
        TextField field = new TextField();
        field.setPromptText(prompt);
        field.getStyleClass().add("tt-text-field");
        return field;
    }

    private static Label errorLabel() {
        Label label = new Label();
        label.getStyleClass().add("tt-error-label");
        label.setVisible(false);
        label.setManaged(false);
        return label;
    }

    private static ComboBox<LocalTime> buildTimeCombo() {
        ComboBox<LocalTime> combo = new ComboBox<>();
        for (int hour = 7; hour <= 22; hour++) {
            combo.getItems().add(LocalTime.of(hour, 0));
            if (hour < 22) {
                combo.getItems().add(LocalTime.of(hour, 30));
            }
        }
        combo.getStyleClass().add("tt-combo");
        combo.setPrefWidth(92);

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("HH:mm");
        combo.setCellFactory(lv -> new ListCell<>() {
            @Override protected void updateItem(LocalTime time, boolean empty) {
                super.updateItem(time, empty);
                setText(empty || time == null ? "" : time.format(fmt));
            }
        });
        combo.setButtonCell(new ListCell<>() {
            @Override protected void updateItem(LocalTime time, boolean empty) {
                super.updateItem(time, empty);
                setText(empty || time == null ? "" : time.format(fmt));
            }
        });
        return combo;
    }
}
