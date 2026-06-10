package com.timeapp.ui.model;

import javafx.beans.property.*;
import javafx.collections.*;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.UUID;

/**
 * Represents one enrolled course inside a {@link TimeTable}.
 *
 * Distinct from the existing {@link TimetableClass} (which is a TimeEntry
 * for display on the daily timeline).  This model is the *source of truth*
 * held by the semester container; TimetableClass instances are derived from
 * it at render time by DashboardController.
 *
 * A single course can recur on multiple days/times ??e.g. a course that meets
 * Monday 09:00??0:30 AND Wednesday 09:00??0:30 is represented as ONE
 * TimetableClassRecord with TWO ClassTimeSlot entries.
 */
public class TimetableClassRecord {

    // ?? Preset accent colors for card theming ?????????????????????????????????
    public enum AccentColor {
        PURPLE ("#9B72FF", "#F5F0FF"),
        BLUE   ("#4A9EFF", "#F0F4FF"),
        TEAL   ("#30D5C8", "#F0FAFA"),
        GREEN  ("#34C759", "#F0FFF4"),
        ORANGE ("#FF9F0A", "#FFF8F0"),
        RED    ("#FF453A", "#FFF0F0"),
        PINK   ("#FF6B9D", "#FFF0F5");

        public final String strip;   // left accent strip colour
        public final String bg;      // card background colour

        AccentColor(String strip, String bg) { this.strip = strip; this.bg = bg; }
    }

    // ?? Time slot: one recurrence instance ????????????????????????????????????
    public static class ClassTimeSlot {
        private final ObjectProperty<DayOfWeek> day       = new SimpleObjectProperty<>();
        private final ObjectProperty<LocalTime> startTime = new SimpleObjectProperty<>();
        private final ObjectProperty<LocalTime> endTime   = new SimpleObjectProperty<>();

        public ClassTimeSlot(DayOfWeek day, LocalTime start, LocalTime end) {
            this.day.set(day);
            this.startTime.set(start);
            this.endTime.set(end);
        }

        public DayOfWeek getDayOfWeek()  { return day.get(); }
        public LocalTime getStartTime()  { return startTime.get(); }
        public LocalTime getEndTime()    { return endTime.get(); }

        public ObjectProperty<DayOfWeek> dayProperty()       { return day; }
        public ObjectProperty<LocalTime> startTimeProperty() { return startTime; }
        public ObjectProperty<LocalTime> endTimeProperty()   { return endTime; }
    }

    // ?? Fields ????????????????????????????????????????????????????????????????

    private final String id = UUID.randomUUID().toString();

    private final StringProperty subject   = new SimpleStringProperty("");
    private final StringProperty teacher   = new SimpleStringProperty("");
    private final StringProperty classroom = new SimpleStringProperty("");

    private final ObjectProperty<AccentColor> accentColor =
            new SimpleObjectProperty<>(AccentColor.PURPLE);

    /** All recurrence slots for this course (multiple days allowed). */
    private final ObservableList<ClassTimeSlot> timeSlots =
            FXCollections.observableArrayList();

    // ?? Constructors ??????????????????????????????????????????????????????????

    public TimetableClassRecord(String subject, String teacher,
                                 String classroom, AccentColor color) {
        this.subject.set(subject);
        this.teacher.set(teacher);
        this.classroom.set(classroom);
        this.accentColor.set(color);
    }

    /** Creates a fully-specified single-slot class record. */
    public static TimetableClassRecord of(String subject, String teacher,
                                           String classroom, AccentColor color,
                                           DayOfWeek day, LocalTime start, LocalTime end) {
        TimetableClassRecord r = new TimetableClassRecord(subject, teacher, classroom, color);
        r.addTimeSlot(new ClassTimeSlot(day, start, end));
        return r;
    }

    // ?? Mutation ??????????????????????????????????????????????????????????????

    public void addTimeSlot(ClassTimeSlot slot) { timeSlots.add(slot); }
    public void removeTimeSlot(ClassTimeSlot slot) { timeSlots.remove(slot); }

    // ?? Property accessors ????????????????????????????????????????????????????

    public String                             getId()          { return id; }
    public String                             getSubject()     { return subject.get(); }
    public void                               setSubject(String v){ subject.set(v); }
    public StringProperty                     subjectProperty(){ return subject; }

    public String                             getTeacher()     { return teacher.get(); }
    public void                               setTeacher(String v){ teacher.set(v); }
    public StringProperty                     teacherProperty(){ return teacher; }

    public String                             getClassroom()   { return classroom.get(); }
    public void                               setClassroom(String v){ classroom.set(v); }
    public StringProperty                     classroomProperty(){ return classroom; }

    public AccentColor                        getAccentColor() { return accentColor.get(); }
    public void                               setAccentColor(AccentColor c){ accentColor.set(c); }
    public ObjectProperty<AccentColor>        accentColorProperty(){ return accentColor; }

    public ObservableList<ClassTimeSlot>      getTimeSlots()   { return timeSlots; }

    @Override public String toString() { return getSubject(); }
}
