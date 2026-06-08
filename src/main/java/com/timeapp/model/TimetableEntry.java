package com.timeapp.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * A recurring weekly course rule — the persistence model.
 *
 * Pure POJO (no JavaFX): stored in ScheduleData.timetableEntries and
 * serialized to JSON by Jackson.
 *
 * TimetableExpander reads these rules and produces TimetableClass
 * instances (concrete occurrences) for a requested date.
 *
 * Relationship to TimetableClassRecord:
 *   TimetableClassRecord = UI model (AccentColor, ObservableList slots, used
 *                          in the Timetable panels)
 *   TimetableEntry       = persistence model (plain Java, stored in JSON)
 *   TimetableClassRecord.toTimetableEntries() converts UI → persistence.
 */
public class TimetableEntry {

    private String    title;
    private DayOfWeek dayOfWeek;
    private LocalTime startTime;
    private LocalTime endTime;
    private String    room;
    private String    teacher;
    /**
     * Stored as the AccentColor enum name (e.g. "PURPLE") so the UI layer
     * can reconstruct the AccentColor via AccentColor.valueOf(entry.getColor()).
     */
    private String    color;
    private String    tag;
    private LocalDate semesterStart;
    private LocalDate semesterEnd;

    /** No-arg constructor for Jackson. */
    public TimetableEntry() {}

    @JsonCreator
    public TimetableEntry(
            @JsonProperty("title")         String    title,
            @JsonProperty("dayOfWeek")     DayOfWeek dayOfWeek,
            @JsonProperty("startTime")     LocalTime startTime,
            @JsonProperty("endTime")       LocalTime endTime,
            @JsonProperty("room")          String    room,
            @JsonProperty("teacher")       String    teacher,
            @JsonProperty("color")         String    color,
            @JsonProperty("tag")           String    tag,
            @JsonProperty("semesterStart") LocalDate semesterStart,
            @JsonProperty("semesterEnd")   LocalDate semesterEnd) {
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

    public String    getTitle()         { return title; }
    public DayOfWeek getDayOfWeek()     { return dayOfWeek; }
    public LocalTime getStartTime()     { return startTime; }
    public LocalTime getEndTime()       { return endTime; }
    public String    getRoom()          { return room; }
    public String    getTeacher()       { return teacher; }
    public String    getColor()         { return color; }
    public String    getTag()           { return tag; }
    public LocalDate getSemesterStart() { return semesterStart; }
    public LocalDate getSemesterEnd()   { return semesterEnd; }

    public void setTitle(String v)          { this.title         = v; }
    public void setDayOfWeek(DayOfWeek v)   { this.dayOfWeek     = v; }
    public void setStartTime(LocalTime v)   { this.startTime     = v; }
    public void setEndTime(LocalTime v)     { this.endTime       = v; }
    public void setRoom(String v)           { this.room          = v; }
    public void setTeacher(String v)        { this.teacher       = v; }
    public void setColor(String v)          { this.color         = v; }
    public void setTag(String v)            { this.tag           = v; }
    public void setSemesterStart(LocalDate v){ this.semesterStart = v; }
    public void setSemesterEnd(LocalDate v) { this.semesterEnd   = v; }

    /** True when date falls within [semesterStart, semesterEnd] (inclusive). */
    public boolean isDateInRange(LocalDate date) {
        if (date == null) return false;
        boolean afterStart = semesterStart == null || !date.isBefore(semesterStart);
        boolean beforeEnd  = semesterEnd   == null || !date.isAfter(semesterEnd);
        return afterStart && beforeEnd;
    }
}
