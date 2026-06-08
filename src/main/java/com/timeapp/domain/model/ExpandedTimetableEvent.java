package com.timeapp.domain.model;

import java.time.LocalDateTime;

/**
 * A concrete occurrence of a recurring {@link TimetableEntry}.
 *
 * Created at runtime by {@link com.timeapp.domain.service.TimetableExpander}
 * for a specific calendar date. For example, a TimetableEntry for
 * "Monday 09:00–10:30" expands into separate ExpandedTimetableEvents
 * for 2025-02-17 09:00, 2025-02-24 09:00, …, up to semesterEnd.
 *
 * Implements Schedulable so it can be merged with CalendarEvents
 * into a single unified List<Schedulable> by TimelineBuilder.
 */
public class ExpandedTimetableEvent implements Schedulable {

    private final LocalDateTime  startTime;
    private final LocalDateTime  endTime;
    private final TimetableEntry source;

    public ExpandedTimetableEvent(LocalDateTime startTime,
                                   LocalDateTime endTime,
                                   TimetableEntry source) {
        this.startTime = startTime;
        this.endTime   = endTime;
        this.source    = source;
    }

    @Override public LocalDateTime getStartTime() { return startTime; }
    @Override public LocalDateTime getEndTime()   { return endTime; }
    @Override public String        getTitle()     { return source.getTitle(); }
    @Override public EventType     getType()      { return EventType.TIMETABLE; }

    /** The original recurring rule that produced this occurrence. */
    public TimetableEntry getSource() { return source; }
}
