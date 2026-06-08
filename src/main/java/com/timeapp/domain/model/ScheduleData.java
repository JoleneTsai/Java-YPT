package com.timeapp.domain.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Root persistence object — the entire app state in one serialisable bag.
 *
 * This is what {@link com.timeapp.domain.repository.ScheduleRepository}
 * reads from and writes to storage (JSON file, database, etc.).
 *
 * Three independent lists:
 *   calendarEvents   — one-time events (meetings, appointments …)
 *   todoTasks        — unscheduled to-do items
 *   timetableEntries — recurring weekly course rules
 */
public class ScheduleData {

    private List<CalendarEvent>   calendarEvents   = new ArrayList<>();
    private List<ToDoTask>        todoTasks        = new ArrayList<>();
    private List<TimetableEntry>  timetableEntries = new ArrayList<>();

    public ScheduleData() {}

    public ScheduleData(List<CalendarEvent>  calendarEvents,
                        List<ToDoTask>       todoTasks,
                        List<TimetableEntry> timetableEntries) {
        this.calendarEvents   = calendarEvents;
        this.todoTasks        = todoTasks;
        this.timetableEntries = timetableEntries;
    }

    public List<CalendarEvent>  getCalendarEvents()   { return calendarEvents; }
    public List<ToDoTask>       getTodoTasks()        { return todoTasks; }
    public List<TimetableEntry> getTimetableEntries() { return timetableEntries; }

    public void setCalendarEvents(List<CalendarEvent> v)   { this.calendarEvents   = v; }
    public void setTodoTasks(List<ToDoTask> v)             { this.todoTasks        = v; }
    public void setTimetableEntries(List<TimetableEntry> v){ this.timetableEntries = v; }
}
