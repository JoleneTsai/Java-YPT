package model;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;

public class TimetableEntry {
    private String title;
    private DayOfWeek dayOfWeek;
    private LocalTime startTime;
    private LocalTime endTime;
    private String room;
    private String teacher;
    private String color;
    private String tag;
    private LocalDate semesterStart;
    private LocalDate semesterEnd;

    public TimetableEntry() {
    }

    public TimetableEntry(String title, DayOfWeek dayOfWeek, LocalTime startTime, LocalTime endTime,
                          String room, String teacher, String color, String tag,
                          LocalDate semesterStart, LocalDate semesterEnd) {
        this.title = title;
        this.dayOfWeek = dayOfWeek;
        this.startTime = startTime;
        this.endTime = endTime;
        this.room = room;
        this.teacher = teacher;
        this.color = color;
        this.tag = tag;
        this.semesterStart = semesterStart;
        this.semesterEnd = semesterEnd;
    }

    public String getTitle() {
        return title;
    }

    public DayOfWeek getDayOfWeek() {
        return dayOfWeek;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public LocalTime getEndTime() {
        return endTime;
    }

    public String getRoom() {
        return room;
    }

    public String getTeacher() {
        return teacher;
    }

    public String getColor() {
        return color;
    }

    public String getTag() {
        return tag;
    }

    public LocalDate getSemesterStart() {
        return semesterStart;
    }

    public LocalDate getSemesterEnd() {
        return semesterEnd;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setDayOfWeek(DayOfWeek dayOfWeek) {
        this.dayOfWeek = dayOfWeek;
    }

    public void setStartTime(LocalTime startTime) {
        this.startTime = startTime;
    }

    public void setEndTime(LocalTime endTime) {
        this.endTime = endTime;
    }

    public void setRoom(String room) {
        this.room = room;
    }

    public void setTeacher(String teacher) {
        this.teacher = teacher;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public void setSemesterStart(LocalDate semesterStart) {
        this.semesterStart = semesterStart;
    }

    public void setSemesterEnd(LocalDate semesterEnd) {
        this.semesterEnd = semesterEnd;
    }
}
