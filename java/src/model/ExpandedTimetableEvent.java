package model;

import java.time.LocalDateTime;

public class ExpandedTimetableEvent implements Schedulable {
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private TimetableEntry source;

    public ExpandedTimetableEvent(LocalDateTime startTime, LocalDateTime endTime, TimetableEntry source) {
        this.startTime = startTime;
        this.endTime = endTime;
        this.source = source;
    }

    @Override
    public LocalDateTime getStartTime() {
        return startTime;
    }

    @Override
    public LocalDateTime getEndTime() {
        return endTime;
    }

    @Override
    public String getTitle() {
        return source.getTitle();
    }

    @Override
    public EventType getType() {
        return EventType.TIMETABLE;
    }

    public TimetableEntry getSource() {
        return source;
    }
}
