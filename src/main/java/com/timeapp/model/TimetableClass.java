package com.timeapp.model;

import java.time.LocalTime;

/**
 * A timetable class displayed as a purple card on the timeline.
 */
public class TimetableClass extends TimeEntry {

    private final String location;
    private final String professor;

    public TimetableClass(LocalTime start, LocalTime end,
                          String title, String location, String professor) {
        super(start, end, title);
        this.location  = location;
        this.professor = professor;
    }

    public String getLocation()  { return location; }
    public String getProfessor() { return professor; }

    @Override public Type getType() { return Type.TIMETABLE_CLASS; }
}
