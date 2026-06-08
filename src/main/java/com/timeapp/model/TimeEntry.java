package com.timeapp.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Unified base class for every item rendered on the Timeline.
 *
 * ── Key change from previous version ─────────────────────────────────────────
 * startTime / endTime are now LocalDateTime (date + time) instead of LocalTime.
 * This allows the business/service layer to work with concrete dates without
 * needing a separate "selectedDay" context variable, and makes every entry
 * self-contained for JSON persistence.
 *
 * ── Jackson polymorphism ──────────────────────────────────────────────────────
 * The @JsonTypeInfo / @JsonSubTypes annotations let Jackson serialize and
 * deserialize subclasses stored inside ScheduleData lists.
 * The "type" property is written to JSON automatically (e.g. "TODO",
 * "CALENDAR_EVENT", "TIMETABLE_CLASS") and used on read to pick the
 * correct concrete class.
 * ─────────────────────────────────────────────────────────────────────────────
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(value = TodoEntry.class,      name = "TODO"),
    @JsonSubTypes.Type(value = CalendarEvent.class,  name = "CALENDAR_EVENT"),
    @JsonSubTypes.Type(value = TimetableClass.class, name = "TIMETABLE_CLASS")
})
@JsonIgnoreProperties(ignoreUnknown = true)
public abstract class TimeEntry {

    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String        title;

    /** No-arg constructor required by Jackson for deserialization. */
    protected TimeEntry() {}

    protected TimeEntry(LocalDateTime startTime, LocalDateTime endTime, String title) {
        this.startTime = startTime;
        this.endTime   = endTime;
        this.title     = title;
    }

    public LocalDateTime getStartTime() { return startTime; }
    public LocalDateTime getEndTime()   { return endTime; }
    public String        getTitle()     { return title; }

    /** Setter required by Jackson for deserialization. */
    public void setStartTime(LocalDateTime v) { this.startTime = v; }
    public void setEndTime(LocalDateTime v)   { this.endTime   = v; }
    public void setTitle(String v)            { this.title     = v; }

    /** Duration in minutes. */
    public long getDurationMinutes() {
        if (startTime == null || endTime == null) return 0;
        return Duration.between(startTime, endTime).toMinutes();
    }

    public abstract EventType getType();
}
