package com.timeapp.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;

/**
 * A concrete single occurrence of a recurring course on the Timeline.
 *
 * ── Role in the unified architecture ─────────────────────────────────────────
 * This class replaces the old LocalTime-based TimetableClass AND the former
 * ExpandedTimetableEvent from the domain layer.
 *
 * TimetableExpander.expandForDay() creates instances of this class from
 * TimetableEntry (the recurring rule). They are never persisted directly —
 * only TimetableEntry objects are stored in ScheduleData.  TimetableClass
 * is always derived at runtime.
 *
 * Fields added vs previous version: color (hex string), tag.
 * Fields renamed: location → room, professor → teacher (matching TimetableEntry).
 * ─────────────────────────────────────────────────────────────────────────────
 */
public class TimetableClass extends TimeEntry {

    private String room;
    private String teacher;
    private String color;   // hex from TimetableEntry.color
    private String tag;

    /** No-arg constructor for Jackson (defensive; normally not persisted). */
    public TimetableClass() {}

    @JsonCreator
    public TimetableClass(
            @JsonProperty("startTime") LocalDateTime startTime,
            @JsonProperty("endTime")   LocalDateTime endTime,
            @JsonProperty("title")     String title,
            @JsonProperty("room")      String room,
            @JsonProperty("teacher")   String teacher,
            @JsonProperty("color")     String color,
            @JsonProperty("tag")       String tag) {
        super(startTime, endTime, title);
        this.room    = room;
        this.teacher = teacher;
        this.color   = color;
        this.tag     = tag;
    }

    /** Convenience constructor without color/tag (backward compatible). */
    public TimetableClass(LocalDateTime start, LocalDateTime end,
                          String title, String room, String teacher) {
        this(start, end, title, room, teacher, null, null);
    }

    @Override public EventType getType() { return EventType.TIMETABLE_CLASS; }

    public String getRoom()    { return room; }
    public String getTeacher() { return teacher; }
    public String getColor()   { return color; }
    public String getTag()     { return tag; }

    public void setRoom(String v)    { this.room    = v; }
    public void setTeacher(String v) { this.teacher = v; }
    public void setColor(String v)   { this.color   = v; }
    public void setTag(String v)     { this.tag     = v; }

    // ── Backward-compat aliases for TimelinePane rendering ────────────────────
    /** @deprecated Use getRoom() */
    public String getLocation()  { return room; }
    /** @deprecated Use getTeacher() */
    public String getProfessor() { return teacher; }
}
