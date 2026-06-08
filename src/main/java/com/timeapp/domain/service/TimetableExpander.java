package com.timeapp.domain.service;

import com.timeapp.domain.model.ExpandedTimetableEvent;
import com.timeapp.domain.model.TimetableEntry;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Converts recurring weekly course rules into concrete dated occurrences.
 *
 * A {@link TimetableEntry} represents a rule: "every Monday, 09:00–10:30,
 * from semesterStart to semesterEnd".  For a requested date window [from, to]
 * this expander produces one {@link ExpandedTimetableEvent} per occurrence
 * that falls inside the effective range (intersection of the requested window
 * and the entry's own semester boundaries).
 *
 * Algorithm:
 *   1. Determine effectiveStart = max(from, entry.semesterStart)
 *   2. Determine effectiveEnd   = min(to,   entry.semesterEnd)
 *   3. Find the first occurrence of entry.dayOfWeek ≥ effectiveStart
 *   4. Iterate weekly (+ 7 days) until date > effectiveEnd
 */
public class TimetableExpander {

    private static final int DAYS_PER_WEEK = 7;

    /**
     * Expands all entries within the given date range.
     *
     * @param entries list of recurring course rules
     * @param from    first day of the requested window (inclusive)
     * @param to      last  day of the requested window (inclusive)
     * @return concrete dated occurrences sorted by natural insertion order
     *         (sort by startTime is done later in TimelineBuilder)
     */
    public List<ExpandedTimetableEvent> expand(List<TimetableEntry> entries,
                                                LocalDate from, LocalDate to) {
        Objects.requireNonNull(entries, "entries must not be null");
        Objects.requireNonNull(from,    "from must not be null");
        Objects.requireNonNull(to,      "to must not be null");

        List<ExpandedTimetableEvent> result = new ArrayList<>();
        if (from.isAfter(to)) return result;

        for (TimetableEntry entry : entries) {
            if (!isExpandable(entry)) continue;

            LocalDate effectiveStart = resolveEffectiveStart(entry, from);
            LocalDate effectiveEnd   = resolveEffectiveEnd(entry, to);
            if (effectiveStart.isAfter(effectiveEnd)) continue;

            LocalDate current =
                effectiveStart.with(TemporalAdjusters.nextOrSame(entry.getDayOfWeek()));

            while (!current.isAfter(effectiveEnd)) {
                result.add(createEvent(entry, current));
                current = current.plusDays(DAYS_PER_WEEK);
            }
        }
        return result;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private boolean isExpandable(TimetableEntry e) {
        return e != null
            && e.getDayOfWeek() != null
            && e.getStartTime() != null
            && e.getEndTime()   != null;
    }

    private LocalDate resolveEffectiveStart(TimetableEntry e, LocalDate requested) {
        if (e.getSemesterStart() == null || !requested.isBefore(e.getSemesterStart()))
            return requested;
        return e.getSemesterStart();
    }

    private LocalDate resolveEffectiveEnd(TimetableEntry e, LocalDate requested) {
        if (e.getSemesterEnd() == null || !requested.isAfter(e.getSemesterEnd()))
            return requested;
        return e.getSemesterEnd();
    }

    private ExpandedTimetableEvent createEvent(TimetableEntry e, LocalDate date) {
        LocalDateTime start = LocalDateTime.of(date, e.getStartTime());
        LocalDateTime end   = LocalDateTime.of(date, e.getEndTime());
        // Guard against overnight courses (rare, but handle gracefully)
        if (end.isBefore(start)) end = end.plusDays(1);
        return new ExpandedTimetableEvent(start, end, e);
    }
}
