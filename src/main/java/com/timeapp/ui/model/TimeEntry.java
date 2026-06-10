package com.timeapp.ui.model;

import java.time.LocalTime;

/**
 * Base class for all items that appear on the timeline.
 */
public abstract class TimeEntry {
    private final LocalTime startTime;
    private final LocalTime endTime;
    private final String title;

    protected TimeEntry(LocalTime startTime, LocalTime endTime, String title) {
        this.startTime = startTime;
        this.endTime   = endTime;
        this.title     = title;
    }

    public LocalTime getStartTime() { return startTime; }
    public LocalTime getEndTime()   { return endTime; }
    public String    getTitle()     { return title; }

    /** Duration in minutes */
    public long getDurationMinutes() {
        return java.time.Duration.between(startTime, endTime).toMinutes();
    }

    public enum Type { TODO, CALENDAR_EVENT, TIMETABLE_CLASS }
    public abstract Type getType();
}
