package com.timeapp.view.timetable;

import com.timeapp.controller.DashboardController;
import com.timeapp.view.IconLabel;
import javafx.geometry.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.time.LocalDate;

/**
 * Panel 4 — Create New TimeTable Screen.
 *
 * Triggered from Panel 3's mini-FAB.
 *
 * Form fields:
 *   Title (TextField)  — semester name, e.g. "113-2 Semester Spring"
 *   SemStart (DatePicker) — first day of the semester
 *   SemEnd   (DatePicker) — last day of the semester
 *
 * Save flow:
 *   Validates inputs → calls ctrl.saveNewTimetable(title, start, end).
 *   Controller creates a new TimeTable, appends it to timetableList,
 *   sets it as the active timetable, and navigates back to Panel 3.
 *
 * Date-range contract:
 *   The start/end values stored here are read by DashboardController's
 *   addActiveTimetableClasses() to gate whether classes should appear in
 *   the daily Timeline.  Classes silently disappear once end < today.
 */
public class TimetableCreatePane extends VBox {

    private final DashboardController ctrl;

    // Form fields
    private final TextField  titleField  = formField("e.g. 113-2 Semester Spring");
    private final DatePicker startPicker = buildDatePicker();
    private final DatePicker endPicker   = buildDatePicker();

    // Inline validation labels
    private final Label titleError = errorLabel();
    private final Label dateError  = errorLabel();

    public TimetableCreatePane(DashboardController ctrl) {
        this.ctrl = ctrl;
        getStyleClass().add("page");
        setFillWidth(true);

        // Sensible defaults: current month through +5 months
        startPicker.setValue(LocalDate.now().withDayOfMonth(1));
        endPicker.setValue(LocalDate.now().withDayOfMonth(1).plusMonths(5).minusDays(1));

        getChildren().addAll(buildHeader(), buildForm());
    }

    // ── Header ────────────────────────────────────────────────────────────────

    private HBox buildHeader() {
        HBox bar = new HBox(12);
        bar.getStyleClass().add("tt-panel-header");
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(16, 20, 16, 20));

        // Back → Panel 3
        Label backIcon = IconLabel.of(IconLabel.CHEVRON_LEFT, 18, "topbar-icon");
        Button backBtn = new Button();
        backBtn.setGraphic(backIcon);
        backBtn.getStyleClass().add("icon-btn");
        backBtn.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        backBtn.setOnAction(e -> ctrl.backToTimetableList());

        Label title = new Label("Add TimeTable");
        title.getStyleClass().add("tt-panel-title");
        HBox.setHgrow(title, Priority.ALWAYS);
        title.setAlignment(Pos.CENTER);
        title.setMaxWidth(Double.MAX_VALUE);

        // Save / Done
        Label checkIcon = IconLabel.of(IconLabel.CHECK, 16, "tt-save-icon");
        Button saveBtn = new Button();
        saveBtn.setGraphic(checkIcon);
        saveBtn.getStyleClass().addAll("icon-btn", "tt-save-btn");
        saveBtn.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        saveBtn.setOnAction(e -> onSave());
        Tooltip.install(saveBtn, new Tooltip("Save TimeTable"));

        bar.getChildren().addAll(backBtn, title, saveBtn);
        return bar;
    }

    // ── Form ─────────────────────────────────────────────────────────────────

    private VBox buildForm() {
        VBox form = new VBox(0);
        form.getStyleClass().add("tt-form");
        form.setPadding(new Insets(8, 20, 40, 20));

        form.getChildren().addAll(
            formRow("Title", titleField),
            titleError,
            separator(),
            dateRow("SemStart", startPicker),
            separator(),
            dateRow("SemEnd",   endPicker),
            dateError,
            separator()
        );

        return form;
    }

    // ── Form helpers ──────────────────────────────────────────────────────────

    private HBox formRow(String labelText, TextField field) {
        Label lbl = new Label(labelText);
        lbl.getStyleClass().add("tt-form-label");
        lbl.setPrefWidth(86);
        lbl.setMinWidth(86);

        HBox row = new HBox(12, lbl, field);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(14, 0, 14, 0));
        HBox.setHgrow(field, Priority.ALWAYS);
        return row;
    }

    private HBox dateRow(String labelText, DatePicker picker) {
        Label lbl = new Label(labelText);
        lbl.getStyleClass().add("tt-form-label");
        lbl.setPrefWidth(86);
        lbl.setMinWidth(86);

        picker.setMaxWidth(Double.MAX_VALUE);
        picker.getStyleClass().add("tt-date-picker");

        HBox row = new HBox(12, lbl, picker);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(14, 0, 14, 0));
        HBox.setHgrow(picker, Priority.ALWAYS);
        return row;
    }

    private Region separator() {
        Region r = new Region();
        r.getStyleClass().add("tt-form-separator");
        r.setPrefHeight(1);
        r.setMaxHeight(1);
        return r;
    }

    private static TextField formField(String prompt) {
        TextField tf = new TextField();
        tf.setPromptText(prompt);
        tf.getStyleClass().add("tt-text-field");
        return tf;
    }

    private static DatePicker buildDatePicker() {
        DatePicker dp = new DatePicker();
        dp.setEditable(false);
        return dp;
    }

    private static Label errorLabel() {
        Label lbl = new Label();
        lbl.getStyleClass().add("tt-error-label");
        lbl.setVisible(false);
        lbl.setManaged(false);
        return lbl;
    }

    // ── Save ─────────────────────────────────────────────────────────────────

    private void onSave() {
        boolean valid = true;

        // Validate title
        String title = titleField.getText().trim();
        if (title.isEmpty()) {
            titleError.setText("Please enter a semester title.");
            titleError.setVisible(true);
            titleError.setManaged(true);
            titleField.getStyleClass().add("tt-field-error");
            valid = false;
        } else {
            titleError.setVisible(false);
            titleError.setManaged(false);
            titleField.getStyleClass().remove("tt-field-error");
        }

        // Validate dates
        LocalDate start = startPicker.getValue();
        LocalDate end   = endPicker.getValue();
        if (start == null || end == null) {
            dateError.setText("Please select both start and end dates.");
            dateError.setVisible(true);
            dateError.setManaged(true);
            valid = false;
        } else if (!end.isAfter(start)) {
            dateError.setText("End date must be after start date.");
            dateError.setVisible(true);
            dateError.setManaged(true);
            valid = false;
        } else {
            dateError.setVisible(false);
            dateError.setManaged(false);
        }

        if (!valid) return;

        // Persist via controller
        ctrl.saveNewTimetable(title, start, end);

        // Reset form for next use
        titleField.clear();
        startPicker.setValue(LocalDate.now().withDayOfMonth(1));
        endPicker.setValue(LocalDate.now().withDayOfMonth(1).plusMonths(5).minusDays(1));
    }
}
