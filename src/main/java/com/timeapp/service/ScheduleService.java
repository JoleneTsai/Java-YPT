package com.timeapp.service;

import com.timeapp.model.*;
import com.timeapp.repository.ScheduleRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Business façade — the only class DashboardController calls.
 *
 * Owns the in-memory ScheduleData and coordinates:
 *   • ScheduleRepository   — load / save to JSON
 *   • TimetableExpander    — expand recurring rules into TimetableClass instances
 *   • TimelineBuilder      — merge + sort all entries for a given day
 *
 * Every mutating operation automatically calls saveAll() so the UI never
 * needs to trigger saves manually.
 *
 * ── Wiring (in DashboardController constructor) ───────────────────────────────
 *   service = new ScheduleService(
 *       new JsonScheduleRepository(),
 *       new TimetableExpander(),
 *       new TimelineBuilder());
 *   service.loadData();
 * ─────────────────────────────────────────────────────────────────────────────
 */
public class ScheduleService {

    private final ScheduleRepository repository;
    private final TimetableExpander  expander;
    private final TimelineBuilder    builder;
    private       ScheduleData       data;

    public ScheduleService(ScheduleRepository repository,
                            TimetableExpander  expander,
                            TimelineBuilder    builder) {
        this.repository = Objects.requireNonNull(repository);
        this.expander   = Objects.requireNonNull(expander);
        this.builder    = Objects.requireNonNull(builder);
        this.data       = new ScheduleData();
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    public void loadData() {
        data = repository.loadAll();
    }

    public void saveData() {
        repository.saveAll(data);
    }

    // ── Timeline query — primary method for DashboardController ──────────────

    /**
     * Returns all TimeEntry items for the given date, sorted by startTime.
     *
     * Includes:
     *   1. CalendarEvents whose startTime.toLocalDate() == date
     *   2. TodoEntries    whose startTime.toLocalDate() == date
     *   3. TimetableClass occurrences expanded from TimetableEntries
     *      (only those whose semester date range covers date)
     */
    public List<TimeEntry> getTimelineForDay(LocalDate date) {
        if (date == null) return Collections.emptyList();

        List<CalendarEvent>  events  = calendarEventsOnDay(date);
        List<TodoEntry>      todos   = todoEntriesOnDay(date);
        List<TimetableClass> classes = expander.expandForDay(
                                           data.getTimetableEntries(), date);

        return builder.buildTimeline(events, todos, classes);
    }

    // ── Calendar events ───────────────────────────────────────────────────────

    public void addCalendarEvent(CalendarEvent event) {
        Objects.requireNonNull(event);
        data.getCalendarEvents().add(event);
        saveData();
    }

    public List<CalendarEvent> getAllCalendarEvents() {
        return Collections.unmodifiableList(data.getCalendarEvents());
    }

    // ── To-do entries ─────────────────────────────────────────────────────────

    public void addTodoEntry(TodoEntry entry) {
        Objects.requireNonNull(entry);
        data.getTodoEntries().add(entry);
        saveData();
    }

    public void updateTodoCompleted(TodoEntry entry, boolean completed) {
        entry.setCompleted(completed);
        saveData();
    }

    public void deleteTodoEntry(TodoEntry entry) {
        data.getTodoEntries().remove(entry);
        saveData();
    }

    public List<TodoEntry> getAllTodoEntries() {
        return Collections.unmodifiableList(data.getTodoEntries());
    }

    // ── Timetable entries (recurring rules) ───────────────────────────────────

    /**
     * Persists one or more TimetableEntry rules produced by
     * TimetableClassRecord.toTimetableEntries().
     */
    public void addTimetableEntries(List<TimetableEntry> entries) {
        Objects.requireNonNull(entries);
        data.getTimetableEntries().addAll(entries);
        saveData();
    }

    public List<TimetableEntry> getAllTimetableEntries() {
        return Collections.unmodifiableList(data.getTimetableEntries());
    }

    /**
     * Returns only the TimetableEntries whose semesterStart/End matches
     * the given SemesterInfo id — used when loading a TimeTable's classes.
     */
    public List<TimetableEntry> getEntriesForSemester(SemesterInfo info) {
        if (info == null) return Collections.emptyList();
        return data.getTimetableEntries().stream()
            .filter(e -> Objects.equals(e.getSemesterStart(), info.getStartDate())
                      && Objects.equals(e.getSemesterEnd(),   info.getEndDate()))
            .collect(java.util.stream.Collectors.toList());
    }

    // ── Semester / TimeTable ──────────────────────────────────────────────────

    public void addSemester(SemesterInfo info) {
        Objects.requireNonNull(info);
        data.getSemesters().add(info);
        saveData();
    }

    public void updateSemester(SemesterInfo info) {
        for (int i = 0; i < data.getSemesters().size(); i++) {
            if (data.getSemesters().get(i).getId().equals(info.getId())) {
                data.getSemesters().set(i, info);
                break;
            }
        }
        saveData();
    }

    public List<SemesterInfo> getAllSemesters() {
        return Collections.unmodifiableList(data.getSemesters());
    }

    // ── Raw access for seeding ─────────────────────────────────────────────────

    public ScheduleData getData() { return data; }

    public boolean isEmpty() {
        return data.getSemesters().isEmpty()
            && data.getCalendarEvents().isEmpty()
            && data.getTodoEntries().isEmpty()
            && data.getTimetableEntries().isEmpty();
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private List<CalendarEvent> calendarEventsOnDay(LocalDate date) {
        return data.getCalendarEvents().stream()
            .filter(e -> e.getStartTime() != null)
            .filter(e -> e.getStartTime().toLocalDate().equals(date))
            .collect(java.util.stream.Collectors.toList());
    }

    private List<TodoEntry> todoEntriesOnDay(LocalDate date) {
        return data.getTodoEntries().stream()
            .filter(e -> e.getStartTime() != null)
            .filter(e -> e.getStartTime().toLocalDate().equals(date))
            .collect(java.util.stream.Collectors.toList());
    }
}
