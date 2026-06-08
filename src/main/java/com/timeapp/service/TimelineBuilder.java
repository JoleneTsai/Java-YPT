package com.timeapp.service;

import com.timeapp.model.TimeEntry;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Merges multiple TimeEntry lists and sorts them chronologically.
 *
 * All inputs are TimeEntry subclasses (CalendarEvent, TodoEntry,
 * TimetableClass), so no cross-type conversion is needed.
 * The result is a single List<TimeEntry> ready for TimelinePane to render.
 */
public class TimelineBuilder {

    /**
     * Merge any number of TimeEntry lists into one sorted result.
     *
     * @param sources varargs of lists (nulls and null items are skipped safely)
     */
    @SafeVarargs
    public final List<TimeEntry> buildTimeline(List<? extends TimeEntry>... sources) {
        List<TimeEntry> result = new ArrayList<>();
        for (List<? extends TimeEntry> source : sources) {
            if (source == null) continue;
            for (TimeEntry e : source) {
                if (e != null && e.getStartTime() != null) result.add(e);
            }
        }
        result.sort(Comparator.comparing(TimeEntry::getStartTime));
        return result;
    }

    /**
     * Filter a sorted timeline to entries whose start date equals {@code date}.
     */
    public List<TimeEntry> getEntriesOnDate(List<TimeEntry> timeline,
                                             LocalDate date) {
        if (timeline == null || date == null) return Collections.emptyList();
        return timeline.stream()
            .filter(e -> e.getStartTime().toLocalDate().equals(date))
            .collect(Collectors.toList());
    }

    /**
     * Returns true if events a and b overlap (half-open intervals).
     * Back-to-back events (end of a == start of b) are NOT considered a conflict.
     */
    public boolean hasConflict(TimeEntry a, TimeEntry b) {
        if (a == null || b == null) return false;
        if (a.getStartTime() == null || a.getEndTime() == null) return false;
        if (b.getStartTime() == null || b.getEndTime() == null) return false;
        return a.getStartTime().isBefore(b.getEndTime())
            && b.getStartTime().isBefore(a.getEndTime());
    }
}
