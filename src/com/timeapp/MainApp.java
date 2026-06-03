package com.timeapp;

import com.timeapp.view.DashboardView;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.text.Font;
import javafx.stage.Stage;

/**
 * Main entry point for the Time Management App.
 * Simulates a mobile screen (390×844 — iPhone 14 Pro resolution).
 *
 * ── Font loading strategy ────────────────────────────────────────────────────
 * Font.loadFont() MUST be called here, before any Scene/Stage is created,
 * so the font is registered with JavaFX's font engine BEFORE CSS is parsed.
 *
 * The call uses getResourceAsStream() (not toExternalForm()) because:
 *   • getResourceAsStream() works identically from both IDE and fat-jar.
 *   • getResource().toExternalForm() can produce a "jar:file:..." URL that
 *     Font.loadFont() rejects on some JVM/OS combinations.
 *
 * The second argument (size) is the registration size; it does NOT lock the
 * font to that size — CSS / Label.setFont() can still use any size freely.
 * We use 10 as a safe, non-zero sentinel value.
 * ─────────────────────────────────────────────────────────────────────────────
 */
public class MainApp extends Application {

    public static final double APP_WIDTH  = 390;
    public static final double APP_HEIGHT = 844;

    /**
     * The CSS font-family name that JavaFX registers for fa-solid-900.ttf.
     * Confirmed from the TTF name table (name_id=1): "Font Awesome 6 Free Solid"
     * Use this exact string in app.css: -fx-font-family: "Font Awesome 6 Free Solid";
     */
    public static final String FA_SOLID_FAMILY = "Font Awesome 6 Free Solid";

    @Override
    public void start(Stage primaryStage) throws Exception {

        // ── Step 1: Register FontAwesome before Scene creation ────────────────
        loadFontAwesome();

        // ── Step 2: Build the scene ───────────────────────────────────────────
        DashboardView dashboard = new DashboardView();

        Scene scene = new Scene(dashboard.getRoot(), APP_WIDTH, APP_HEIGHT);
        scene.getStylesheets().add(
            getClass().getResource("/com/timeapp/css/app.css").toExternalForm()
        );

        primaryStage.setTitle("TimeFlow — Dashboard");
        primaryStage.setScene(scene);
        primaryStage.setResizable(false);
        primaryStage.show();
    }

    /**
     * Loads the FontAwesome Solid TTF from the classpath resource directory.
     *
     * Diagnostic: if the font loads successfully, Font.loadFont() returns a Font
     * whose getName() is "Font Awesome 6 Free Solid".  If it returns a Font with
     * getName() = "System" (or null), the file path is wrong or the file is corrupt.
     */
    private void loadFontAwesome() {
        // Primary file: fa-solid-900.ttf  (standard web-download name)
        boolean loaded = tryLoad("/com/timeapp/fonts/fa-solid-900.ttf");

        if (!loaded) {
            // Fallback: the OTF variant shipped under the full official name
            loaded = tryLoad("/com/timeapp/fonts/Font Awesome 6 Free-Solid-900.otf");
        }

        if (!loaded) {
            System.err.println(
                "[TimeFlow] WARNING: FontAwesome icon font could not be loaded.\n" +
                "  → Icon glyphs will render as □ boxes.\n" +
                "  → Ensure 'fa-solid-900.ttf' is present in:\n" +
                "      src/main/resources/com/timeapp/fonts/\n" +
                "  → Font family name expected by CSS: \"" + FA_SOLID_FAMILY + "\""
            );
        }
    }

    private boolean tryLoad(String resourcePath) {
        try (var stream = getClass().getResourceAsStream(resourcePath)) {
            if (stream == null) return false;
            Font loaded = Font.loadFont(stream, 10);
            // loadFont returns null on failure; "System" means the font was not recognised
            boolean ok = loaded != null && !loaded.getFamily().equals("System");
            if (ok) {
                System.out.println("[TimeFlow] Font loaded: " + loaded.getFamily()
                    + " from " + resourcePath);
            }
            return ok;
        } catch (Exception e) {
            return false;
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
