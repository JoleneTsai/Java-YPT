package com.timeapp.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * Lightweight POJO that persists semester metadata in ScheduleData.
 *
 * The JavaFX UI counterpart is {@link TimeTable}, which has Observable
 * properties and an ObservableList of TimetableClassRecord objects.
 * DashboardController converts between the two at load/save time.
 *
 * Kept as a separate POJO (not TimeTable itself) so Jackson never needs
 * to handle JavaFX Observable fields.
 */
public class SemesterInfo {

    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("yyyy/MM/dd");

    private String    id;
    private String    title;
    private LocalDate startDate;
    private LocalDate endDate;

    public SemesterInfo() {}

    @JsonCreator
    public SemesterInfo(
            @JsonProperty("id")        String    id,
            @JsonProperty("title")     String    title,
            @JsonProperty("startDate") LocalDate startDate,
            @JsonProperty("endDate")   LocalDate endDate) {
        this.id        = id        != null ? id : UUID.randomUUID().toString();
        this.title     = title;
        this.startDate = startDate;
        this.endDate   = endDate;
    }

    public SemesterInfo(String title, LocalDate startDate, LocalDate endDate) {
        this(UUID.randomUUID().toString(), title, startDate, endDate);
    }

    public String    getId()        { return id; }
    public String    getTitle()     { return title; }
    public LocalDate getStartDate() { return startDate; }
    public LocalDate getEndDate()   { return endDate; }

    public void setId(String v)         { this.id        = v; }
    public void setTitle(String v)      { this.title     = v; }
    public void setStartDate(LocalDate v){ this.startDate = v; }
    public void setEndDate(LocalDate v) { this.endDate   = v; }

    public String getDateRangeDisplay() {
        String s = startDate != null ? startDate.format(DATE_FMT) : "—";
        String e = endDate   != null ? endDate.format(DATE_FMT)   : "—";
        return s + " – " + e;
    }

    /** True when date falls within [startDate, endDate] inclusive. */
    public boolean isDateInRange(LocalDate date) {
        if (date == null) return false;
        boolean afterStart = startDate == null || !date.isBefore(startDate);
        boolean beforeEnd  = endDate   == null || !date.isAfter(endDate);
        return afterStart && beforeEnd;
    }
}
