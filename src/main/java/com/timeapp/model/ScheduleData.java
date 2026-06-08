package com.timeapp.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Root persistence object — the entire app state in one JSON-serializable bag.
 *
 * JsonScheduleRepository reads and writes exactly this object.
 *
 * Four lists:
 *   semesters        — semester metadata (title, start/end dates)
 *   calendarEvents   — one-time events
 *   todoEntries      — to-do items with optional time anchor
 *   timetableEntries — recurring weekly course rules
 *
 * TimetableClass is NOT stored here: it is derived at runtime by
 * TimetableExpander from timetableEntries.
 */
public class ScheduleData {

    private List<SemesterInfo>   semesters        = new ArrayList<>();
    private List<CalendarEvent>  calendarEvents   = new ArrayList<>();
    private List<TodoEntry>      todoEntries      = new ArrayList<>();
    private List<TimetableEntry> timetableEntries = new ArrayList<>();

    public ScheduleData() {}

    public List<SemesterInfo>   getSemesters()        { return semesters; }
    public List<CalendarEvent>  getCalendarEvents()   { return calendarEvents; }
    public List<TodoEntry>      getTodoEntries()      { return todoEntries; }
    public List<TimetableEntry> getTimetableEntries() { return timetableEntries; }

    public void setSemesters(List<SemesterInfo> v)        { this.semesters        = v; }
    public void setCalendarEvents(List<CalendarEvent> v)  { this.calendarEvents   = v; }
    public void setTodoEntries(List<TodoEntry> v)         { this.todoEntries      = v; }
    public void setTimetableEntries(List<TimetableEntry> v){ this.timetableEntries = v; }
}
