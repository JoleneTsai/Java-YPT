package com.timeapp.domain.model;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Persisted timetable container.
 *
 * This is separate from com.timeapp.model.TimeTable, which is a JavaFX UI model.
 * TimetableData is plain Java and is safe for JSON storage.
 */
public class TimetableData {

    private String id;
    private String title;
    private LocalDate startDate;
    private LocalDate endDate;
    private List<TimetableEntry> classes = new ArrayList<>();

    public TimetableData() {
        this.id = UUID.randomUUID().toString();
    }

    public TimetableData(String id, String title, LocalDate startDate, LocalDate endDate,
                         List<TimetableEntry> classes) {
        this.id = id == null || id.isBlank() ? UUID.randomUUID().toString() : id;
        this.title = title;
        this.startDate = startDate;
        this.endDate = endDate;
        this.classes = classes == null ? new ArrayList<>() : classes;
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public List<TimetableEntry> getClasses() {
        return classes;
    }

    public void setId(String id) {
        this.id = id == null || id.isBlank() ? UUID.randomUUID().toString() : id;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public void setClasses(List<TimetableEntry> classes) {
        this.classes = classes == null ? new ArrayList<>() : classes;
    }
}
