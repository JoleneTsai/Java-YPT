package com.timeapp.view;

import com.timeapp.controller.DashboardController;
import com.timeapp.domain.model.ToDoTask;
import javafx.geometry.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Full-panel "Add To-Do" form — replaces the plain TextInputDialog.
 *
 * Slides in from the right when ctrl.overlayPaneProperty() == "ADD_TODO".
 * Same visual design language as AddSchedulePane, TimetableAddClassPane, etc.
 *
 * Layout:
 *   [‹]    Add To-Do    [✓]      ← header
 *   Title  [____________]
 *   Date   [DatePicker  ]
 *   Time   [09:00 ▾     ]
 */
public class AddTodoPane extends VBox {

    private final DashboardController ctrl;

    private final TextField           titleField = styledField("e.g. Review lecture notes");
    private final DatePicker          datePicker = styledDatePicker();
    private final ComboBox<LocalTime> timeBox    = buildTimeCombo();

    private final Label titleError = errorLabel();

    public AddTodoPane(DashboardController ctrl) {
        this.ctrl = ctrl;
        getStyleClass().add("page");
        setFillWidth(true);

        timeBox.setValue(LocalTime.of(9, 0));

        getChildren().addAll(buildHeader(), buildForm());

        ctrl.overlayPaneProperty().addListener((obs, from, to) -> {
            if ("ADD_TODO".equals(to)) {
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

        Label title = new Label("Add To-Do");
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

    private VBox buildForm() {
        VBox form = new VBox(0);
        form.getStyleClass().add("tt-form");
        form.setPadding(new Insets(8, 20, 48, 20));

        form.getChildren().addAll(
            fieldRow("Title",     titleField),
            titleError,
            sep(),
            dateRow("Date",      datePicker),
            sep(),
            comboRow("Time slot", timeBox),
            sep()
        );

        return form;
    }

    // ── Save ─────────────────────────────────────────────────────────────────

    private void onSave() {
        String text = titleField.getText().trim();
        if (text.isEmpty()) {
            titleError.setText("Title is required.");
            show(titleError);
            titleField.getStyleClass().add("tt-field-error");
            return;
        }
        hide(titleError);
        titleField.getStyleClass().remove("tt-field-error");

        ToDoTask task = new ToDoTask(text, false);
        LocalDate d = datePicker.getValue();
        LocalTime t = timeBox.getValue();
        if (d != null && t != null) task.setScheduled(d, t);

        ctrl.saveTodo(task);
    }

    // ── Reset ─────────────────────────────────────────────────────────────────

    public void resetForm(LocalDate defaultDate) {
        titleField.clear();
        datePicker.setValue(defaultDate != null ? defaultDate : LocalDate.now());
        timeBox.setValue(LocalTime.of(9, 0));
        hide(titleError);
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
