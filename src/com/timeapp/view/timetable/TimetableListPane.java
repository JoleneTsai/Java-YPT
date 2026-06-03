package com.timeapp.view.timetable;

import com.timeapp.controller.DashboardController;
import com.timeapp.model.TimeTable;
import com.timeapp.view.IconLabel;
import javafx.geometry.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.shape.Circle;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Panel 3 — TimeTable List & Selector Screen.
 *
 * Triggered from Panel 1's FAB → "TimeTable".
 *
 * Layout:
 *   ┌─────────────────────────────────────────┐
 *   │  [‹]         TimeTable                  │  ← header
 *   ├─────────────────────────────────────────┤
 *   │  Active / Ongoing                       │  ← section header
 *   │  ┌───────────────────────────────────┐  │
 *   │  │  113-2 Semester Spring            ●  │  ← active (filled circle)
 *   │  │  2025/02/17 – 2025/07/18          │  │
 *   │  └───────────────────────────────────┘  │
 *   │  Archived / Past                        │  ← section header
 *   │  ┌───────────────────────────────────┐  │
 *   │  │  113-1 Semester Fall              ○  │  ← inactive (outline circle)
 *   │  │  2024/09/02 – 2025/01/17          │  │
 *   │  └───────────────────────────────────┘  │
 *   └─────────────────────────────────────────┘
 *                                    [+] mini-FAB → Panel 4
 *
 * Selection logic:
 *   Clicking a row calls ctrl.setActiveTimetable(tt).
 *   The list rebuilds (via ObservableList listener) to update the circle icons.
 *
 * Active vs Archived classification:
 *   A timetable is "Active/Ongoing" when endDate >= today.
 *   It is "Archived/Past" when endDate < today.
 */
public class TimetableListPane extends VBox {

    private final DashboardController ctrl;
    private final VBox listContainer = new VBox(0);

    public TimetableListPane(DashboardController ctrl) {
        this.ctrl = ctrl;
        getStyleClass().add("page");
        setFillWidth(true);

        getChildren().addAll(buildHeader(), buildScrollArea());

        // Mini FAB — open Panel 4
        // (Must be in an AnchorPane for absolute positioning;
        //  wrap in a relative-positioned container here)
        StackPane fabWrapper = new StackPane(buildMiniFab());
        StackPane.setAlignment(fabWrapper.getChildren().get(0), Pos.BOTTOM_RIGHT);
        fabWrapper.setPadding(new Insets(0, 20, 36, 0));
        fabWrapper.setPickOnBounds(false);
        VBox.setVgrow(fabWrapper, Priority.NEVER);

        // We need the mini fab anchored — swap this VBox to AnchorPane
        // by injecting the fab into the scroll area's parent AnchorPane.
        // Since this is a VBox root, place the fab in a StackPane overlay.
        getChildren().add(fabWrapper);

        // Rebuild list when timetables change or active changes
        ctrl.getTimetableList().addListener(
            (javafx.collections.ListChangeListener<TimeTable>) c -> buildList());
        ctrl.activeTimetableProperty().addListener(
            (obs, o, n) -> buildList());

        buildList();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Header
    // ═══════════════════════════════════════════════════════════════════════════

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

        // Right spacer matches back button width for visual centering of title
        Region spacer = new Region();
        spacer.setPrefWidth(40); spacer.setMinWidth(40);

        bar.getChildren().addAll(backBtn, title, spacer);
        return bar;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Scrollable list area
    // ═══════════════════════════════════════════════════════════════════════════

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

    // ═══════════════════════════════════════════════════════════════════════════
    // List building
    // ═══════════════════════════════════════════════════════════════════════════

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

        // ── Left: name + date range ───────────────────────────────────────────
        Label nameLbl = new Label(tt.getTitle());
        nameLbl.getStyleClass().add(isActive ? "tt-list-name-active" : "tt-list-name");

        Label dateLbl = new Label(tt.getDateRangeDisplay());
        dateLbl.getStyleClass().add("tt-list-dates");

        VBox textBox = new VBox(3, nameLbl, dateLbl);
        HBox.setHgrow(textBox, Priority.ALWAYS);

        // ── Right: selection indicator ────────────────────────────────────────
        //   Active timetable → filled circle (solid dot via CSS fill)
        //   Inactive         → outline circle (stroke only)
        Circle indicator = new Circle(9);
        if (isActive) {
            indicator.getStyleClass().add("tt-list-indicator-active");
        } else {
            indicator.getStyleClass().add("tt-list-indicator-inactive");
        }

        HBox row = new HBox(12, textBox, indicator);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(16, 20, 16, 20));
        row.getStyleClass().add("tt-list-row");
        HBox.setMargin(indicator, new Insets(0, 0, 0, 8));

        // Separator line below row
        VBox rowWithSep = new VBox();
        rowWithSep.getChildren().add(row);
        Region sep = new Region();
        sep.getStyleClass().add("tt-form-separator");
        sep.setPrefHeight(1); sep.setMaxHeight(1);
        rowWithSep.getChildren().add(sep);

        // Click → set active timetable
        row.setOnMouseClicked(e -> ctrl.setActiveTimetable(tt));
        row.setOnMouseEntered(e -> row.getStyleClass().add("tt-list-row-hover"));
        row.setOnMouseExited(e ->  row.getStyleClass().remove("tt-list-row-hover"));

        return row;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Mini FAB → Panel 4
    // ═══════════════════════════════════════════════════════════════════════════

    private Button buildMiniFab() {
        Label plusIcon = IconLabel.of(IconLabel.PLUS, 18, "fab-main-icon");
        Button btn = new Button();
        btn.setGraphic(plusIcon);
        btn.getStyleClass().addAll("fab-main", "tt-list-mini-fab");
        btn.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        btn.setOnAction(e -> ctrl.showCreateTimetable());
        Tooltip.install(btn, new Tooltip("Create New TimeTable"));
        StackPane.setAlignment(btn, Pos.BOTTOM_RIGHT);
        return btn;
    }
}
