package com.timeapp.view;

import com.timeapp.controller.DashboardController;
import javafx.geometry.*;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.scene.shape.Circle;

/**
 * Navigation drawer that slides in from the left.
 * Width: 260 px.  Starts hidden (translateX = −260) and is animated
 * to translateX = 0 by DashboardController.
 *
 * Nav items and icon codepoints
 * ─────────────────────────────
 *  Label       Icon constant          FA codepoint  CSS class
 *  ──────────  ─────────────────────  ────────────  ─────────────────
 *  TimeLine    IconLabel.CLOCK        \uf017        nav-icon
 *  TimeTable   IconLabel.TABLE        \uf0ce        nav-icon          ← fixed
 *  Calendar    IconLabel.CALENDAR     \uf133        nav-icon
 *
 * All icons are created via IconLabel.of() which:
 *   1. Sets Font.font("Font Awesome 6 Free Solid", size) in Java code.
 *   2. Adds the CSS class "glyph-icon" for colour control in app.css.
 *   3. Adds the caller-supplied class (e.g. "nav-icon") for size/spacing.
 *
 * This keeps Java code free of raw colour/size values — those live in app.css.
 */
public class SidebarDrawer {

    private static final double WIDTH = 260;

    private final VBox root;
    private final DashboardController ctrl;

    /** Tracks which nav row is currently highlighted. */
    private String activeNav = "TimeLine";

    public SidebarDrawer(DashboardController ctrl) {
        this.ctrl = ctrl;

        root = new VBox();
        root.setPrefWidth(WIDTH);
        root.getStyleClass().add("sidebar");
        root.setFillWidth(true);

        root.getChildren().add(buildHeader());

        Region divider = new Region();
        divider.getStyleClass().add("sidebar-divider");
        root.getChildren().add(divider);

        root.getChildren().add(buildNavItems());

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);
        root.getChildren().add(spacer);

        root.getChildren().add(buildBottomSection());

        // Register with controller for slide animation
        ctrl.wireSidebarPane(root);
    }

    public VBox getNode() { return root; }

    // ── Header ────────────────────────────────────────────────────────────────

    private VBox buildHeader() {
        Circle avatar = new Circle(28);
        avatar.getStyleClass().add("sidebar-avatar");

        Label initials = new Label("TF");
        initials.getStyleClass().add("sidebar-avatar-label");

        StackPane avatarStack = new StackPane(avatar, initials);
        avatarStack.setAlignment(Pos.CENTER);

        Label appName = new Label("TimeFlow");
        appName.getStyleClass().add("sidebar-app-name");

        Label subtitle = new Label("Stay on track ✦");
        subtitle.getStyleClass().add("sidebar-subtitle");

        VBox header = new VBox(8, avatarStack, appName, subtitle);
        header.setAlignment(Pos.CENTER);
        header.setPadding(new Insets(40, 20, 24, 20));
        return header;
    }

    // ── Nav items ─────────────────────────────────────────────────────────────

    /**
     * Builds the three navigation rows.
     *
     * Each row is: [glyph icon] [label + sublabel VBox]
     *
     * Icon is produced by IconLabel.of(codepoint, size, styleClass):
     *   - codepoint  : FA solid Unicode from the IconLabel constants
     *   - size        : 16 px — matching the "nav-icon" CSS font-size
     *   - styleClass  : "nav-icon" — targets colour in app.css
     */
    private VBox buildNavItems() {
        // { codepoint, label, sublabel }
        String[][] navItems = {
            { IconLabel.CLOCK,    "TimeLine",  "Your daily view"  },
            { IconLabel.TABLE,    "TimeTable", "Weekly schedule"  },   // \uf0ce
            { IconLabel.CALENDAR, "Calendar",  "Monthly overview" },
        };

        VBox nav = new VBox(4);
        nav.setPadding(new Insets(8, 12, 8, 12));

        for (String[] item : navItems) {
            String codepoint = item[0];
            String label     = item[1];
            String sublabel  = item[2];

            nav.getChildren().add(buildNavRow(codepoint, label, sublabel));
        }

        return nav;
    }

    private HBox buildNavRow(String codepoint, String label, String sublabel) {
        HBox row = new HBox(14);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(12, 16, 12, 16));
        row.getStyleClass().add("nav-item");

        if (label.equals(activeNav)) {
            row.getStyleClass().add("nav-item-active");
        }

        // ── Icon ── created with IconLabel; font is set in Java, colour in CSS
        Label iconLbl = IconLabel.of(codepoint, 16, "nav-icon");

        // ── Text ──
        VBox textBox = new VBox(1);
        Label nameLbl = new Label(label);
        nameLbl.getStyleClass().add(
            label.equals(activeNav) ? "nav-label-active" : "nav-label");

        Label subLbl = new Label(sublabel);
        subLbl.getStyleClass().add("nav-sublabel");

        textBox.getChildren().addAll(nameLbl, subLbl);
        row.getChildren().addAll(iconLbl, textBox);

        // ── Interactions ──
        row.setOnMouseClicked(e -> {
            activeNav = label;
            // Rebuild nav rows to refresh active state
            rebuildActiveState((VBox) row.getParent());
            ctrl.navigateTo(label);
        });

        row.setOnMouseEntered(e -> {
            if (!label.equals(activeNav))
                row.getStyleClass().add("nav-item-hover");
        });
        row.setOnMouseExited(e ->
            row.getStyleClass().remove("nav-item-hover"));

        return row;
    }

    /** Refreshes active/inactive CSS classes on all nav rows after selection. */
    private void rebuildActiveState(VBox navContainer) {
        for (var child : navContainer.getChildren()) {
            if (!(child instanceof HBox row)) continue;

            row.getStyleClass().remove("nav-item-active");

            // Find the label node (second child of the HBox)
            if (row.getChildren().size() < 2) continue;
            var textBox = row.getChildren().get(1);
            if (!(textBox instanceof VBox vb) || vb.getChildren().isEmpty()) continue;
            var firstLabel = vb.getChildren().get(0);
            if (!(firstLabel instanceof Label nameLbl)) continue;

            boolean isActive = nameLbl.getText().equals(activeNav);
            nameLbl.getStyleClass().removeAll("nav-label", "nav-label-active");
            nameLbl.getStyleClass().add(isActive ? "nav-label-active" : "nav-label");

            if (isActive) {
                row.getStyleClass().add("nav-item-active");
            }
        }
    }

    // ── Bottom section ────────────────────────────────────────────────────────

    private HBox buildBottomSection() {
        HBox bottom = new HBox(10);
        bottom.setAlignment(Pos.CENTER_LEFT);
        bottom.setPadding(new Insets(16, 20, 40, 20));

        // Settings icon — \uf013
        Label gear = IconLabel.of(IconLabel.COG, 16, "sidebar-bottom-icon");
        gear.setOnMouseClicked(e -> System.out.println("[Nav] Settings"));

        // Info icon — \uf129
        Label info = IconLabel.of(IconLabel.INFO_CIRCLE, 16, "sidebar-bottom-icon");
        info.setOnMouseClicked(e -> System.out.println("[Nav] About"));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label version = new Label("v1.0.0");
        version.getStyleClass().add("sidebar-version");

        bottom.getChildren().addAll(gear, info, spacer, version);
        return bottom;
    }
}
