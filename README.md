# Java-YPT Study Planner

Java-YPT is a JavaFX study planning app for managing daily schedules, to-do tasks, calendars, and course timetables. The app uses a mobile-style dashboard layout and stores user data locally through a JSON-based persistence layer.

---

## Project Structure

```text
Java-YPT/
|-- pom.xml                                      Maven build file
|-- README.md
`-- src/main/
    |-- java/
    |   |-- module-info.java                     JPMS module descriptor
    |   `-- com/timeapp/
    |       |-- MainApp.java                     JavaFX entry point and font loader
    |       |
    |       |-- controller/
    |       |   `-- DashboardController.java     Main state controller and action handler
    |       |
    |       |-- domain/                          Plain Java domain and persistence layer
    |       |   |-- model/
    |       |   |   |-- ScheduleData.java        Root object for persisted app data
    |       |   |   |-- CalendarEvent.java       Persisted schedule event
    |       |   |   |-- ToDoTask.java            Persisted to-do task
    |       |   |   |-- TimetableData.java       Persisted timetable container
    |       |   |   |-- TimetableEntry.java      Persisted course rule
    |       |   |   |-- ExpandedTimetableEvent.java
    |       |   |   |-- EventType.java
    |       |   |   `-- Schedulable.java
    |       |   |-- repository/
    |       |   |   |-- ScheduleRepository.java
    |       |   |   `-- JsonScheduleRepository.java
    |       |   `-- service/
    |       |       |-- ScheduleService.java
    |       |       |-- TimetableExpander.java
    |       |       `-- TimelineBuilder.java
    |       |
    |       |-- ui/model/                        JavaFX observable models for views
    |       |   |-- TimeEntry.java
    |       |   |-- TodoEntry.java
    |       |   |-- CalendarEvent.java
    |       |   |-- TimeTable.java
    |       |   |-- TimetableClass.java
    |       |   `-- TimetableClassRecord.java
    |       |
    |       `-- view/
    |           |-- DashboardView.java
    |           |-- TimelinePane.java
    |           |-- AddSchedulePane.java
    |           |-- AddTodoPane.java
    |           |-- SidebarDrawer.java
    |           |-- WeekStripBar.java
    |           |-- FabButton.java
    |           |-- IconLabel.java
    |           |-- calendar/
    |           |   |-- CalendarMainPane.java
    |           |   `-- CalendarDayPane.java
    |           `-- timetable/
    |               |-- TimetableMainPane.java
    |               |-- TimetableAddClassPane.java
    |               |-- TimetableListPane.java
    |               `-- TimetableCreatePane.java
    |
    `-- resources/
        `-- com/timeapp/
            |-- css/
            |   `-- app.css
            `-- fonts/
                `-- fa-solid-900.ttf
```

`src/main/resources/` follows the standard Maven resource layout. CSS and font files are copied to the classpath during the build and loaded by the JavaFX app at runtime.

---

## Running the App

Requirements:

- Java 17+
- Maven 3.8+

Run the app:

```bash
mvn javafx:run
```

Fallback command:

```bash
mvn compile exec:java
```

---

## Architecture: Layer Overview

The app is organized around a JavaFX view layer, a controller layer, a service layer, and a domain persistence layer.

```text
User interaction
      |
      v
View classes
      |
      v
DashboardController
      |
      v
ScheduleService
      |
      v
ScheduleRepository
      |
      v
JsonScheduleRepository
      |
      v
schedule-data.json
```

Responsibilities:

- `view`: Builds the JavaFX screens and handles local UI interaction.
- `controller`: Holds shared UI state and routes user actions.
- `domain/service`: Provides app-level operations such as add, update, delete, load, and save.
- `domain/repository`: Defines and implements data storage.
- `domain/model`: Contains plain Java objects used for persisted data.
- `ui/model`: Contains JavaFX observable objects used for screen binding.

---

## Architecture: Main Screens

### Timeline

`TimelinePane` displays schedule events, timetable classes, and to-do tasks for the selected day.

Key behavior:

- Events are positioned by start and end time.
- Overlapping schedule events and class blocks are laid out side by side.
- To-do tasks are displayed as compact checkbox rows above the timeline cards.
- Schedule and to-do items can be edited or deleted through context menus.

### Calendar

The calendar view contains:

- `CalendarMainPane`: Month view and date selection.
- `CalendarDayPane`: Daily event list for a selected date.

Schedule items in the daily list can be edited or deleted from the context menu.

### TimeTable

The timetable view contains:

- `TimetableMainPane`: Main weekly timetable grid.
- `TimetableAddClassPane`: Add course form.
- `TimetableListPane`: Timetable selection list.
- `TimetableCreatePane`: New timetable form.

A timetable stores its own title, date range, and class list.

---

## Architecture: Controller State Flow

`DashboardController` is the central coordinator between the UI and the app data.

```text
Action                         Controller method             Result
-----------------------------  ----------------------------  ------------------------------
Select a day                   selectDay(date)               Timeline and calendar update
Return to today                backToToday()                 Selected date resets to today
Open sidebar                   toggleSidebar()               Sidebar visibility changes
Navigate to another panel      navigateTo(panel)             Active panel changes
Add schedule                   saveSchedule(event)           Event is saved to JSON
Edit schedule                  updateSchedule(old, new)      Event is updated in JSON
Delete schedule                deleteSchedule(event)         Event is removed from JSON
Add to-do                      saveTodo(task)                Task is saved to JSON
Edit to-do                     updateTodo(old, new)          Task is updated in JSON
Delete to-do                   deleteTodo(task)              Task is removed from JSON
Create timetable               saveNewTimetable(...)         Timetable is saved to JSON
Add class                      saveClass(record)             Class is saved under timetable
```

Views observe controller properties and refresh when the underlying state changes.

---

## Model Reference

### Persistence Model

The persistence model is stored under `com.timeapp.domain.model`.

```text
ScheduleData
|-- List<CalendarEvent> calendarEvents
|-- List<ToDoTask> todoTasks
|-- List<TimetableEntry> timetableEntries
`-- List<TimetableData> timetables
```

Important classes:

- `ScheduleData`: Root data object saved to JSON.
- `CalendarEvent`: One-time schedule event with title, start time, end time, location, description, color, and tag.
- `ToDoTask`: To-do task with title, completed state, scheduled date, and scheduled time.
- `TimetableData`: Full timetable object with id, title, start date, end date, and class list.
- `TimetableEntry`: Course rule with weekday, time range, room, teacher, color, tag, and semester date range.

### UI Model

The UI model is stored under `com.timeapp.ui.model`.

These classes use JavaFX properties and observable lists so the interface can update when data changes:

- `TimeTable`
- `TimetableClassRecord`
- `TimetableClass`
- `CalendarEvent`
- `TodoEntry`
- `TimeEntry`

The controller converts between UI models and persistence models when loading or saving data.

---

## Persistence: JSON Storage

The app stores data through `JsonScheduleRepository`.

```text
DashboardController
      |
      v
ScheduleService
      |
      v
JsonScheduleRepository
      |
      v
schedule-data.json
```

Save flow:

```text
User saves data
-> DashboardController action method
-> ScheduleService add/update/delete method
-> ScheduleService.saveData()
-> JsonScheduleRepository.saveAll(data)
-> schedule-data.json
```

Load flow:

```text
App starts
-> DashboardController initializes ScheduleService
-> ScheduleService.loadData()
-> JsonScheduleRepository.loadAll()
-> domain model data is converted into UI model data
```

---

## JSON Data Shape

The root JSON object contains four sections:

```jsonc
{
  "calendarEvents": [],
  "todoTasks": [],
  "timetableEntries": [],
  "timetables": []
}
```

### Timetables

`timetables` stores complete timetable objects. Each timetable has its own identity, title, date range, and class list.

```jsonc
"timetables": [
  {
    "id": "...",
    "title": "113-2",
    "startDate": "2026-02-23",
    "endDate": "2026-06-26",
    "classes": [
      {
        "title": "Algorithm",
        "dayOfWeek": "FRIDAY",
        "startTime": "13:00",
        "endTime": "15:00",
        "room": "Room 65304",
        "teacher": "Prof. Lee",
        "color": "#4A9EFF",
        "tag": "class",
        "semesterStart": "2026-02-23",
        "semesterEnd": "2026-06-26"
      }
    ]
  }
]
```

This allows the app to save an entire timetable instead of only saving separate class entries.

---

## Timeline Layout

Timeline entries are positioned by time:

```text
start time -> vertical position
duration   -> card height
overlap    -> horizontal column placement
```

Schedule and class blocks can share the same time range without replacing each other. To-do tasks are rendered as compact rows and placed above the schedule layer by default.

---

## Git Ignore Rules

The repository ignores generated build output and local runtime data:

```text
target/
schedule-data.json
```

Reasons:

- `target/` is generated by Maven and should not be committed.
- `schedule-data.json` is local runtime data and may contain personal test records.

---

## Development Checks

Compile:

```bash
mvn -q -DskipTests compile
```

Run tests:

```bash
mvn -q test
```

Check whitespace issues in the current diff:

```bash
git diff --check
```
