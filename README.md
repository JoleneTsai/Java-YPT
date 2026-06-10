# TimeFlow — Time Management App

A production-grade **JavaFX** mobile-style dashboard implementing a full-featured
day-planner, course timetable, and calendar timeline.
Targets a **390 × 844 px** viewport (iPhone 14 Pro equivalent) for desktop
prototyping of a mobile UI.

---

## Project Structure

```
TimeManagementApp/
├── pom.xml                                         Maven build file
└── src/main/
    ├── java/
    │   ├── module-info.java                        JPMS module descriptor
    │   └── com/timeapp/
    │       ├── MainApp.java                        Entry point + FA font loader
    │       │
    │       ├── model/                              Pure data — no JavaFX UI imports
    │       │   ├── TimeEntry.java                  Abstract base (start/end/title)
    │       │   ├── TodoEntry.java                  Checkbox item + BooleanProperty
    │       │   ├── CalendarEvent.java              Single-day event (location)
    │       │   ├── TimetableClass.java             Daily timeline card (location + professor)
    │       │   ├── TimeTable.java                  Semester container (title, date range, classes)
    │       │   └── TimetableClassRecord.java       Course record with AccentColor + ClassTimeSlot
    │       │
    │       ├── controller/
    │       │   └── DashboardController.java        All state properties + action methods
    │       │
    │       ├── domain/                             Pure Java domain + persistence layer
    │       │   ├── model/
    │       │   │   ├── ScheduleData.java           Root JSON persistence object
    │       │   │   ├── TimetableData.java          Persisted timetable container
    │       │   │   ├── TimetableEntry.java         Persisted recurring class rule
    │       │   │   ├── CalendarEvent.java          Persisted full-date calendar event
    │       │   │   ├── ToDoTask.java               Persisted to-do task
    │       │   │   ├── Schedulable.java            Timeline-compatible event interface
    │       │   │   ├── EventType.java              CALENDAR / TIMETABLE
    │       │   │   └── ExpandedTimetableEvent.java Concrete dated class occurrence
    │       │   ├── repository/
    │       │   │   ├── ScheduleRepository.java     Storage abstraction
    │       │   │   └── JsonScheduleRepository.java JSON load/save implementation
    │       │   └── service/
    │       │       ├── ScheduleService.java         Data facade called by controller
    │       │       ├── TimetableExpander.java       Recurring classes -> dated events
    │       │       └── TimelineBuilder.java         Merge + sort timeline entries
    │       │
    │       └── view/
    │           ├── DashboardView.java              Root AnchorPane — layer stack + panel switching
    │           ├── IconLabel.java                  FontAwesome glyph factory
    │           ├── WeekStripBar.java               7-day selector + week navigation arrows
    │           ├── TimelinePane.java               Scrollable hour grid + entry cards
    │           ├── SidebarDrawer.java              Sliding navigation drawer (260 px)
    │           ├── FabButton.java                  FAB + animated speed-dial
    │           └── timetable/
    │               ├── TimetableMainPane.java      Panel 1 — week grid hub + sub-panel router
    │               ├── TimetableAddClassPane.java  Panel 2 — add class form
    │               ├── TimetableListPane.java      Panel 3 — semester list & selector
    │               └── TimetableCreatePane.java    Panel 4 — create new semester form
    │
    └── resources/                                  ← Separated from Java source (this project)
        └── com/timeapp/
            ├── css/
            │   └── app.css                         Full design system (858 lines)
            └── fonts/
                └── fa-solid-900.ttf                FontAwesome 6 Free Solid
```

> **Why resources are separate from Java source**
> `src/main/resources/` is Maven's standard resource directory. Files placed here
> are copied verbatim to the classpath root at build time, keeping UI assets
> (fonts, stylesheets) cleanly separated from compiled `.class` files.
> Java code accesses them at runtime via `getClass().getResource("/com/timeapp/...")`.

---

## Running the App

**Requirements:** Java 17+, Maven 3.8+

```bash
cd TimeManagementApp
mvn javafx:run
```

**Fallback** (if `mvn javafx:run` fails — e.g. older Maven without `org.openjfx`
in plugin groups):

```bash
mvn compile exec:java
```

---

## Architecture: Layer Stack

`DashboardView` composes the entire screen as a single `AnchorPane` with four
z-ordered layers:

```
AnchorPane  root  (390 × 844)
│
├─ [z=0]  StackPane  contentStack          ← swaps between the two top-level sections
│          ├─ BorderPane  timelineSection  ← default view
│          │    ├─ TOP:     VBox
│          │    │            ├─ TopBar (HBox) — [☰] · date · [↻]
│          │    │            └─ WeekStrip (HBox) — ‹ SUN MON … SAT ›
│          │    └─ CENTER:  TimelinePane (ScrollPane → AnchorPane canvas)
│          │                    ├─ Hour labels + divider lines
│          │                    └─ TimeEntry cards (positioned by timeToY() math)
│          │
│          └─ AnchorPane  TimetableMainPane   ← shown when navigating to "TimeTable"
│               ├─ BorderPane  mainContent    ← always visible
│               │    ├─ TOP:    VBox (top-bar + SUN/MON…SAT week header)
│               │    └─ CENTER: ScrollPane (week schedule grid with class cards)
│               ├─ StackPane  subStack        ← sub-panel overlay layer
│               │    ├─ TimetableAddClassPane  (Panel 2, slides in from right)
│               │    ├─ TimetableListPane      (Panel 3, slides in from right)
│               │    └─ TimetableCreatePane    (Panel 4, slides in from right)
│               └─ VBox  timetableFab          ← own speed-dial FAB
│
├─ [z=1]  Rectangle  dimOverlay              ← 35 % black, shown when drawer is open
│
├─ [z=2]  VBox  SidebarDrawer                ← slides in/out: translateX −260 ↔ 0
│          ├─ Avatar + "TimeFlow" header
│          ├─ Nav items: TimeLine · TimeTable · Calendar
│          └─ Bottom bar: ⚙ · ℹ · version
│
└─ [z=3]  VBox  FabButton                    ← anchored bottom-right (Timeline only)
           ├─ [mini] "Add Schedule"
           ├─ [mini] "Add To-do"
           └─ [main] "+" circular FAB
```

---

## Architecture: Controller State Flow

`DashboardController` is the single source of truth.
Views observe its `Property<>` fields and never mutate shared state directly.

```
User action          Controller method          Property changes         View reaction
───────────────────  ─────────────────────────  ──────────────────────  ──────────────────────────────────
Tap ☰                toggleSidebar()            sidebarOpen = true      SidebarDrawer slides in (280 ms)
Tap dim overlay      closeSidebar()             sidebarOpen = false     SidebarDrawer slides out
Tap day cell         selectDay(d)               selectedDay = d         WeekStrip highlights · Timeline reloads
Tap ↻                backToToday()              selectedDay = today     + weekStart resets to this week
Tap FAB +            toggleFab()                fabExpanded = true      Mini-FABs animate in
Tap "TimeTable" nav  navigateTo("TimeTable")    activePanel = TIMETABLE cross-fade to TimetableMainPane
Tap "TimeLine" nav   navigateTo("TimeLine")     activePanel = TIMELINE  cross-fade back to TimelinePane
Tap "Add Class" FAB  showAddClassPane()         activeTimetablePane=ADD Panel 2 slides in from right
Tap back arrow (P2)  backToTimetableMain()      activeTimetablePane=MAIN Panel 2 slides out
Tap save (P2)        saveClass(record)          classes list grows      Timeline re-renders + JSON saves
Tap "TimeTable" FAB  showTimetableList()        activeTimetablePane=LIST Panel 3 slides in
Tap row (P3)         setActiveTimetable(tt)     activeTimetable = tt    Panel 3 circle indicators rebuild
Tap mini-FAB (P3)    showCreateTimetable()      activeTimetablePane=CREATE Panel 4 slides in
Tap save (P4)        saveNewTimetable(…)        timetableList grows     Panel 3 rebuilds · JSON saves
```

---

## Architecture: Timetable Panel Navigation

The four panels form a linear navigation stack managed by
`ctrl.activeTimetablePaneProperty()` (`"MAIN"` → `"ADD"` / `"LIST"` → `"CREATE"`).

```
Panel 1 — TimetableMainPane   (always-visible base)
│   FAB → "Add Class"    ──────────────────────────► Panel 2 — TimetableAddClassPane
│                                                        [✓ Save]  → ctrl.saveClass()  → service.saveData()
│                                                        [‹ Back]  → back to Panel 1
│
│   FAB → "TimeTable"    ──────────────────────────► Panel 3 — TimetableListPane
│                                                        Tap row   → ctrl.setActiveTimetable()
│                                                        [‹ Back]  → back to Panel 1
│                                                        Mini-FAB  ────────────────────► Panel 4 — TimetableCreatePane
│                                                                                            [✓ Save] → ctrl.saveNewTimetable() → service.saveData()
│                                                                                            [‹ Back] → back to Panel 3
```

All transitions use a horizontal `translateX` slide animation (280 ms, `EASE_BOTH`).
Panels are `visible=false` + `managed=false` when off-screen so they receive no mouse events.

---

## Architecture: Timeline Filtering (Semester Date Gate)

`DashboardController.addActiveTimetableClasses()` enforces that recurring class
entries from the active timetable only appear in the Timeline when:

1. A `ClassTimeSlot.dayOfWeek` matches the queried date's day-of-week.
2. The queried date falls within `[activeTimetable.startDate, activeTimetable.endDate]`.

```java
// Inside DashboardController
private void addActiveTimetableClasses(List<TimeEntry> list, LocalDate date) {
    TimeTable tt = activeTimetable.get();
    if (tt == null || !tt.isDateInRange(date)) return;   // ← semester gate
    for (TimetableClassRecord rec : tt.getClasses()) {
        for (ClassTimeSlot slot : rec.getTimeSlots()) {
            if (slot.getDayOfWeek() == date.getDayOfWeek()) {
                list.add(new TimetableClass(...));
            }
        }
    }
}
```

This means that once a semester's end date passes, its classes silently stop
appearing in the daily timeline with no additional code required.

---

## Design System (app.css)

### Colour Palette

| Token           | Value       | Usage                                    |
|-----------------|-------------|------------------------------------------|
| Background      | `#FFFFFF`   | App root, timeline canvas, cards         |
| Surface         | `#FAFAFA`   | Week strip, sidebar background           |
| Surface-high    | `#F2F2F7`   | Hover states, combo boxes                |
| Border          | `#E5E5EA`   | Dividers, card outlines, separators      |
| Text Primary    | `#111111`   | Headings, card titles, active labels     |
| Text Secondary  | `#8E8E93`   | Hour labels, day names, muted text       |
| Text Tertiary   | `#C7C7CC`   | Placeholders, empty-state hints          |
| Accent Blue     | `#007AFF`   | Active day, active nav, save button      |
| Accent Red      | `#FF3B30`   | To-do checkbox border, today circle      |
| Accent Purple   | `#5856D6`   | Timetable class cards (strip)            |
| Accent Teal     | `#30D5C8`   | Timetable card option                    |
| Accent Green    | `#34C759`   | Timetable card option                    |
| Accent Orange   | `#FF9F0A`   | Timetable card option                    |

### Icon Font System

All icons use **FontAwesome 6 Free Solid** (`fa-solid-900.ttf`).
The font is registered in `MainApp.java` via `Font.loadFont(getResourceAsStream(...), 10)`
**before** the Scene is created, ensuring it is available when CSS is parsed.

Icons are created exclusively through `IconLabel.of(codepoint, sizePx, styleClass)`,
which applies the font via **inline style** (`node.setStyle("-fx-font-family: '...'")`).
This is necessary because CSS cascade (priority 2) overrides `Font.font()` bean
properties (priority 4), but inline style (priority 1) always wins.

```java
// Example: create a 18px hamburger icon with colour controlled by CSS
Label icon = IconLabel.of(IconLabel.BARS, 18, "topbar-icon");
button.setGraphic(icon);
// Colour is set in app.css: .topbar-icon { -fx-text-fill: #333333; }
```

### Key Icon Codepoints (all verified in fa-solid-900.ttf)

| Constant          | Unicode    | Icon         | Used in                         |
|-------------------|------------|--------------|----------------------------------|
| `BARS`            | `\uf0c9`  | ☰ hamburger  | Top-bar menu button              |
| `CLOCK`           | `\uf017`  | 🕐 clock     | TimeLine sidebar nav item        |
| `TABLE`           | `\uf0ce`  | 🗃 table     | TimeTable sidebar nav item       |
| `CALENDAR`        | `\uf133`  | 📅 calendar  | Calendar sidebar nav item        |
| `COG`             | `\uf013`  | ⚙ gear       | Sidebar bottom bar               |
| `INFO_CIRCLE`     | `\uf129`  | ℹ info       | Sidebar bottom bar               |
| `PLUS`            | `\uf067`  | + plus       | FAB main, "Add Class" mini       |
| `ROTATE_RIGHT`    | `\uf01e`  | ↻ rotate     | "Back to Today" top-bar button   |
| `CALENDAR_CHECK`  | `\uf274`  | ✓ cal-check  | "Add Schedule" FAB mini          |
| `CIRCLE_CHECK`    | `\uf058`  | ✅ check      | "Add To-do" FAB mini             |
| `XMARK`           | `\uf00d`  | ✕ close      | FAB main when speed-dial is open |
| `CHEVRON_LEFT`    | `\uf053`  | ‹ back       | Panel 2/3/4 header back button   |
| `CHECK`           | `\uf00c`  | ✓ check      | Panel 2/4 header save button     |

---

## Model Reference

### TimeEntry hierarchy (Timeline items)

```
TimeEntry  (abstract — start, end, title)
├── TodoEntry           BooleanProperty completed
├── CalendarEvent       String location
└── TimetableClass      String location, professor
```

### TimetableClassRecord (Timetable domain)

```
TimetableClassRecord
├── String subject, teacher, classroom
├── AccentColor  (PURPLE | BLUE | TEAL | GREEN | ORANGE | RED | PINK)
│                  each has .strip (hex) + .bg (hex) for card colouring
└── ObservableList<ClassTimeSlot>
         └── ClassTimeSlot  (DayOfWeek day, LocalTime start, LocalTime end)
```

### TimeTable (Semester container)

```
TimeTable
├── String id  (UUID)
├── StringProperty title
├── ObjectProperty<LocalDate> startDate
├── ObjectProperty<LocalDate> endDate
├── boolean isDateInRange(LocalDate)   ← used by Timeline filtering
└── ObservableList<TimetableClassRecord> classes
```

### Persistence model (domain)

The UI model and persistence model are intentionally separate:

| Layer | Class | Purpose |
|---|---|---|
| UI | `com.timeapp.model.TimeTable` | JavaFX observable semester object used by views |
| UI | `com.timeapp.model.TimetableClassRecord` | JavaFX observable class form/list object |
| Domain | `com.timeapp.domain.model.TimetableData` | Plain Java timetable container saved to JSON |
| Domain | `com.timeapp.domain.model.TimetableEntry` | Plain Java recurring class rule saved to JSON |

`DashboardController` converts between UI objects and domain objects when
loading or saving. This keeps JavaFX `Property<>` objects out of the JSON layer.

```
ScheduleData
├── List<CalendarEvent> calendarEvents
├── List<ToDoTask> todoTasks
├── List<TimetableEntry> timetableEntries   (legacy-compatible list)
└── List<TimetableData> timetables          (full timetable persistence)

TimetableData
├── String id
├── String title
├── LocalDate startDate
├── LocalDate endDate
└── List<TimetableEntry> classes
```

---

## Persistence: JSON Storage

`DashboardController` owns a `ScheduleService` wired to:

```java
new JsonScheduleRepository("schedule-data.json")
```

Save class flow:

```
UI Save Class
→ DashboardController.saveClass(record)
→ ScheduleService.addClassToTimetable(timetableId, entry)
→ ScheduleService.saveData()
→ JsonScheduleRepository.saveAll(data)
→ schedule-data.json
```

Save timetable flow:

```
UI Save TimeTable
→ DashboardController.saveNewTimetable(title, start, end)
→ ScheduleService.addTimetable(timetableData)
→ JsonScheduleRepository.saveAll(data)
→ schedule-data.json
```

Startup flow:

```
DashboardController()
→ ScheduleService.loadData()
→ JsonScheduleRepository.loadAll()
→ ScheduleData.timetables
→ converted back to UI TimeTable / TimetableClassRecord
```

### Current JSON shape

```json
{
  "calendarEvents": [],
  "todoTasks": [],
  "timetableEntries": [],
  "timetables": [
    {
      "id": "semester-113-2",
      "title": "113-2 Semester",
      "startDate": "2026-02-17",
      "endDate": "2026-06-20",
      "classes": [
        {
          "title": "Linear Algebra",
          "dayOfWeek": "WEDNESDAY",
          "startTime": "10:00",
          "endTime": "12:00",
          "room": "A101",
          "teacher": "Prof. Lee",
          "color": "#4A9EFF",
          "tag": "課程",
          "semesterStart": "2026-02-17",
          "semesterEnd": "2026-06-20"
        }
      ]
    }
  ]
}
```

### Backward compatibility

Older JSON files may only contain `timetableEntries`. The controller can still
load those entries, group them into a saved timetable, and persist them back
through the new `timetables` structure. Timeline expansion uses
`timetables[].classes` first, and only falls back to legacy `timetableEntries`
when no full timetable data exists.

### Verified in version 8

- Domain layer compiles with `javac`.
- `JsonScheduleRepository.saveAll()` writes the new `timetables` JSON structure.
- `JsonScheduleRepository.loadAll()` reads the new structure back.
- A saved timetable preserves `title`, `startDate`, `endDate`, and `classes`.
- `git diff --check` passes.

### Still needs full app verification

This environment does not have Maven / JavaFX dependencies installed, so full
JavaFX startup was not verified here. On a machine with Maven, run:

```bash
mvn compile
mvn javafx:run
```

Manual UI checks:

- Create an empty TimeTable, restart, and confirm it still appears.
- Add a class, restart, and confirm the TimeTable title/date/classes remain.
- Confirm `schedule-data.json` updates after saving TimeTable/Class.
- Confirm legacy `timetableEntries` data still loads if present.
- Confirm existing timetable grid and timeline rendering still work.

---

## Timeline Layout Math

```
GUTTER   = 56 px    left column width (hour labels)
ROW_H    = 64 px    pixels per 60 minutes
PADDING  =  8 px    top offset before first hour label
START_H  =  7       first hour shown (07:00)

timeToY(LocalTime t) = PADDING + (t.hour + t.minute/60 − START_H) × ROW_H
minutesToPx(m)       = m / 60.0 × ROW_H
card min-height      = 44 px   (ensures short events remain readable)
```

---

## Timetable Schedule Grid Math

```
GUTTER  = 56 px    (same as Timeline — time label column)
ROW_H   = 56 px    (pixels per 60 minutes in the week grid)
COL_W   = (390 − 56) / 7 ≈ 47.7 px   (one day column)
START_H =  7       (grid starts at 07:00)

cardX = 56 + dayOfWeek.getValue() % 7 × COL_W + 2
cardY = (slot.startHour − START_H + slot.startMinute/60) × ROW_H + 8
cardH = (endHour − startHour + minuteDiff/60) × ROW_H − 4   (min 32 px)
```

---

## Extending the App

### Add a class programmatically

```java
TimetableClassRecord rec = TimetableClassRecord.of(
    "Computer Networks", "Prof. Lin", "Lab A1",
    TimetableClassRecord.AccentColor.TEAL,
    DayOfWeek.TUESDAY, LocalTime.of(10, 0), LocalTime.of(11, 30));

ctrl.saveClass(rec);
// Controller appends to activeTimetable.getClasses(),
// calls loadEntriesForDay() to refresh the Timeline,
// and navigates back to Panel 1 automatically.
```

### Create a new semester

```java
ctrl.saveNewTimetable("114-1 Semester Fall",
    LocalDate.of(2025, 9, 1),
    LocalDate.of(2026, 1, 16));
// Controller creates a TimeTable, adds it to timetableList,
// sets it as active, and navigates to Panel 3 to confirm.
```

### Add swipe navigation to WeekStrip

```java
// In WeekStripBar.java, add to the root HBox after construction:
root.setOnSwipeLeft(e  -> ctrl.nextWeek());
root.setOnSwipeRight(e -> ctrl.prevWeek());
```

### Navigate to Calendar section

```java
// In DashboardController.navigateTo(), add:
case "Calendar" -> {
    activePanel.set("CALENDAR");
    // then build and show CalendarView in DashboardView's contentStack
}
```

---

## Known Compile-Time Fixes (applied in this codebase)

| File | Issue | Fix |
|---|---|---|
| `TimetableMainPane.java` | `btnAddClass` used in lambda before its declaration | Both buttons declared **before** any lambda that references them |
| `TimetableAddClassPane.java` | `DateTimeFormatter` not found in inner class | Added `import java.time.format.DateTimeFormatter;` explicitly (`java.time.*` does not cover sub-packages) |
| `TimetableAddClassPane.java` | `node might not have been initialized` | `private HBox node = null;` — explicit null initialiser |
| `TimetableListPane.java` | `ListChangeListener` not found | Added `import javafx.collections.ListChangeListener;` |
| `IconLabel.java` | CSS cascade overrides `Font.font()` bean property | Font set via `node.setStyle(…)` (inline style, priority 1) instead of `Font.font()` alone (bean property, priority 4) |
| `pom.xml` | `No plugin found for prefix 'javafx'` | Added `<pluginManagement>` to register `org.openjfx` prefix; `exec-maven-plugin` fallback also included |
