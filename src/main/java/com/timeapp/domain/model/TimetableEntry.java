package com.timeapp.domain.model;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * A recurring weekly course rule — the domain / persistence model.
 *
 * Stores the RULE (which weekday, what time), not a concrete occurrence.
 * {@link com.timeapp.domain.service.TimetableExpander} converts rules
 * into concrete {@link ExpandedTimetableEvent} instances for a given
 * date range.
 *
 * Key fields not present in the UI-layer TimetableClassRecord:
 *   • semesterStart / semesterEnd  — hard date boundaries; the expander
 *     will not generate occurrences outside this window.
 *   • color  — stored as a hex String for JSON persistence; the UI layer
 *     maps this to AccentColor enum values.
 *   • tag    — arbitrary label for grouping / filtering.
 *
 * Relationship to UI layer:
 *   TimetableEntry  (domain, persisted)
 *       └── maps to / from  TimetableClassRecord  (UI, JavaFX Properties)
 */
public class TimetableEntry {

    private String    title;
    private DayOfWeek dayOfWeek;
    private LocalTime startTime;
    private LocalTime endTime;
    private String    room;
    private String    teacher;
    private String    color;
    private String    tag;
    private LocalDate semesterStart;
    private LocalDate semesterEnd;

    public TimetableEntry() {}

    public TimetableEntry(String title, DayOfWeek dayOfWeek,
                          LocalTime startTime, LocalTime endTime,
                          String room, String teacher,
                          String color, String tag,
                          LocalDate semesterStart, LocalDate semesterEnd) {
        this.title         = title;
        this.dayOfWeek     = dayOfWeek;
        this.startTime     = startTime;
        this.endTime       = endTime;
        this.room          = room;
        this.teacher       = teacher;
        this.color         = color;
        this.tag           = tag;
        this.semesterStart = semesterStart;
        this.semesterEnd   = semesterEnd;
    }

    public String    getTitle()        { return title; }
    public DayOfWeek getDayOfWeek()    { return dayOfWeek; }
    public LocalTime getStartTime()    { return startTime; }
    public LocalTime getEndTime()      { return endTime; }
    public String    getRoom()         { return room; }
    public String    getTeacher()      { return teacher; }
    public String    getColor()        { return color; }
    public String    getTag()          { return tag; }
    public LocalDate getSemesterStart(){ return semesterStart; }
    public LocalDate getSemesterEnd()  { return semesterEnd; }

    public void setTitle(String title)              { this.title         = title; }
    public void setDayOfWeek(DayOfWeek d)           { this.dayOfWeek     = d; }
    public void setStartTime(LocalTime t)           { this.startTime     = t; }
    public void setEndTime(LocalTime t)             { this.endTime       = t; }
    public void setRoom(String room)                { this.room          = room; }
    public void setTeacher(String teacher)          { this.teacher       = teacher; }
    public void setColor(String color)              { this.color         = color; }
    public void setTag(String tag)                  { this.tag           = tag; }
    public void setSemesterStart(LocalDate d)       { this.semesterStart = d; }
    public void setSemesterEnd(LocalDate d)         { this.semesterEnd   = d; }
}
