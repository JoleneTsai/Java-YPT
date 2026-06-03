package model;

import java.time.LocalDateTime;

public class CalendarEvent implements Schedulable {
    private String title;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String location;
    private String description;
    private String color;
    private String tag;

    public CalendarEvent() {
    }

    public CalendarEvent(String title, LocalDateTime startTime, LocalDateTime endTime,
                         String location, String description, String color, String tag) {
        this.title = title;
        this.startTime = startTime;
        this.endTime = endTime;
        this.location = location;
        this.description = description;
        this.color = color;
        this.tag = tag;
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
        return title;
    }

    @Override
    public EventType getType() {
        return EventType.CALENDAR;
    }

    public String getLocation() {
        return location;
    }

    public String getDescription() {
        return description;
    }

    public String getColor() {
        return color;
    }

    public String getTag() {
        return tag;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }
}
