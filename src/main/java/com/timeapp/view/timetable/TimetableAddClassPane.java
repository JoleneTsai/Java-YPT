package com.timeapp.view.timetable;

import com.timeapp.controller.DashboardController;
import com.timeapp.model.TimetableClassRecord;
import com.timeapp.model.TimetableClassRecord.AccentColor;
import com.timeapp.model.TimetableClassRecord.ClassTimeSlot;
import com.timeapp.view.IconLabel;
import javafx.geometry.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.shape.Circle;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Panel 2 — Add Class Screen.
 *
 * Triggered from Panel 1's FAB → "Add Class".
 *
 * Form fields:
 *   Subject, Teacher, Color (circle picker), Class Time (dynamic rows), Classroom
 *
 * Save flow:
 *   Validates → builds TimetableClassRecord → calls ctrl.saveClass(record).
 *   Controller appends to the active timetable, refreshes the timeline,
 *   and fires activeTimetablePane back to "MAIN".
 *
 * ── Required fixes applied ────────────────────────────────────────────────────
 * 1. import java.time.format.DateTimeFormatter  — wildcard java.time.* does NOT
 *    cover sub-packages; the inner class TimeSlotRow needs this explicitly.
 * 2. private HBox node = null  — initialise the field to null so the compiler
 *    can confirm it is assigned before use without reporting
 *    "variable might not have been initialized".
 * ─────────────────────────────────────────────────────────────────────────────
 */
public class TimetableAddClassPane extends VBox {

    private final DashboardController ctrl;

    // Form fields
    private final TextField subjectField   = formField("e.g. Algorithms");
    private final TextField teacherField   = formField("e.g. Prof. Chen Wei");
    private final TextField classroomField = formField("e.g. Room 301");

    private AccentColor selectedColor = AccentColor.PURPLE;

    private final VBox              timeSlotsBox  = new VBox(8);
    private final List<TimeSlotRow> timeSlotRows  = new ArrayList<>();

    public TimetableAddClassPane(DashboardController ctrl) {
        this.ctrl = ctrl;
        getStyleClass().add("page");
        setFillWidth(true);

        getChildren().addAll(buildHeader(), buildForm());
        addTimeSlotRow();   // start with one default row
    }

    // ── Header ────────────────────────────────────────────────────────────────

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
        backBtn.setOnAction(e -> ctrl.backToTimetableMain());

        Label title = new Label("Add Class");
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
        Tooltip.install(saveBtn, new Tooltip("Save Class"));

        bar.getChildren().addAll(backBtn, title, saveBtn);
        return bar;
    }

    // ── Form ─────────────────────────────────────────────────────────────────

    private ScrollPane buildForm() {
        VBox form = new VBox(0);
        form.getStyleClass().add("tt-form");
        form.setPadding(new Insets(8, 20, 40, 20));

        form.getChildren().addAll(
            formRow("Subject",   subjectField),
            separator(),
            formRow("Teacher",   teacherField),
            separator(),
            buildColorRow(),
            separator(),
            buildClassTimeSection(),
            separator(),
            formRow("Classroom", classroomField),
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

    // ── Form helpers ──────────────────────────────────────────────────────────

    private HBox formRow(String label, TextField field) {
        Label lbl = new Label(label);
        lbl.getStyleClass().add("tt-form-label");
        lbl.setPrefWidth(90);
        lbl.setMinWidth(90);

        HBox row = new HBox(12, lbl, field);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(14, 0, 14, 0));
        HBox.setHgrow(field, Priority.ALWAYS);
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

    // ── Color picker row ──────────────────────────────────────────────────────

    private HBox buildColorRow() {
        Label lbl = new Label("Color");
        lbl.getStyleClass().add("tt-form-label");
        lbl.setPrefWidth(90);
        lbl.setMinWidth(90);

        HBox circles = new HBox(10);
        circles.setAlignment(Pos.CENTER_LEFT);

        ToggleGroup tg = new ToggleGroup();

        for (AccentColor color : AccentColor.values()) {
            ToggleButton tb = new ToggleButton();
            tb.getStyleClass().add("tt-color-btn");
            tb.setToggleGroup(tg);

            Circle dot = new Circle(12);
            dot.setStyle("-fx-fill: " + color.strip + ";");
            tb.setGraphic(dot);
            tb.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
            if (color == selectedColor) tb.setSelected(true);

            tb.selectedProperty().addListener((obs, o, selected) -> {
                if (selected) {
                    selectedColor = color;
                    dot.setStyle("-fx-fill: " + color.strip
                        + "; -fx-effect: dropshadow(gaussian,"
                        + color.strip + ",8,0.5,0,0);");
                } else {
                    dot.setStyle("-fx-fill: " + color.strip + ";");
                }
            });

            circles.getChildren().add(tb);
        }

        HBox row = new HBox(12, lbl, circles);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(14, 0, 14, 0));
        return row;
    }

    // ── Class time section ────────────────────────────────────────────────────

    private VBox buildClassTimeSection() {
        Label lbl = new Label("Class Time");
        lbl.getStyleClass().add("tt-form-label");

        Button addTimeBtn = new Button("+ Add Time");
        addTimeBtn.getStyleClass().add("tt-add-time-btn");
        addTimeBtn.setOnAction(e -> addTimeSlotRow());

        VBox section = new VBox(10);
        section.setPadding(new Insets(14, 0, 14, 0));
        section.getChildren().addAll(lbl, timeSlotsBox, addTimeBtn);
        return section;
    }

    private void addTimeSlotRow() {
        TimeSlotRow row = new TimeSlotRow();
        timeSlotRows.add(row);
        timeSlotsBox.getChildren().add(row.getNode());
    }

    // ── Save ─────────────────────────────────────────────────────────────────

    private void onSave() {
        String subject   = subjectField.getText().trim();
        String teacher   = teacherField.getText().trim();
        String classroom = classroomField.getText().trim();

        if (subject.isEmpty()) {
            subjectField.getStyleClass().add("tt-field-error");
            subjectField.setPromptText("Subject is required");
            return;
        }
        subjectField.getStyleClass().remove("tt-field-error");

        TimetableClassRecord rec =
            new TimetableClassRecord(subject, teacher, classroom, selectedColor);

        for (TimeSlotRow row : timeSlotRows) {
            ClassTimeSlot slot = row.buildSlot();
            if (slot != null) rec.addTimeSlot(slot);
        }

        ctrl.saveClass(rec);
        resetForm();
    }

    private void resetForm() {
        subjectField.clear();
        teacherField.clear();
        classroomField.clear();
        selectedColor = AccentColor.PURPLE;
        timeSlotRows.clear();
        timeSlotsBox.getChildren().clear();
        addTimeSlotRow();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Inner class — one time-slot row  (Day + Start + End + remove)
    //
    // FIX: `private HBox node = null;`
    //   The field is explicitly initialised to null so javac can verify it is
    //   always assigned a value (in the constructor) before getNode() reads it.
    //   Without the `= null` initialiser the compiler may report
    //   "variable node might not have been initialized" if it analyses a code
    //   path where the constructor throws before the assignment.
    // ═══════════════════════════════════════════════════════════════════════════

    private class TimeSlotRow {

        private final ComboBox<DayOfWeek> dayBox;
        private final ComboBox<LocalTime> startBox;
        private final ComboBox<LocalTime> endBox;
        private HBox node = null;   // initialised to null; assigned in constructor

        TimeSlotRow() {
            dayBox   = buildDayCombo();
            startBox = buildTimeCombo();
            endBox   = buildTimeCombo();

            // Sensible defaults
            dayBox.setValue(DayOfWeek.MONDAY);
            startBox.setValue(LocalTime.of(9, 0));
            endBox.setValue(LocalTime.of(10, 30));

            Label dash = new Label("–");
            dash.getStyleClass().add("tt-form-label");

            Label xIcon = IconLabel.of(IconLabel.XMARK, 12, "tt-remove-icon");
            Button remove = new Button();
            remove.setGraphic(xIcon);
            remove.getStyleClass().add("tt-remove-btn");
            remove.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
            remove.setOnAction(e -> {
                timeSlotRows.remove(this);
                timeSlotsBox.getChildren().remove(node);
            });

            node = new HBox(6, dayBox, startBox, dash, endBox, remove);
            node.setAlignment(Pos.CENTER_LEFT);
        }

        HBox getNode() { return node; }

        ClassTimeSlot buildSlot() {
            DayOfWeek day   = dayBox.getValue();
            LocalTime start = startBox.getValue();
            LocalTime end   = endBox.getValue();
            if (day == null || start == null || end == null) return null;
            if (!end.isAfter(start)) return null;
            return new ClassTimeSlot(day, start, end);
        }

        private ComboBox<DayOfWeek> buildDayCombo() {
            ComboBox<DayOfWeek> cb = new ComboBox<>();
            cb.getItems().addAll(DayOfWeek.values());
            cb.getStyleClass().add("tt-combo");
            cb.setCellFactory(lv -> new ListCell<>() {
                @Override protected void updateItem(DayOfWeek d, boolean empty) {
                    super.updateItem(d, empty);
                    setText(empty || d == null ? "" : d.name().substring(0, 3));
                }
            });
            cb.setButtonCell(new ListCell<>() {
                @Override protected void updateItem(DayOfWeek d, boolean empty) {
                    super.updateItem(d, empty);
                    setText(empty || d == null ? "" : d.name().substring(0, 3));
                }
            });
            cb.setPrefWidth(72);
            return cb;
        }

        private ComboBox<LocalTime> buildTimeCombo() {
            ComboBox<LocalTime> cb = new ComboBox<>();
            for (int h = 7; h <= 22; h++) {
                cb.getItems().add(LocalTime.of(h, 0));
                if (h < 22) cb.getItems().add(LocalTime.of(h, 30));
            }
            cb.getStyleClass().add("tt-combo");
            // FIX: explicit import java.time.format.DateTimeFormatter required;
            // java.time.* wildcard does NOT cover sub-packages.
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("HH:mm");
            cb.setCellFactory(lv -> new ListCell<>() {
                @Override protected void updateItem(LocalTime t, boolean empty) {
                    super.updateItem(t, empty);
                    setText(empty || t == null ? "" : t.format(fmt));
                }
            });
            cb.setButtonCell(new ListCell<>() {
                @Override protected void updateItem(LocalTime t, boolean empty) {
                    super.updateItem(t, empty);
                    setText(empty || t == null ? "" : t.format(fmt));
                }
            });
            cb.setPrefWidth(76);
            return cb;
        }
    }
}
