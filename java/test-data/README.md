# YPT App Data Layer Test Data

These JSON files are for testing `JsonScheduleRepository.loadAll()` and `saveAll()`.

Use them by passing the file path into:

```java
ScheduleRepository repo = new JsonScheduleRepository("java/test-data/basic-schedule.json");
ScheduleData data = repo.loadAll();
```

## Files

- `basic-schedule.json`
  - Normal mixed data.
  - Expected counts: calendarEvents = 2, todoTasks = 3, timetableEntries = 2.

- `empty-schedule.json`
  - Empty lists.
  - Expected counts: calendarEvents = 0, todoTasks = 0, timetableEntries = 0.

- `edge-case-schedule.json`
  - Includes a cross-day event and a same-time conflict.
  - Expected counts: calendarEvents = 3, todoTasks = 2, timetableEntries = 1.
  - Conflict check: "期中考複習" overlaps with "線性代數".
  - Cross-day check: "通宵讀書" starts on 2026-05-22 and ends on 2026-05-23.

- `timetable-expansion-schedule.json`
  - Focused on weekly timetable data.
  - Expected counts: calendarEvents = 1, todoTasks = 1, timetableEntries = 3.
  - Useful for checking whether timetable entries expand correctly within semester dates.
