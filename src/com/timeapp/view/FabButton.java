package com.timeapp.view;

import com.timeapp.MainApp;
import com.timeapp.controller.DashboardController;
import javafx.animation.*;
import javafx.geometry.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.util.Duration;

/**
 * Circular FAB positioned at the bottom-right.
 * Expands into a speed-dial with two mini-FABs.
 *
 * ── Icon swap fix ─────────────────────────────────────────────────────────────
 * expandDial() / collapseDial() previously called getText() / setText() on the
 * graphic Label to toggle between PLUS and XMARK. setText() preserves the
 * Label's existing font (which was set correctly by IconLabel.of()), so the
 * inline-style fix in IconLabel means this continues to work correctly.
 *
 * However: we now call setIconText() helper which also re-applies the inline
 * font style after setText(), as an extra guarantee in case the Label was
 * ever detached and reattached to the scene graph (which can trigger a CSS
 * re-pass that clears inline styles in buggy JavaFX builds).
 * ─────────────────────────────────────────────────────────────────────────────
 */
public class FabButton {

    private final VBox root;
    private final DashboardController ctrl;

    private final Button btnAddSchedule;
    private final Button btnAddTodo;
    private final Button mainFab;

    // Keep a direct reference to the main FAB's icon Label so we can swap it
    private final Label mainFabIcon;

    private static final String FA_FAMILY = MainApp.FA_SOLID_FAMILY;

    public FabButton(DashboardController ctrl) {
        this.ctrl = ctrl;

        // ── Mini FABs ─────────────────────────────────────────────────────────
        btnAddSchedule = miniFab(IconLabel.CALENDAR_CHECK, "Add Schedule");
        btnAddSchedule.setOnAction(e -> ctrl.onAddSchedule());

        btnAddTodo = miniFab(IconLabel.CIRCLE_CHECK, "Add To-do");
        btnAddTodo.setOnAction(e -> ctrl.onAddTodo());

        // ── Main FAB ──────────────────────────────────────────────────────────
        // Store the icon Label so expandDial/collapseDial can swap its text
        // without losing the inline font style.
        mainFabIcon = IconLabel.of(IconLabel.PLUS, 20, "fab-main-icon");

        mainFab = new Button();
        mainFab.setGraphic(mainFabIcon);
        mainFab.getStyleClass().add("fab-main");
        mainFab.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        mainFab.setOnAction(e -> ctrl.toggleFab());

        // ── Root VBox ─────────────────────────────────────────────────────────
        root = new VBox(8);
        root.setAlignment(Pos.BOTTOM_RIGHT);
        root.getChildren().addAll(btnAddSchedule, btnAddTodo, mainFab);

        // Start collapsed
        btnAddSchedule.setVisible(false);
        btnAddSchedule.setManaged(false);
        btnAddTodo.setVisible(false);
        btnAddTodo.setManaged(false);

        ctrl.fabExpandedProperty().addListener((obs, o, expanded) -> {
            if (expanded) expandDial(); else collapseDial();
        });
    }

    public VBox getNode() { return root; }

    // ── Animation ─────────────────────────────────────────────────────────────

    private void expandDial() {
        // Swap + → ✕  and re-pin the inline font style so CSS can't override it
        swapIcon(mainFabIcon, IconLabel.XMARK, 20);
        mainFab.getStyleClass().add("fab-main-active");
        showMini(btnAddTodo,      0);
        showMini(btnAddSchedule, 80);
    }

    private void collapseDial() {
        swapIcon(mainFabIcon, IconLabel.PLUS, 20);
        mainFab.getStyleClass().remove("fab-main-active");
        hideMini(btnAddTodo);
        hideMini(btnAddSchedule);
    }

    /**
     * Swaps the text on an existing icon Label AND re-applies the inline font
     * style. This is important because setText() alone preserves the font bean
     * property but does NOT re-apply the inline style if it was somehow cleared.
     */
    private void swapIcon(Label iconLabel, String codepoint, double sizePx) {
        iconLabel.setText(codepoint);
        // Re-pin inline style as a guarantee (no-op if already set correctly)
        iconLabel.setStyle(
            "-fx-font-family: '" + FA_FAMILY + "';" +
            "-fx-font-size: " + sizePx + "px;"
        );
    }

    private void showMini(Button btn, int delayMs) {
        btn.setVisible(true);
        btn.setManaged(true);
        btn.setOpacity(0);
        btn.setScaleX(0.5);
        btn.setScaleY(0.5);

        new Timeline(
            new KeyFrame(Duration.millis(delayMs),
                new KeyValue(btn.opacityProperty(), 0),
                new KeyValue(btn.scaleXProperty(), 0.5),
                new KeyValue(btn.scaleYProperty(), 0.5)),
            new KeyFrame(Duration.millis(delayMs + 200),
                new KeyValue(btn.opacityProperty(), 1, Interpolator.EASE_OUT),
                new KeyValue(btn.scaleXProperty(), 1, Interpolator.EASE_OUT),
                new KeyValue(btn.scaleYProperty(), 1, Interpolator.EASE_OUT))
        ).play();
    }

    private void hideMini(Button btn) {
        Timeline tl = new Timeline(
            new KeyFrame(Duration.millis(150),
                new KeyValue(btn.opacityProperty(), 0,   Interpolator.EASE_IN),
                new KeyValue(btn.scaleXProperty(), 0.5, Interpolator.EASE_IN),
                new KeyValue(btn.scaleYProperty(), 0.5, Interpolator.EASE_IN))
        );
        tl.setOnFinished(e -> { btn.setVisible(false); btn.setManaged(false); });
        tl.play();
    }

    // ── Mini FAB factory ──────────────────────────────────────────────────────

    private Button miniFab(String codepoint, String labelText) {
        HBox content = new HBox(8);
        content.setAlignment(Pos.CENTER_RIGHT);

        Label tip     = new Label(labelText);
        tip.getStyleClass().add("fab-mini-label");

        Label iconLbl = IconLabel.of(codepoint, 14, "fab-mini-icon");

        content.getChildren().addAll(tip, iconLbl);

        Button btn = new Button();
        btn.setGraphic(content);
        btn.getStyleClass().add("fab-mini");
        btn.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        return btn;
    }
}
