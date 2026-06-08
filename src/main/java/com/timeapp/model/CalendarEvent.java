package com.timeapp.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;

/**
 * A one-time calendar event.
 *
 * Unified model: serves both as the JavaFX UI display object (rendered as
 * a blue card in TimelinePane) and as the JSON persistence object (stored
 * in ScheduleData.calendarEvents).
 *
 * Fields added vs previous version: description, color (hex string), tag.
 */
public class CalendarEvent extends TimeEntry {

    private String location;
    private String description;
    private String color;   // hex string, e.g. "#4A9EFF"
    private String tag;

    /** No-arg constructor for Jackson. */
    public CalendarEvent() {}

    /** Full constructor used by the service layer and form saves. */
    @JsonCreator
    public CalendarEvent(
            @JsonProperty("startTime")   LocalDateTime startTime,
            @JsonProperty("endTime")     LocalDateTime endTime,
            @JsonProperty("title")       String title,
            @JsonProperty("location")    String location,
            @JsonProperty("description") String description,
            @JsonProperty("color")       String color,
            @JsonProperty("tag")         String tag) {
        super(startTime, endTime, title);
        this.location    = location;
        this.description = description;
        this.color       = color;
        this.tag         = tag;
    }

    /** Convenience constructor: only the fields the UI currently fills in. */
    public CalendarEvent(LocalDateTime start, LocalDateTime end,
                         String title, String location) {
        this(start, end, title, location, null, null, null);
    }

    @Override public EventType getType() { return EventType.CALENDAR_EVENT; }

    public String getLocation()    { return location; }
    public String getDescription() { return description; }
    public String getColor()       { return color; }
    public String getTag()         { return tag; }

    public void setLocation(String v)    { this.location    = v; }
    public void setDescription(String v) { this.description = v; }
    public void setColor(String v)       { this.color       = v; }
    public void setTag(String v)         { this.tag         = v; }
}
