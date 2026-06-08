package com.timeapp.service;

import com.timeapp.model.TimetableClass;
import com.timeapp.model.TimetableEntry;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;

/**
 * Converts TimetableEntry recurring rules into concrete TimetableClass
 * occurrences for a specific date range.
 *
 * Key change from the domain.service version:
 *   Returns List<TimetableClass> (a TimeEntry subclass) instead of
 *   List<ExpandedTimetableEvent>, so results flow directly into
 *   ScheduleService.getTimelineForDay() without any conversion step.
 */
public class TimetableExpander {

    /**
     * Expands all entries into TimetableClass occurrences for a single day.
     * The date gate (semesterStart/End) is enforced by TimetableEntry.isDateInRange().
     */
    public List<TimetableClass> expandForDay(List<TimetableEntry> entries,
                                              LocalDate date) {
        List<TimetableClass> result = new ArrayList<>();
        if (entries == null || date == null) return result;

        for (TimetableEntry entry : entries) {
            if (!isExpandable(entry))             continue;
            if (!entry.isDateInRange(date))        continue;  // semester gate
            if (entry.getDayOfWeek() != date.getDayOfWeek()) continue;

            result.add(toTimetableClass(entry, date));
        }
        return result;
    }

    /**
     * Expands all entries into TimetableClass occurrences across a date range.
     * Useful for weekly views.
     */
    public List<TimetableClass> expandRange(List<TimetableEntry> entries,
                                             LocalDate from, LocalDate to) {
        List<TimetableClass> result = new ArrayList<>();
        if (entries == null || from == null || to == null || from.isAfter(to))
            return result;

        for (TimetableEntry entry : entries) {
            if (!isExpandable(entry)) continue;

            LocalDate effectiveStart = entry.getSemesterStart() != null &&
                                       entry.getSemesterStart().isAfter(from)
                                       ? entry.getSemesterStart() : from;
            LocalDate effectiveEnd   = entry.getSemesterEnd() != null &&
                                       entry.getSemesterEnd().isBefore(to)
                                       ? entry.getSemesterEnd() : to;
            if (effectiveStart.isAfter(effectiveEnd)) continue;

            LocalDate current = effectiveStart.with(
                TemporalAdjusters.nextOrSame(entry.getDayOfWeek()));
            while (!current.isAfter(effectiveEnd)) {
                result.add(toTimetableClass(entry, current));
                current = current.plusDays(7);
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

    private TimetableClass toTimetableClass(TimetableEntry entry, LocalDate date) {
        LocalDateTime start = LocalDateTime.of(date, entry.getStartTime());
        LocalDateTime end   = LocalDateTime.of(date, entry.getEndTime());
        if (end.isBefore(start)) end = end.plusDays(1); // guard overnight
        return new TimetableClass(
            start, end,
            entry.getTitle(),
            entry.getRoom(),
            entry.getTeacher(),
            entry.getColor(),
            entry.getTag());
    }
}
