package com.timeapp.ui.model;

import java.time.LocalTime;

/**
 * A calendar event displayed as a blue card on the timeline.
 */
public class CalendarEvent extends TimeEntry {

    private final String location;
    private final com.timeapp.domain.model.CalendarEvent sourceEvent;

    public CalendarEvent(LocalTime start, LocalTime end, String title, String location) {
        this(start, end, title, location, null);
    }

    public CalendarEvent(LocalTime start, LocalTime end, String title, String location,
                         com.timeapp.domain.model.CalendarEvent sourceEvent) {
        super(start, end, title);
        this.location = location;
        this.sourceEvent = sourceEvent;
    }

    public String getLocation() { return location; }
    public com.timeapp.domain.model.CalendarEvent getSourceEvent() { return sourceEvent; }

    @Override public Type getType() { return Type.CALENDAR_EVENT; }
}
