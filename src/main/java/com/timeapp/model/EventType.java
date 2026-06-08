package com.timeapp.model;

/**
 * Discriminates between a one-time calendar event and a recurring
 * timetable occurrence in the unified Schedulable timeline.
 * TODO items are placed on the timeline visually but are not
 * Schedulable (they have no fixed end time).
 */
public enum EventType {
    TODO,
    CALENDAR_EVENT,
    TIMETABLE_CLASS
}
