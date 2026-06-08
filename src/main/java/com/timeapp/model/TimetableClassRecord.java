package com.timeapp.model;

import javafx.beans.property.*;
import javafx.collections.*;
import java.time.*;
import java.util.*;

/**
 * UI model for one enrolled course in a semester.
 * Used exclusively by the Timetable panels (TimetableMainPane, AddClassPane).
 *
 * NOT serialized to JSON directly. toTimetableEntries() converts this into
 * one TimetableEntry per time slot for persistence in ScheduleData.
 *
 * AccentColor.name() is stored in TimetableEntry.color so the UI can
 * reconstruct the AccentColor via AccentColor.valueOf(entry.getColor()).
 */
public class TimetableClassRecord {

    // ── Accent colours ─────────────────────────────────────────────────────────
    public enum AccentColor {
        PURPLE("#9B72FF", "#F5F0FF"),
        BLUE  ("#4A9EFF", "#F0F4FF"),
        TEAL  ("#30D5C8", "#F0FAFA"),
        GREEN ("#34C759", "#F0FFF4"),
        ORANGE("#FF9F0A", "#FFF8F0"),
        RED   ("#FF453A", "#FFF0F0"),
        PINK  ("#FF6B9D", "#FFF0F5");

        public final String strip;
        public final String bg;
        AccentColor(String strip, String bg) { this.strip = strip; this.bg = bg; }

        /** Safe lookup; returns PURPLE if name is null or unrecognised. */
        public static AccentColor fromName(String name) {
            if (name == null) return PURPLE;
            try { return valueOf(name); }
            catch (IllegalArgumentException e) { return PURPLE; }
        }
    }

    // ── Time slot ──────────────────────────────────────────────────────────────
    public static class ClassTimeSlot {
        private final ObjectProperty<DayOfWeek> day       = new SimpleObjectProperty<>();
        private final ObjectProperty<LocalTime> startTime = new SimpleObjectProperty<>();
        private final ObjectProperty<LocalTime> endTime   = new SimpleObjectProperty<>();

        public ClassTimeSlot(DayOfWeek day, LocalTime start, LocalTime end) {
            this.day.set(day); this.startTime.set(start); this.endTime.set(end);
        }
        public DayOfWeek getDayOfWeek()  { return day.get(); }
        public LocalTime getStartTime()  { return startTime.get(); }
        public LocalTime getEndTime()    { return endTime.get(); }
        public ObjectProperty<DayOfWeek> dayProperty()       { return day; }
        public ObjectProperty<LocalTime> startTimeProperty() { return startTime; }
        public ObjectProperty<LocalTime> endTimeProperty()   { return endTime; }
    }

    // ── Fields ─────────────────────────────────────────────────────────────────
    private final String                        id          = UUID.randomUUID().toString();
    private final StringProperty                subject     = new SimpleStringProperty("");
    private final StringProperty                teacher     = new SimpleStringProperty("");
    private final StringProperty                classroom   = new SimpleStringProperty("");
    private final ObjectProperty<AccentColor>   accentColor = new SimpleObjectProperty<>(AccentColor.PURPLE);
    private final ObservableList<ClassTimeSlot> timeSlots   = FXCollections.observableArrayList();

    public TimetableClassRecord(String subject, String teacher,
                                 String classroom, AccentColor color) {
        this.subject.set(subject);
        this.teacher.set(teacher);
        this.classroom.set(classroom);
        this.accentColor.set(color);
    }

    public static TimetableClassRecord of(String subject, String teacher,
                                           String classroom, AccentColor color,
                                           DayOfWeek day, LocalTime start, LocalTime end) {
        TimetableClassRecord r = new TimetableClassRecord(subject, teacher, classroom, color);
        r.addTimeSlot(new ClassTimeSlot(day, start, end));
        return r;
    }

    // ── Bridge: UI → persistence ───────────────────────────────────────────────

    /**
     * Converts this UI record into one TimetableEntry per time slot.
     * Each entry carries the semester boundaries so TimetableExpander
     * can apply the date gate correctly.
     *
     * @param semStart first day of the owning semester
     * @param semEnd   last  day of the owning semester
     */
    public List<TimetableEntry> toTimetableEntries(LocalDate semStart, LocalDate semEnd) {
        List<TimetableEntry> entries = new ArrayList<>();
        for (ClassTimeSlot slot : timeSlots) {
            TimetableEntry e = new TimetableEntry(
                getSubject(), slot.getDayOfWeek(),
                slot.getStartTime(), slot.getEndTime(),
                getClassroom(), getTeacher(),
                accentColor.get().name(),   // store enum name for round-trip
                null,                        // tag
                semStart, semEnd);
            entries.add(e);
        }
        return entries;
    }

    // ── Bridge: persistence → UI ───────────────────────────────────────────────

    /**
     * Reconstructs a TimetableClassRecord from a persisted TimetableEntry.
     * One TimetableEntry = one time slot. Multiple entries for the same
     * subject are merged in DashboardController.loadTimeTables().
     */
    public static TimetableClassRecord fromTimetableEntry(TimetableEntry e) {
        AccentColor color = AccentColor.fromName(e.getColor());
        TimetableClassRecord rec = new TimetableClassRecord(
            e.getTitle(), e.getTeacher(), e.getRoom(), color);
        rec.addTimeSlot(new ClassTimeSlot(
            e.getDayOfWeek(), e.getStartTime(), e.getEndTime()));
        return rec;
    }

    // ── Mutations ──────────────────────────────────────────────────────────────
    public void addTimeSlot(ClassTimeSlot slot)    { timeSlots.add(slot); }
    public void removeTimeSlot(ClassTimeSlot slot) { timeSlots.remove(slot); }

    // ── Accessors ──────────────────────────────────────────────────────────────
    public String                             getId()               { return id; }
    public String                             getSubject()          { return subject.get(); }
    public String                             getTeacher()          { return teacher.get(); }
    public String                             getClassroom()        { return classroom.get(); }
    public AccentColor                        getAccentColor()      { return accentColor.get(); }
    public ObservableList<ClassTimeSlot>      getTimeSlots()        { return timeSlots; }

    public void setSubject(String v)           { subject.set(v); }
    public void setTeacher(String v)           { teacher.set(v); }
    public void setClassroom(String v)         { classroom.set(v); }
    public void setAccentColor(AccentColor v)  { accentColor.set(v); }

    public StringProperty          subjectProperty()   { return subject; }
    public StringProperty          teacherProperty()   { return teacher; }
    public StringProperty          classroomProperty() { return classroom; }
    public ObjectProperty<AccentColor> accentColorProperty() { return accentColor; }

    @Override public String toString() { return getSubject(); }
}
