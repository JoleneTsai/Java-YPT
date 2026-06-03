package service;

import model.ExpandedTimetableEvent;
import model.TimetableEntry;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class TimetableExpander {
    private static final int DAYS_PER_WEEK = 7;

    /**
     * Expands weekly timetable rules into concrete runtime events inside the requested date range.
     */
    public List<ExpandedTimetableEvent> expand(List<TimetableEntry> entries, LocalDate from, LocalDate to) {
        Objects.requireNonNull(entries, "entries must not be null");
        Objects.requireNonNull(from, "from must not be null");
        Objects.requireNonNull(to, "to must not be null");

        List<ExpandedTimetableEvent> result = new ArrayList<>();

        if (from.isAfter(to)) {
            return result;
        }

        for (TimetableEntry entry : entries) {
            if (!isExpandable(entry)) {
                continue;
            }

            LocalDate startDate = resolveEffectiveStartDate(entry, from);
            LocalDate endDate = resolveEffectiveEndDate(entry, to);

            if (startDate.isAfter(endDate)) {
                continue;
            }

            LocalDate current = startDate.with(TemporalAdjusters.nextOrSame(entry.getDayOfWeek()));

            while (!current.isAfter(endDate)) {
                result.add(createExpandedEvent(entry, current));
                current = current.plusDays(DAYS_PER_WEEK);
            }
        }

        return result;
    }

    private boolean isExpandable(TimetableEntry entry) {
        return entry != null
                && entry.getDayOfWeek() != null
                && entry.getStartTime() != null
                && entry.getEndTime() != null;
    }

    private LocalDate resolveEffectiveStartDate(TimetableEntry entry, LocalDate requestedStart) {
        if (entry.getSemesterStart() == null || !requestedStart.isBefore(entry.getSemesterStart())) {
            return requestedStart;
        }
        return entry.getSemesterStart();
    }

    private LocalDate resolveEffectiveEndDate(TimetableEntry entry, LocalDate requestedEnd) {
        if (entry.getSemesterEnd() == null || !requestedEnd.isAfter(entry.getSemesterEnd())) {
            return requestedEnd;
        }
        return entry.getSemesterEnd();
    }

    private ExpandedTimetableEvent createExpandedEvent(TimetableEntry entry, LocalDate date) {
        LocalDateTime start = LocalDateTime.of(date, entry.getStartTime());
        LocalDateTime end = LocalDateTime.of(date, entry.getEndTime());

        if (end.isBefore(start)) {
            end = end.plusDays(1);
        }

        return new ExpandedTimetableEvent(start, end, entry);
    }
}
