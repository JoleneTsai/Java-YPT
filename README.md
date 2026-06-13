# TimeFlow - JavaFX Time Management App

TimeFlow is a Java 17 / JavaFX schedule app for managing a daily timeline,
semester timetables, calendar events, and to-do items. The UI is designed as a
mobile-style prototype with a fixed 390 x 844 viewport.

## Current Version

This branch represents the current version-11 codebase.

Main updates included in the current version:

- Domain model and UI model are separated.
- Timetable data is saved as full timetable objects, including title, semester
  start date, semester end date, and classes.
- Legacy `timetableEntries` data can still be read.
- TimeTable create, edit, delete, class add, class edit, and class delete flows
  are connected to persistence.
- Calendar events and to-do tasks are saved through the service layer.
- Runtime data is stored in `schedule-data.json` at the project root.

## Requirements

- Java 17
- Maven 3.8 or newer
- JavaFX dependencies are downloaded by Maven from `pom.xml`

## How To Run

From PowerShell, run the app from the project root:

```powershell
cd D:\資料\作業\ypt_todo_no_deadline
& 'C:\apache-maven\bin\mvn.cmd' clean compile
& 'C:\apache-maven\bin\mvn.cmd' javafx:run
```

If Maven is already available in your `PATH`, you can use:

```powershell
cd D:\資料\作業\ypt_todo_no_deadline
mvn clean compile
mvn javafx:run
```

Fallback command:

```powershell
& 'C:\apache-maven\bin\mvn.cmd' compile exec:java
```

When the app starts, the console prints the data file path, for example:

```text
[TimeFlow] Data file: D:\資料\作業\ypt_todo_no_deadline\schedule-data.json
```

## Project Structure

```text
ypt_todo_no_deadline/
|-- pom.xml
|-- README.md
|-- schedule-data.json
|-- src/
|   |-- main/
|   |   |-- java/
|   |   |   |-- module-info.java
|   |   |   `-- com/timeapp/
|   |   |       |-- MainApp.java
|   |   |       |-- controller/
|   |   |       |-- domain/
|   |   |       |   |-- model/
|   |   |       |   |-- repository/
|   |   |       |   `-- service/
|   |   |       |-- ui/
|   |   |       |   `-- model/
|   |   |       `-- view/
|   |   |           `-- timetable/
|   |   `-- resources/
|   |       `-- com/timeapp/
|   |           |-- css/
|   |           `-- fonts/
|   `-- com/
`-- target/
```

Main source code lives under `src/main/java`. The `target/` folder is generated
by Maven and should not be edited or committed.

## Main Packages

| Package | Purpose |
|---|---|
| `com.timeapp` | JavaFX application entry point |
| `com.timeapp.controller` | Connects UI actions, state, service calls, and persistence |
| `com.timeapp.domain.model` | Plain Java domain objects used by service and repository layers |
| `com.timeapp.domain.repository` | Data storage abstraction and JSON implementation |
| `com.timeapp.domain.service` | Business logic for timetable expansion, timeline building, and schedule operations |
| `com.timeapp.ui.model` | JavaFX-friendly UI state models with observable properties |
| `com.timeapp.view` | Main JavaFX views and reusable UI components |
| `com.timeapp.view.timetable` | TimeTable-specific screens |

## Domain Model

| File | Purpose |
|---|---|
| `Schedulable.java` | Common interface for timeline-compatible events |
| `CalendarEvent.java` | Saved calendar event with real start and end date-time |
| `TimetableEntry.java` | Weekly recurring class rule |
| `ExpandedTimetableEvent.java` | Runtime expanded concrete class event |
| `ToDoTask.java` | Saved to-do item; it is not part of the schedulable timeline model |
| `TimetableData.java` | Saved full timetable with id, title, semester dates, and classes |
| `ScheduleData.java` | Root object for all saved data |
| `EventType.java` | Event type enum |

## Service Layer

| File | Purpose |
|---|---|
| `TimetableExpander.java` | Expands weekly `TimetableEntry` rules into dated `ExpandedTimetableEvent` objects |
| `TimelineBuilder.java` | Builds sorted timelines from calendar events and expanded timetable events |
| `ScheduleService.java` | Main business facade used by the controller |

Timeline rules:

- Timeline only mixes `CalendarEvent` and `ExpandedTimetableEvent`.
- To-do tasks are stored separately and are not `Schedulable`.
- Events are sorted by start time.
- Two events conflict when their time ranges overlap.
- Cross-day events are shown on their start date only.

## Repository Layer

| File | Purpose |
|---|---|
| `ScheduleRepository.java` | Repository interface |
| `JsonScheduleRepository.java` | JSON file read/write implementation |

The repository currently uses a simple local JSON file and does not use a
database.

## Runtime Data

The app writes user data to:

```text
schedule-data.json
```

This file is created or updated at runtime. It is not Java source code. Keeping
it in the project root makes it easy to inspect saved data during development.

Current JSON structure:

```json
{
  "calendarEvents": [],
  "todoTasks": [],
  "timetableEntries": [],
  "timetables": [
    {
      "id": "example-id",
      "title": "114-1 Semester",
      "startDate": "2026-09-01",
      "endDate": "2027-01-15",
      "classes": [
        {
          "title": "Linear Algebra",
          "dayOfWeek": "MONDAY",
          "startTime": "09:00",
          "endTime": "10:30",
          "room": "A101",
          "teacher": "Prof. Lee",
          "color": "#9B72FF",
          "tag": "class",
          "semesterStart": "2026-09-01",
          "semesterEnd": "2027-01-15"
        }
      ]
    }
  ]
}
```

## Current Feature Status

Implemented:

- Timeline display with sample/demo data retained.
- Add calendar event.
- Add to-do task.
- Create TimeTable with semester start and end dates.
- Edit and delete TimeTable.
- Add, edit, and delete classes.
- Save and load data through `ScheduleService`.
- Read old `timetableEntries` format and convert it into the newer timetable
  structure.

Known limitation:

- The sidebar Calendar page is still a placeholder; selecting Calendar currently
  stays on the timeline view.

## Build Check

Compile the project with:

```powershell
& 'C:\apache-maven\bin\mvn.cmd' clean compile
```

Expected result:

```text
BUILD SUCCESS
```
