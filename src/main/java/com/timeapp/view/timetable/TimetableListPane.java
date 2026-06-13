package com.timeapp.view.timetable;

import com.timeapp.controller.DashboardController;
import com.timeapp.ui.model.TimeTable;
import com.timeapp.view.IconLabel;
import javafx.beans.binding.Bindings;
import javafx.collections.ListChangeListener;
import javafx.geometry.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.shape.Circle;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Panel 3 -- TimeTable List & Selector Screen.
 *
 * FIX (v10):
 *   The bottom-right mini-FAB was invisible because ScrollPane(Priority.ALWAYS)
 *   consumed all remaining height, giving the FAB wrapper zero height.
 *
 *   Fix: move the "+" button into the top bar right side (replacing the spacer),
 *   consistent with how Panel 2 (AddClass) and Panel 4 (CreateTimeTable) work.
 *   The bottom FAB row and buildFabRow()/buildMiniFab() methods are removed.
 */
public class TimetableListPane extends VBox {

    private final DashboardController ctrl;
    private final VBox listContainer = new VBox(0);

    public TimetableListPane(DashboardController ctrl) {
        this.ctrl = ctrl;
        getStyleClass().add("page");
        setFillWidth(true);

        getChildren().addAll(buildHeader(), buildScrollArea());

        ctrl.getTimetableList().addListener((ListChangeListener<TimeTable>) c -> buildList());
        ctrl.activeTimetableProperty().addListener((obs, o, n) -> buildList());
        ctrl.dataRevisionProperty().addListener((obs, o, n) -> buildList());

        buildList();
    }

    // ── Header ────────────────────────────────────────────────────────────────

    private HBox buildHeader() {
        HBox bar = new HBox(12);
        bar.getStyleClass().add("tt-panel-header");
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(16, 20, 16, 20));

        // Back to Panel 1
        Label backIcon = IconLabel.of(IconLabel.CHEVRON_LEFT, 18, "topbar-icon");
        Button backBtn = new Button();
        backBtn.setGraphic(backIcon);
        backBtn.getStyleClass().add("icon-btn");
        backBtn.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        backBtn.setOnAction(e -> ctrl.backToTimetableMain());

        // Title (centered)
        Label title = new Label("TimeTable");
        title.getStyleClass().add("tt-panel-title");
        HBox.setHgrow(title, Priority.ALWAYS);
        title.setAlignment(Pos.CENTER);
        title.setMaxWidth(Double.MAX_VALUE);

        // "+" button on the right -- navigates to Panel 4 (Create New TimeTable)
        // FIX: replaces the old Region spacer which was just dead space.
        Label plusIcon = IconLabel.of(IconLabel.PLUS, 18, "tt-save-icon");
        Button addBtn  = new Button();
        addBtn.setGraphic(plusIcon);
        addBtn.getStyleClass().addAll("icon-btn", "tt-save-btn");
        addBtn.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        addBtn.setOnAction(e -> ctrl.showCreateTimetable());
        Tooltip.install(addBtn, new Tooltip("Create New TimeTable"));

        bar.getChildren().addAll(backBtn, title, addBtn);
        return bar;
    }

    // ── Scrollable list ───────────────────────────────────────────────────────

    private ScrollPane buildScrollArea() {
        listContainer.setFillWidth(true);

        ScrollPane sp = new ScrollPane(listContainer);
        sp.setFitToWidth(true);
        sp.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        sp.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        sp.getStyleClass().add("timeline-scroll");
        VBox.setVgrow(sp, Priority.ALWAYS);
        return sp;
    }

    // ── List building ─────────────────────────────────────────────────────────

    private void buildList() {
        listContainer.getChildren().clear();
        LocalDate today = LocalDate.now();

        List<TimeTable> active = ctrl.getTimetableList().stream()
            .filter(tt -> tt.getEndDate() != null && !tt.getEndDate().isBefore(today))
            .collect(Collectors.toList());

        List<TimeTable> archived = ctrl.getTimetableList().stream()
            .filter(tt -> tt.getEndDate() == null || tt.getEndDate().isBefore(today))
            .collect(Collectors.toList());

        if (!active.isEmpty()) {
            listContainer.getChildren().add(sectionHeader("Active / Ongoing"));
            for (TimeTable tt : active)
                listContainer.getChildren().add(buildRow(tt));
        }

        if (!archived.isEmpty()) {
            listContainer.getChildren().add(sectionHeader("Archived / Past"));
            for (TimeTable tt : archived)
                listContainer.getChildren().add(buildRow(tt));
        }

        if (ctrl.getTimetableList().isEmpty()) {
            Label empty = new Label("No timetables yet.\nTap + to create one.");
            empty.getStyleClass().add("tt-empty-hint");
            empty.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
            empty.setAlignment(Pos.CENTER);
            empty.setPadding(new Insets(48, 0, 0, 0));
            empty.setMaxWidth(Double.MAX_VALUE);
            listContainer.getChildren().add(empty);
        }
    }

    private Label sectionHeader(String text) {
        Label lbl = new Label(text);
        lbl.getStyleClass().add("tt-list-section-header");
        lbl.setPadding(new Insets(20, 20, 8, 20));
        lbl.setMaxWidth(Double.MAX_VALUE);
        return lbl;
    }

    private HBox buildRow(TimeTable tt) {
        boolean isActive = tt.equals(ctrl.getActiveTimetable());

        Label nameLbl = new Label(tt.getTitle());
        nameLbl.getStyleClass().add(isActive ? "tt-list-name-active" : "tt-list-name");
        nameLbl.textProperty().bind(tt.titleProperty());

        Label dateLbl = new Label(tt.getDateRangeDisplay());
        dateLbl.getStyleClass().add("tt-list-dates");
        dateLbl.textProperty().bind(Bindings.createStringBinding(
            tt::getDateRangeDisplay,
            tt.startDateProperty(),
            tt.endDateProperty()
        ));

        VBox textBox = new VBox(3, nameLbl, dateLbl);
        HBox.setHgrow(textBox, Priority.ALWAYS);

        Button editBtn = new Button("Edit");
        editBtn.getStyleClass().add("tt-add-time-btn");
        editBtn.setOnAction(e -> { e.consume(); ctrl.editTimetable(tt); });

        Button deleteBtn = new Button("Delete");
        deleteBtn.getStyleClass().add("tt-add-time-btn");
        deleteBtn.setOnAction(e -> { e.consume(); ctrl.deleteTimetable(tt); });

        Circle indicator = new Circle(9);
        indicator.getStyleClass().add(
            isActive ? "tt-list-indicator-active" : "tt-list-indicator-inactive");

        HBox row = new HBox(12, textBox, editBtn, deleteBtn, indicator);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(16, 20, 16, 20));
        row.getStyleClass().add("tt-list-row");
        HBox.setMargin(indicator, new Insets(0, 0, 0, 8));

        Region sep = new Region();
        sep.getStyleClass().add("tt-form-separator");
        sep.setPrefHeight(1); sep.setMaxHeight(1);

        // VBox wrapping row + separator (only the row is returned for getChildren.add)
        row.setOnMouseClicked(e -> ctrl.setActiveTimetable(tt));
        row.setOnMouseEntered(e -> row.getStyleClass().add("tt-list-row-hover"));
        row.setOnMouseExited(e  -> row.getStyleClass().remove("tt-list-row-hover"));

        return row;
    }
}
