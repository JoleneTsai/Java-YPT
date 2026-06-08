package com.timeapp.domain.service;

import com.timeapp.domain.model.CalendarEvent;
import com.timeapp.domain.model.ExpandedTimetableEvent;
import com.timeapp.domain.model.Schedulable;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Merges one-time calendar events and expanded timetable occurrences into
 * a single chronologically sorted {@code List<Schedulable>}.
 *
 * Also provides two utility queries used by the UI layer:
 *   • {@link #getEventsOnDate}  — filters the timeline to a single day
 *   • {@link #hasConflict}      — detects time overlap between two events
 */
public class TimelineBuilder {

    /**
     * Builds a unified, chronologically sorted timeline.
     *
     * Items with a null startTime or endTime are silently skipped to avoid
     * NullPointerExceptions during sorting.
     *
     * @param calendarEvents   one-time events from ScheduleService
     * @param timetableEvents  expanded occurrences from TimetableExpander
     * @return mutable, sorted list — safe to filter further
     */
    public List<Schedulable> buildTimeline(List<CalendarEvent>          calendarEvents,
                                            List<ExpandedTimetableEvent> timetableEvents) {
        List<Schedulable> timeline = new ArrayList<>();
        addSafe(timeline, calendarEvents);
        addSafe(timeline, timetableEvents);
        timeline.sort(Comparator.comparing(Schedulable::getStartTime));
        return timeline;
    }

    /**
     * Returns all events whose start date equals {@code date}.
     * Cross-midnight events are shown only on their start date.
     */
    public List<Schedulable> getEventsOnDate(List<Schedulable> timeline,
                                              LocalDate date) {
        Objects.requireNonNull(timeline, "timeline must not be null");
        Objects.requireNonNull(date,     "date must not be null");
        return timeline.stream()
            .filter(e -> e.getStartTime().toLocalDate().equals(date))
            .collect(Collectors.toList());
    }

    /**
     * Returns true when events {@code a} and {@code b} overlap.
     * Uses half-open intervals [start, end) so back-to-back events don't conflict.
     */
    public boolean hasConflict(Schedulable a, Schedulable b) {
        Objects.requireNonNull(a, "first event must not be null");
        Objects.requireNonNull(b, "second event must not be null");
        return a.getStartTime().isBefore(b.getEndTime())
            && b.getStartTime().isBefore(a.getEndTime());
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private <T extends Schedulable> void addSafe(List<Schedulable> dest,
                                                   List<T> source) {
        if (source == null) return;
        for (T item : source) {
            if (item != null
                && item.getStartTime() != null
                && item.getEndTime()   != null) {
                dest.add(item);
            }
        }
    }
}
