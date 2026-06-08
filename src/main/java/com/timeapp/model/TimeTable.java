package com.timeapp.model;

import javafx.beans.property.*;
import javafx.collections.*;
import java.time.LocalDate;

/**
 * JavaFX UI semester container. Observable properties allow the timetable
 * panels to bind and react to data changes.
 *
 * This class is NOT serialized to JSON directly. Its data is split into:
 *   SemesterInfo         → stored in ScheduleData.semesters
 *   TimetableEntry list  → stored in ScheduleData.timetableEntries
 *
 * DashboardController rebuilds TimeTable objects from SemesterInfo
 * on load, and converts back to SemesterInfo + TimetableEntries on save.
 */
public class TimeTable {

    private final StringProperty              title     = new SimpleStringProperty("");
    private final ObjectProperty<LocalDate>   startDate = new SimpleObjectProperty<>();
    private final ObjectProperty<LocalDate>   endDate   = new SimpleObjectProperty<>();
    private final ObservableList<TimetableClassRecord> classes =
            FXCollections.observableArrayList();

    /** The ID of the corresponding SemesterInfo (for save-back lookup). */
    private final String semesterId;

    public TimeTable(String semesterId, String title, LocalDate start, LocalDate end) {
        this.semesterId = semesterId;
        this.title.set(title);
        this.startDate.set(start);
        this.endDate.set(end);
    }

    /** Convenience: create from a SemesterInfo POJO. */
    public static TimeTable fromSemesterInfo(SemesterInfo info) {
        return new TimeTable(info.getId(), info.getTitle(),
                             info.getStartDate(), info.getEndDate());
    }

    /** Convert back to a SemesterInfo POJO for persistence. */
    public SemesterInfo toSemesterInfo() {
        return new SemesterInfo(semesterId, getTitle(), getStartDate(), getEndDate());
    }

    /** True when date falls within [startDate, endDate] inclusive. */
    public boolean isDateInRange(LocalDate date) {
        LocalDate s = startDate.get();
        LocalDate e = endDate.get();
        if (date == null) return false;
        boolean afterStart = s == null || !date.isBefore(s);
        boolean beforeEnd  = e == null || !date.isAfter(e);
        return afterStart && beforeEnd;
    }

    public String    getSemesterId() { return semesterId; }
    public String    getTitle()      { return title.get(); }
    public LocalDate getStartDate()  { return startDate.get(); }
    public LocalDate getEndDate()    { return endDate.get(); }

    public void setTitle(String v)       { title.set(v); }
    public void setStartDate(LocalDate v){ startDate.set(v); }
    public void setEndDate(LocalDate v)  { endDate.set(v); }

    public StringProperty            titleProperty()     { return title; }
    public ObjectProperty<LocalDate> startDateProperty() { return startDate; }
    public ObjectProperty<LocalDate> endDateProperty()   { return endDate; }

    public ObservableList<TimetableClassRecord> getClasses() { return classes; }

    public String getDateRangeDisplay() {
        return toSemesterInfo().getDateRangeDisplay();
    }

    @Override public String toString() { return getTitle(); }
}
