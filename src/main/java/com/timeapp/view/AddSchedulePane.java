package com.timeapp.view;

import com.timeapp.controller.DashboardController;
import com.timeapp.domain.model.CalendarEvent;
import javafx.geometry.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Full-panel "Add Schedule" form — replaces the plain JavaFX Dialog.
 *
 * Slides in from the right over the current section (Timeline or Calendar)
 * when ctrl.overlayPaneProperty() is set to "ADD_SCHEDULE".
 * Follows the same visual design as TimetableAddClassPane and TimetableCreatePane:
 *   tt-panel-header, tt-panel-title, tt-form-label, tt-text-field, tt-form-separator,
 *   tt-date-picker, tt-combo.
 *
 * Layout:
 *   [‹]    Add Schedule    [✓]      ← header
 *   Title  [_____________]
 *   Date   [DatePicker   ]
 *   Start  [09:00 ▾      ]
 *   End    [10:00 ▾      ]
 *   Location [___________]
 *   Description
 *   [___________________ ]
 */
public class AddSchedulePane extends VBox {

    private static final DateTimeFormatter TIME_FMT =
            DateTimeFormatter.ofPattern("HH:mm", Locale.ENGLISH);

    private final DashboardController ctrl;

    private final TextField  titleField    = styledField("e.g. Team Meeting");
    private final DatePicker datePicker    = styledDatePicker();
    private final ComboBox<LocalTime> startBox = buildTimeCombo();
    private final ComboBox<LocalTime> endBox   = buildTimeCombo();
    private final TextField  locationField = styledField("e.g. Room 301 / Zoom");
    private final TextArea   descArea      = styledTextArea();

    private final Label titleError = errorLabel();
    private final Label timeError  = errorLabel();

    public AddSchedulePane(DashboardController ctrl) {
        this.ctrl = ctrl;
        getStyleClass().add("page");
        setFillWidth(true);

        // Default times
        startBox.setValue(LocalTime.of(9, 0));
        endBox.setValue(LocalTime.of(10, 0));

        getChildren().addAll(buildHeader(), buildForm());

        // When this pane becomes active, refresh date to pendingAddDate
        ctrl.overlayPaneProperty().addListener((obs, from, to) -> {
            if ("ADD_SCHEDULE".equals(to)) {
                resetForm(ctrl.getPendingAddDate());
            }
        });
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
        backBtn.setOnAction(e -> ctrl.closeOverlay());
        Tooltip.install(backBtn, new Tooltip("Cancel"));

        Label title = new Label("Add Schedule");
        title.getStyleClass().add("tt-panel-title");
        HBox.setHgrow(title, Priority.ALWAYS);
        title.setAlignment(Pos.CENTER);
        title.setMaxWidth(Double.MAX_VALUE);

        Label checkIcon = IconLabel.of(IconLabel.CHECK, 16, "tt-save-icon");
        Button saveBtn  = new Button();
        saveBtn.setGraphic(checkIcon);
        saveBtn.getStyleClass().addAll("icon-btn", "tt-save-btn");
        saveBtn.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        saveBtn.setOnAction(e -> onSave());
        Tooltip.install(saveBtn, new Tooltip("Save"));

        bar.getChildren().addAll(backBtn, title, saveBtn);
        return bar;
    }

    // ── Form ─────────────────────────────────────────────────────────────────

    private ScrollPane buildForm() {
        VBox form = new VBox(0);
        form.getStyleClass().add("tt-form");
        form.setPadding(new Insets(8, 20, 48, 20));

        form.getChildren().addAll(
            fieldRow("Title",       titleField),
            titleError,
            sep(),
            dateRow("Date",        datePicker),
            sep(),
            comboRow("Start",      startBox),
            sep(),
            comboRow("End",        endBox),
            timeError,
            sep(),
            fieldRow("Location",   locationField),
            sep(),
            textAreaRow("Description", descArea),
            sep()
        );

        ScrollPane sp = new ScrollPane(form);
        sp.setFitToWidth(true);
        sp.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        sp.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        sp.getStyleClass().add("timeline-scroll");
        VBox.setVgrow(sp, Priority.ALWAYS);
        return sp;
    }

    // ── Save ─────────────────────────────────────────────────────────────────

    private void onSave() {
        boolean valid = true;

        String title = titleField.getText().trim();
        if (title.isEmpty()) {
            titleError.setText("Title is required.");
            show(titleError); titleField.getStyleClass().add("tt-field-error");
            valid = false;
        } else {
            hide(titleError); titleField.getStyleClass().remove("tt-field-error");
        }

        LocalDate  date  = datePicker.getValue();
        LocalTime  start = startBox.getValue();
        LocalTime  end   = endBox.getValue();
        if (date == null || start == null || end == null || !end.isAfter(start)) {
            timeError.setText(date == null
                ? "Please select a date."
                : "End time must be after start time.");
            show(timeError);
            valid = false;
        } else {
            hide(timeError);
        }

        if (!valid) return;

        CalendarEvent event = new CalendarEvent(
            title,
            LocalDateTime.of(date, start),
            LocalDateTime.of(date, end),
            locationField.getText().trim(),
            descArea.getText().trim(),
            "#4A9EFF",
            "schedule"
        );

        ctrl.saveSchedule(event);
    }

    // ── Reset ─────────────────────────────────────────────────────────────────

    public void resetForm(LocalDate defaultDate) {
        titleField.clear();
        datePicker.setValue(defaultDate != null ? defaultDate : LocalDate.now());
        startBox.setValue(LocalTime.of(9, 0));
        endBox.setValue(LocalTime.of(10, 0));
        locationField.clear();
        descArea.clear();
        hide(titleError); hide(timeError);
        titleField.getStyleClass().remove("tt-field-error");
    }

    // ── Row builders ──────────────────────────────────────────────────────────

    private HBox fieldRow(String labelText, TextField field) {
        Label lbl = label(labelText);
        HBox row = new HBox(12, lbl, field);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(14, 0, 14, 0));
        HBox.setHgrow(field, Priority.ALWAYS);
        return row;
    }

    private HBox dateRow(String labelText, DatePicker picker) {
        Label lbl = label(labelText);
        picker.setMaxWidth(Double.MAX_VALUE);
        HBox row = new HBox(12, lbl, picker);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(14, 0, 14, 0));
        HBox.setHgrow(picker, Priority.ALWAYS);
        return row;
    }

    private HBox comboRow(String labelText, ComboBox<LocalTime> combo) {
        Label lbl = label(labelText);
        HBox row = new HBox(12, lbl, combo);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(14, 0, 14, 0));
        return row;
    }

    private VBox textAreaRow(String labelText, TextArea area) {
        Label lbl = label(labelText);
        lbl.setPadding(new Insets(14, 0, 8, 0));
        VBox box = new VBox(0, lbl, area);
        box.setPadding(new Insets(0, 0, 14, 0));
        return box;
    }

    private Label label(String text) {
        Label l = new Label(text);
        l.getStyleClass().add("tt-form-label");
        l.setPrefWidth(90); l.setMinWidth(90);
        return l;
    }

    private Region sep() {
        Region r = new Region();
        r.getStyleClass().add("tt-form-separator");
        r.setPrefHeight(1); r.setMaxHeight(1);
        return r;
    }

    // ── Field factories ───────────────────────────────────────────────────────

    private static TextField styledField(String prompt) {
        TextField tf = new TextField();
        tf.setPromptText(prompt);
        tf.getStyleClass().add("tt-text-field");
        return tf;
    }

    private static DatePicker styledDatePicker() {
        DatePicker dp = new DatePicker();
        dp.setEditable(false);
        dp.getStyleClass().add("tt-date-picker");
        return dp;
    }

    private static TextArea styledTextArea() {
        TextArea ta = new TextArea();
        ta.setPromptText("Optional notes…");
        ta.setPrefRowCount(3);
        ta.setWrapText(true);
        ta.getStyleClass().add("tt-text-area");
        return ta;
    }

    private static ComboBox<LocalTime> buildTimeCombo() {
        ComboBox<LocalTime> cb = new ComboBox<>();
        for (int h = 7; h <= 22; h++) {
            cb.getItems().add(LocalTime.of(h, 0));
            if (h < 22) cb.getItems().add(LocalTime.of(h, 30));
        }
        cb.getStyleClass().add("tt-combo");
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
        cb.setPrefWidth(110);
        return cb;
    }

    private static Label errorLabel() {
        Label l = new Label();
        l.getStyleClass().add("tt-error-label");
        l.setVisible(false); l.setManaged(false);
        return l;
    }

    private static void show(Label l) { l.setVisible(true);  l.setManaged(true);  }
    private static void hide(Label l) { l.setVisible(false); l.setManaged(false); }
}
