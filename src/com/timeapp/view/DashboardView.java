package com.timeapp.view;

import com.timeapp.controller.DashboardController;
import javafx.geometry.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Assembles the full dashboard layout.
 *
 * Layer stack (bottom → top in AnchorPane z-order):
 *   1. BorderPane  — top bar + week strip + timeline (the "page")
 *   2. Dim overlay — semi-transparent cover shown when sidebar is open
 *   3. SidebarDrawer — slides over from the left
 *   4. FAB          — fixed bottom-right
 *
 * ── Icon strategy ────────────────────────────────────────────────────────────
 * All icon buttons in the top bar use Button.setGraphic(IconLabel.of(...))
 * instead of Button.setText(unicodeString).  This is the correct JavaFX
 * pattern because:
 *   • setGraphic() places the node in the button's content slot — the font is
 *     already set on the Label by IconLabel.of(), so CSS parse order doesn't
 *     matter.
 *   • setText() on a Button goes through CSS -fx-font-family resolution, which
 *     can fail if the font wasn't registered before CSS was applied.
 * ─────────────────────────────────────────────────────────────────────────────
 */
public class DashboardView {

    private static final DateTimeFormatter HEADER_FMT =
            DateTimeFormatter.ofPattern("EEE, MMM d", Locale.ENGLISH);

    private final AnchorPane root;
    private final DashboardController ctrl;

    public DashboardView() {
        ctrl = new DashboardController();
        root = new AnchorPane();
        root.getStyleClass().add("app-root");
        build();
    }

    public AnchorPane getRoot() { return root; }

    // ── Build ─────────────────────────────────────────────────────────────────

    private void build() {
        // ── 1. Main page ──────────────────────────────────────────────────────
        BorderPane page = new BorderPane();
        page.getStyleClass().add("page");

        VBox topSection = new VBox();
        topSection.getChildren().addAll(buildTopBar(), buildWeekStrip());
        page.setTop(topSection);

        TimelinePane timeline = new TimelinePane(390, ctrl.getDayEntries());
        page.setCenter(timeline.getNode());

        AnchorPane.setTopAnchor(page, 0.0);
        AnchorPane.setBottomAnchor(page, 0.0);
        AnchorPane.setLeftAnchor(page, 0.0);
        AnchorPane.setRightAnchor(page, 0.0);
        root.getChildren().add(page);

        // ── 2. Dim overlay ────────────────────────────────────────────────────
        Rectangle overlay = new Rectangle();
        overlay.getStyleClass().add("dim-overlay");
        overlay.setFill(Color.rgb(0, 0, 0, 0.35));
        overlay.widthProperty().bind(root.widthProperty());
        overlay.heightProperty().bind(root.heightProperty());
        overlay.setVisible(false);
        overlay.setOnMouseClicked(e -> ctrl.closeSidebar());

        ctrl.sidebarOpenProperty().addListener((obs, o, open) -> {
            overlay.setVisible(open);
            if (open) ctrl.collapseFab();
        });

        AnchorPane.setTopAnchor(overlay, 0.0);
        AnchorPane.setLeftAnchor(overlay, 0.0);
        root.getChildren().add(overlay);

        // ── 3. Sidebar drawer ─────────────────────────────────────────────────
        SidebarDrawer sidebar = new SidebarDrawer(ctrl);
        VBox sidebarNode = sidebar.getNode();

        Rectangle clip = new Rectangle();
        clip.widthProperty().bind(sidebarNode.widthProperty());
        clip.heightProperty().bind(sidebarNode.heightProperty());
        sidebarNode.setClip(clip);

        AnchorPane.setTopAnchor(sidebarNode, 0.0);
        AnchorPane.setBottomAnchor(sidebarNode, 0.0);
        AnchorPane.setLeftAnchor(sidebarNode, 0.0);
        root.getChildren().add(sidebarNode);

        // ── 4. FAB ────────────────────────────────────────────────────────────
        FabButton fab = new FabButton(ctrl);
        VBox fabNode = fab.getNode();
        AnchorPane.setRightAnchor(fabNode, 20.0);
        AnchorPane.setBottomAnchor(fabNode, 36.0);
        root.getChildren().add(fabNode);

        page.setOnMouseClicked(e -> ctrl.collapseFab());
    }

    // ── Top Bar ───────────────────────────────────────────────────────────────

    private HBox buildTopBar() {
        HBox bar = new HBox();
        bar.getStyleClass().add("top-bar");
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(16, 20, 12, 20));
        bar.setSpacing(12);

        // ── Hamburger button ──────────────────────────────────────────────────
        // Use setGraphic(IconLabel) — NOT setText(unicode) — so the FA font is
        // guaranteed applied before the button is laid out.
        Label hamburgerIcon = IconLabel.of(IconLabel.BARS, 18, "topbar-icon");
        Button hamburger = new Button();
        hamburger.setGraphic(hamburgerIcon);
        hamburger.getStyleClass().add("icon-btn");
        hamburger.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        hamburger.setOnAction(e -> ctrl.toggleSidebar());
        Tooltip.install(hamburger, new Tooltip("Menu"));

        // ── Date label ────────────────────────────────────────────────────────
        Label dateLbl = new Label(LocalDate.now().format(HEADER_FMT));
        dateLbl.getStyleClass().add("header-date");
        ctrl.selectedDayProperty().addListener((obs, o, n) ->
            dateLbl.setText(n.format(HEADER_FMT)));
        HBox.setHgrow(dateLbl, Priority.ALWAYS);
        dateLbl.setAlignment(Pos.CENTER_LEFT);
        dateLbl.setMaxWidth(Double.MAX_VALUE);

        // ── Back to Today button ──────────────────────────────────────────────
        Label rotateIcon = IconLabel.of(IconLabel.ROTATE_RIGHT, 18, "topbar-icon");
        Button todayBtn = new Button();
        todayBtn.setGraphic(rotateIcon);
        todayBtn.getStyleClass().add("icon-btn");
        todayBtn.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        todayBtn.setOnAction(e -> ctrl.backToToday());
        Tooltip.install(todayBtn, new Tooltip("Back to Today"));

        bar.getChildren().addAll(hamburger, dateLbl, todayBtn);
        return bar;
    }

    // ── Week Strip ────────────────────────────────────────────────────────────

    private HBox buildWeekStrip() {
        return new WeekStripBar(ctrl).getNode();
    }
}
