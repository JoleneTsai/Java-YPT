package com.timeapp.domain.model;

/**
 * Discriminates between a one-time calendar event and a recurring
 * timetable occurrence in the unified Schedulable timeline.
 */
public enum EventType {
    CALENDAR,
    TIMETABLE
}
