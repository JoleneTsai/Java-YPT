package com.timeapp.view.timetable;

import com.timeapp.controller.DashboardController;
import com.timeapp.ui.model.TimeTable;
import com.timeapp.view.IconLabel;
import javafx.collections.ListChangeListener;
import javafx.geometry.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.shape.Circle;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Panel 3 ??TimeTable List & Selector Screen.
 *
 * Triggered from Panel 1's FAB ??"TimeTable".
 *
 * Shows all known semesters in two sections:
 *   ??"Active / Ongoing"  ??endDate >= today
 *   ??"Archived / Past"   ??endDate <  today
 *
 * Selection indicator:
 *   Active timetable ??filled blue circle  (.tt-list-indicator-active)
 *   Other entries    ??outline grey circle (.tt-list-indicator-inactive)
 *
 * Tapping a row calls ctrl.setActiveTimetable(tt), which triggers the
 * activeTimetableProperty listener ??buildList() rebuilds to reflect
 * the new selection instantly.
 *
 * Mini-FAB (bottom-right) navigates to Panel 4 to create a new timetable.
 */
public class TimetableListPane extends VBox {

    private final DashboardController ctrl;
    private final VBox listContainer = new VBox(0);

    public TimetableListPane(DashboardController ctrl) {
        this.ctrl = ctrl;
        getStyleClass().add("page");
        setFillWidth(true);

        getChildren().addAll(buildHeader(), buildScrollArea(), buildFabRow());

        // Rebuild list whenever the timetable master list changes
        ctrl.getTimetableList().addListener(
            (ListChangeListener<TimeTable>) c -> buildList());
        // Rebuild when the active timetable selection changes (circle indicators)
        ctrl.activeTimetableProperty().addListener(
            (obs, o, n) -> buildList());

        buildList();
    }

    // ?? Header ????????????????????????????????????????????????????????????????

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

        Label title = new Label("TimeTable");
        title.getStyleClass().add("tt-panel-title");
        HBox.setHgrow(title, Priority.ALWAYS);
        title.setAlignment(Pos.CENTER);
        title.setMaxWidth(Double.MAX_VALUE);

        // Right spacer keeps title centred despite the left-aligned back button
        Region spacer = new Region();
        spacer.setPrefWidth(40);
        spacer.setMinWidth(40);

        bar.getChildren().addAll(backBtn, title, spacer);
        return bar;
    }

    // ?? Scrollable list ???????????????????????????????????????????????????????

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

    // ?? Mini FAB row (bottom-right) ???????????????????????????????????????????

    private StackPane buildFabRow() {
        Button miniFab = buildMiniFab();
        StackPane wrapper = new StackPane(miniFab);
        StackPane.setAlignment(miniFab, Pos.BOTTOM_RIGHT);
        wrapper.setPadding(new Insets(0, 20, 36, 0));
        wrapper.setPickOnBounds(false);
        VBox.setVgrow(wrapper, Priority.NEVER);
        return wrapper;
    }

    // ?? List building ?????????????????????????????????????????????????????????

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

        Label dateLbl = new Label(tt.getDateRangeDisplay());
        dateLbl.getStyleClass().add("tt-list-dates");

        VBox textBox = new VBox(3, nameLbl, dateLbl);
        HBox.setHgrow(textBox, Priority.ALWAYS);

        Button editBtn = new Button("Edit");
        editBtn.getStyleClass().add("tt-add-time-btn");
        editBtn.setOnAction(e -> {
            e.consume();
            ctrl.editTimetable(tt);
        });

        Button deleteBtn = new Button("Delete");
        deleteBtn.getStyleClass().add("tt-add-time-btn");
        deleteBtn.setOnAction(e -> {
            e.consume();
            ctrl.deleteTimetable(tt);
        });

        // Selection indicator circle
        Circle indicator = new Circle(9);
        indicator.getStyleClass().add(
            isActive ? "tt-list-indicator-active" : "tt-list-indicator-inactive");

        HBox row = new HBox(12, textBox, editBtn, deleteBtn, indicator);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(16, 20, 16, 20));
        row.getStyleClass().add("tt-list-row");
        HBox.setMargin(indicator, new Insets(0, 0, 0, 8));

        // Thin separator line below each row
        Region sep = new Region();
        sep.getStyleClass().add("tt-form-separator");
        sep.setPrefHeight(1);
        sep.setMaxHeight(1);

        VBox rowWithSep = new VBox(row, sep);

        // Click ??change active timetable (triggers buildList() via listener)
        row.setOnMouseClicked(e -> ctrl.setActiveTimetable(tt));
        row.setOnMouseEntered(e -> row.getStyleClass().add("tt-list-row-hover"));
        row.setOnMouseExited(e  -> row.getStyleClass().remove("tt-list-row-hover"));

        return row;
    }

    // ?? Mini FAB ??Panel 4 ????????????????????????????????????????????????????

    private Button buildMiniFab() {
        Label plusIcon = IconLabel.of(IconLabel.PLUS, 18, "fab-main-icon");
        Button btn = new Button();
        btn.setGraphic(plusIcon);
        btn.getStyleClass().addAll("fab-main", "tt-list-mini-fab");
        btn.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        btn.setOnAction(e -> ctrl.showCreateTimetable());
        Tooltip.install(btn, new Tooltip("Create New TimeTable"));
        return btn;
    }
}
