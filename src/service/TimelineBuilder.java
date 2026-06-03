package service;

import model.CalendarEvent;
import model.ExpandedTimetableEvent;
import model.Schedulable;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class TimelineBuilder {
    /**
     * Builds a unified timeline from one-time calendar events and expanded timetable events.
     */
    public List<Schedulable> buildTimeline(
            List<CalendarEvent> calendarEvents,
            List<ExpandedTimetableEvent> timetableEvents
    ) {
        List<Schedulable> timeline = new ArrayList<>();
        addCalendarEvents(timeline, calendarEvents);
        addTimetableEvents(timeline, timetableEvents);
        timeline.sort(Comparator.comparing(Schedulable::getStartTime));
        return timeline;
    }

    /**
     * Cross-day events are shown only on the date where they start.
     */
    public List<Schedulable> getEventsOnDate(List<Schedulable> timeline, LocalDate date) {
        Objects.requireNonNull(timeline, "timeline must not be null");
        Objects.requireNonNull(date, "date must not be null");

        return timeline.stream()
                .filter(event -> event.getStartTime().toLocalDate().equals(date))
                .collect(Collectors.toList());
    }

    /**
     * Detects overlap using half-open time ranges: [start, end).
     */
    public boolean hasConflict(Schedulable a, Schedulable b) {
        Objects.requireNonNull(a, "first event must not be null");
        Objects.requireNonNull(b, "second event must not be null");

        return a.getStartTime().isBefore(b.getEndTime())
                && b.getStartTime().isBefore(a.getEndTime());
    }

    private void addCalendarEvents(List<Schedulable> timeline, List<CalendarEvent> calendarEvents) {
        if (calendarEvents == null) {
            return;
        }
        for (CalendarEvent event : calendarEvents) {
            addIfReady(timeline, event);
        }
    }

    private void addTimetableEvents(List<Schedulable> timeline, List<ExpandedTimetableEvent> timetableEvents) {
        if (timetableEvents == null) {
            return;
        }
        for (ExpandedTimetableEvent event : timetableEvents) {
            addIfReady(timeline, event);
        }
    }

    private void addIfReady(List<Schedulable> timeline, Schedulable event) {
        if (event != null && event.getStartTime() != null && event.getEndTime() != null) {
            timeline.add(event);
        }
    }
}
