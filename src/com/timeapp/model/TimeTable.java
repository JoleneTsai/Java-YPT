package com.timeapp.model;

import javafx.beans.property.*;
import javafx.collections.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * Represents one semester / timetable container.
 *
 * Holds:
 *  • A human-readable title     ("113-2 Semester Spring")
 *  • A semester date range      (start … end)
 *  • An observable list of      TimetableClassRecord objects (the enrolled classes)
 *  • A unique ID for persistence
 *
 * ── Timeline filtering contract ──────────────────────────────────────────────
 * DashboardController.getDayEntries() must check {@link #isDateInRange(LocalDate)}
 * before appending a recurring TimetableClass to the daily list.  Classes from
 * this timetable are ONLY shown when:
 *    start <= queryDate <= end
 * This ensures that past or future semesters do not pollute the live timeline.
 * ─────────────────────────────────────────────────────────────────────────────
 */
public class TimeTable {

    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("yyyy/MM/dd");

    // ── Fields ────────────────────────────────────────────────────────────────

    private final String id;

    private final StringProperty  title    = new SimpleStringProperty("");
    private final ObjectProperty<LocalDate> startDate =
            new SimpleObjectProperty<>(LocalDate.now());
    private final ObjectProperty<LocalDate> endDate   =
            new SimpleObjectProperty<>(LocalDate.now().plusMonths(6));

    /** The classes that belong to this timetable. */
    private final ObservableList<TimetableClassRecord> classes =
            FXCollections.observableArrayList();

    // ── Constructors ──────────────────────────────────────────────────────────

    public TimeTable(String title, LocalDate start, LocalDate end) {
        this.id = UUID.randomUUID().toString();
        this.title.set(title);
        this.startDate.set(start);
        this.endDate.set(end);
    }

    /** Convenience: creates a current-semester timetable from today. */
    public static TimeTable currentSemester(String title) {
        LocalDate today = LocalDate.now();
        return new TimeTable(title, today.withDayOfMonth(1),
                             today.withDayOfMonth(1).plusMonths(5).minusDays(1));
    }

    // ── Business logic ────────────────────────────────────────────────────────

    /**
     * Returns true if {@code date} falls within [startDate, endDate] inclusive.
     * Used by DashboardController to gate whether this timetable's classes
     * should appear in the timeline for a given day.
     */
    public boolean isDateInRange(LocalDate date) {
        LocalDate s = startDate.get();
        LocalDate e = endDate.get();
        if (s == null || e == null || date == null) return false;
        return !date.isBefore(s) && !date.isAfter(e);
    }

    /** Human-readable date range string, e.g. "2025/02/17 – 2025/07/18" */
    public String getDateRangeDisplay() {
        LocalDate s = startDate.get();
        LocalDate e = endDate.get();
        String start = s != null ? s.format(DATE_FMT) : "—";
        String end   = e != null ? e.format(DATE_FMT) : "—";
        return start + " – " + end;
    }

    // ── Property accessors ────────────────────────────────────────────────────

    public String                                getId()           { return id; }
    public String                                getTitle()        { return title.get(); }
    public void                                  setTitle(String v){ title.set(v); }
    public StringProperty                        titleProperty()   { return title; }

    public LocalDate                             getStartDate()    { return startDate.get(); }
    public void                                  setStartDate(LocalDate v) { startDate.set(v); }
    public ObjectProperty<LocalDate>             startDateProperty(){ return startDate; }

    public LocalDate                             getEndDate()      { return endDate.get(); }
    public void                                  setEndDate(LocalDate v)   { endDate.set(v); }
    public ObjectProperty<LocalDate>             endDateProperty() { return endDate; }

    public ObservableList<TimetableClassRecord>  getClasses()      { return classes; }

    @Override public String toString() { return getTitle(); }
}
