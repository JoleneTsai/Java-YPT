package com.timeapp.view;

import com.timeapp.MainApp;
import javafx.scene.control.Label;
import javafx.scene.text.Font;

/**
 * Factory for Labels that render FontAwesome Solid glyph icons.
 *
 * ── ROOT CAUSE OF BLANK ICONS (and why this fix works) ───────────────────────
 *
 * JavaFX CSS priority order (highest → lowest):
 *   1. Inline style  — node.setStyle("-fx-font-family: ...")   ← WINS
 *   2. Author CSS    — app.css rules like .app-root, .glyph-icon
 *   3. User-agent    — JavaFX default stylesheet
 *   4. Bean property — node.setFont(Font.font(...))            ← LOSES
 *
 * The bug: app.css sets  `.app-root { -fx-font-family: "Segoe UI"; }`
 * This cascades to EVERY Label descendant, including icon Labels, and
 * overrides Font.font() (priority 4) because CSS (priority 2) beats it.
 *
 * Icons that appeared to "work" (COG, CLOCK, TABLE, etc.) were not actually
 * using FontAwesome — those codepoints happen to have glyphs in Windows'
 * "Segoe UI Symbol" or "Segoe UI" fonts, which is what was really rendering.
 *
 * THE FIX: call label.setStyle("-fx-font-family: '...'; -fx-font-size: ...px;")
 * Inline style (priority 1) beats CSS cascade (priority 2) unconditionally.
 * This is the only reliable way to pin a specific font on a node in JavaFX
 * when a parent sets -fx-font-family in CSS.
 *
 * ── Icon codepoints (FontAwesome 6 Free Solid — verified in fa-solid-900.ttf) ─
 *
 *  Constant          Unicode   Icon name        Used in
 *  ────────────────  ────────  ───────────────  ────────────────────────────
 *  BARS              \uf0c9   bars / hamburger  TopBar left button
 *  CLOCK             \uf017   clock             TimeLine nav item
 *  TABLE             \uf0ce   table             TimeTable nav item
 *  CALENDAR          \uf133   calendar          Calendar nav item
 *  COG               \uf013   gear              Sidebar bottom bar
 *  INFO_CIRCLE       \uf129   info              Sidebar bottom bar
 *  PLUS              \uf067   plus              FAB main button
 *  ROTATE_RIGHT      \uf01e   rotate-right      TopBar "back to today"
 *  CALENDAR_CHECK    \uf274   calendar-check    FAB "Add Schedule" mini
 *  CIRCLE_CHECK      \uf058   circle-check      FAB "Add To-do" mini
 *  XMARK             \uf00d   xmark / close     FAB main when expanded
 * ─────────────────────────────────────────────────────────────────────────────
 */
public final class IconLabel {

    // ── Verified codepoints (all confirmed present in fa-solid-900.ttf) ───────

    /** ☰  bars — hamburger / menu button */
    public static final String BARS           = "\uf0c9";

    /** 🕐  clock — TimeLine nav item */
    public static final String CLOCK          = "\uf017";

    /** 🗃  table — TimeTable nav item  (U+F0CE) */
    public static final String TABLE          = "\uf0ce";

    /** 📅  calendar — Calendar nav item */
    public static final String CALENDAR       = "\uf133";

    /** ⚙  gear — settings */
    public static final String COG            = "\uf013";

    /** ℹ  info-circle */
    public static final String INFO_CIRCLE    = "\uf129";

    /** +  plus — FAB main button */
    public static final String PLUS           = "\uf067";

    /** ↻  rotate-right — "back to today" */
    public static final String ROTATE_RIGHT   = "\uf01e";

    /** 📅✓  calendar-check — "Add Schedule" */
    public static final String CALENDAR_CHECK = "\uf274";

    /** ✅  circle-check — "Add To-do" */
    public static final String CIRCLE_CHECK   = "\uf058";

    /** ✕  xmark — close / collapse FAB */
    public static final String XMARK          = "\uf00d";

    /** ‹  chevron-left — back navigation */
    public static final String CHEVRON_LEFT   = "\uf053";

    /** ✓  check — save / confirm */
    public static final String CHECK          = "\uf00c";

    // ── Font family name (from TTF name table, name_id=1) ─────────────────────
    private static final String FA_FAMILY = MainApp.FA_SOLID_FAMILY;

    // ── Factory ───────────────────────────────────────────────────────────────

    /**
     * Creates a Label that reliably renders a FontAwesome Solid glyph.
     *
     * Font is applied via BOTH:
     *   • Font.font()   — sets the bean property (fallback)
     *   • setStyle()    — sets inline style (priority 1, beats CSS cascade)
     *
     * The inline style is the critical line. Without it, any ancestor node
     * that sets -fx-font-family in CSS will cascade down and override the
     * Font.font() bean property, causing the icon to render in the wrong font.
     *
     * @param codepoint  FA Solid unicode string, e.g. {@code IconLabel.TABLE}
     * @param sizePx     desired icon size in pixels
     * @param styleClass additional CSS class for colour/opacity (optional)
     */
    public static Label of(String codepoint, double sizePx, String styleClass) {
        Label lbl = new Label(codepoint);

        // ── Priority 4: bean property (fallback, may be overridden by CSS) ──
        lbl.setFont(Font.font(FA_FAMILY, sizePx));

        // ── Priority 1: inline style (WINS over all CSS cascade) ─────────────
        // This is the definitive fix. It pins both family and size so no
        // ancestor's -fx-font-family rule can overwrite it.
        lbl.setStyle(
            "-fx-font-family: '" + FA_FAMILY + "';" +
            "-fx-font-size: " + sizePx + "px;"
        );

        // Base class — lets app.css control colour/opacity without touching font
        lbl.getStyleClass().add("glyph-icon");

        if (styleClass != null && !styleClass.isBlank()) {
            lbl.getStyleClass().add(styleClass);
        }

        return lbl;
    }

    /** Convenience overload — no extra style class. */
    public static Label of(String codepoint, double sizePx) {
        return of(codepoint, sizePx, null);
    }

    private IconLabel() {}
}
