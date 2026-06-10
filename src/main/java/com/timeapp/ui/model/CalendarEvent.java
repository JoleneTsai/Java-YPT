package com.timeapp.ui.model;

import java.time.LocalTime;

/**
 * A calendar event displayed as a blue card on the timeline.
 */
public class CalendarEvent extends TimeEntry {

    private final String location;

    public CalendarEvent(LocalTime start, LocalTime end, String title, String location) {
        super(start, end, title);
        this.location = location;
    }

    public String getLocation() { return location; }

    @Override public Type getType() { return Type.CALENDAR_EVENT; }
}
