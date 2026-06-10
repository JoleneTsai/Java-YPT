package com.timeapp.domain.model;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * A to-do task -- the domain / persistence model.
 *
 * v10 fix: Added scheduledDate (YYYY-MM-DD) and scheduledTime (HH:MM) as
 * plain String fields so the manual JSON builder in JsonScheduleRepository
 * can serialize them without any extra date-library dependencies.
 *
 * Backward compatible: both fields are optional (null = unscheduled).
 * Tasks saved before v10 have no scheduledDate and will appear at a
 * fallback time on selectedDay, exactly as before.
 */
public class ToDoTask {

    private String  title;
    private boolean completed;
    private String  scheduledDate;   // "YYYY-MM-DD"  or null
    private String  scheduledTime;   // "HH:MM"       or null

    public ToDoTask() {}

    public ToDoTask(String title, boolean completed) {
        this.title     = title;
        this.completed = completed;
    }

    public String  getTitle()              { return title; }
    public boolean isCompleted()           { return completed; }
    public void    setTitle(String v)      { this.title     = v; }
    public void    setCompleted(boolean v) { this.completed = v; }

    public String getScheduledDate()         { return scheduledDate; }
    public String getScheduledTime()         { return scheduledTime; }
    public void   setScheduledDate(String v) { this.scheduledDate = v; }
    public void   setScheduledTime(String v) { this.scheduledTime = v; }

    /** Convenience: store date+time from Java objects. */
    public void setScheduled(LocalDate date, LocalTime time) {
        this.scheduledDate = (date != null) ? date.toString() : null;
        this.scheduledTime = (time != null)
            ? String.format("%02d:%02d", time.getHour(), time.getMinute()) : null;
    }

    public LocalDate getScheduledLocalDate() {
        if (scheduledDate == null || scheduledDate.isBlank()) return null;
        try { return LocalDate.parse(scheduledDate); } catch (Exception e) { return null; }
    }

    public LocalTime getScheduledLocalTime() {
        if (scheduledTime == null || scheduledTime.isBlank()) return null;
        try { return LocalTime.parse(scheduledTime); } catch (Exception e) { return null; }
    }

    /** True when this task has a specific date AND time pinned to it. */
    public boolean isScheduled() {
        return getScheduledLocalDate() != null && getScheduledLocalTime() != null;
    }
}
