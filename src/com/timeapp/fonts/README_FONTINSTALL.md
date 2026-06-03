Font Awesome installation for TimeFlow

To enable FontAwesome glyph icons within the app, download the Font Awesome Free 'Solid' webfont
and place the OTF/TTF files into this folder.

Recommended files (place one of these in this directory):

- Font Awesome 6 Free-Solid-900.otf
- fa-solid-900.ttf

Download:
- Font Awesome Free: https://fontawesome.com/download (choose the Free version)
  or direct CDN for individual font files via Font Awesome's web downloads.

Why add a font file here?
- JavaFX renders glyph icons when the icon font is available locally (or loaded at runtime).
- The app will try to load the above filenames automatically on startup.

Icons used by the app (codepoints)
- BARS (menu / hamburger):  \uf0c9
- CALENDAR:               \uf133
- COG (settings):         \uf013
- INFO (info-circle):     \uf129
- PLUS:                   \uf067
- SYNC (refresh):         \uf021

If you prefer not to bundle fonts, you can also use the FontAwesome web SVGs and embed them as images,
or add the FontAwesomeFX library as a Maven dependency instead.
