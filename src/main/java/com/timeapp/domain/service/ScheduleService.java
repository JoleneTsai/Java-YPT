package com.timeapp.domain.service;

import com.timeapp.domain.model.CalendarEvent;
import com.timeapp.domain.model.ExpandedTimetableEvent;
import com.timeapp.domain.model.Schedulable;
import com.timeapp.domain.model.ScheduleData;
import com.timeapp.domain.model.TimetableData;
import com.timeapp.domain.model.TimetableEntry;
import com.timeapp.domain.model.ToDoTask;
import com.timeapp.domain.repository.ScheduleRepository;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Business façade — the only class the UI/controller layer should call.
 *
 * Owns the in-memory {@link ScheduleData} and coordinates:
 *   • {@link ScheduleRepository}   — load / save to storage
 *   • {@link TimetableExpander}    — expand recurring rules into dated events
 *   • {@link TimelineBuilder}      — merge + sort events for display
 *
 * Every mutating operation (add*, update*, delete*) automatically persists
 * via {@link #saveData()} so the UI never has to trigger saves manually.
 *
 * ── Wiring in DashboardController (example) ─────────────────────────────────
 *
 *   ScheduleRepository  repo    = new JsonScheduleRepository("timeflow-data.json");
 *   ScheduleService     service = new ScheduleService(repo,
 *                                     new TimetableExpander(),
 *                                     new TimelineBuilder());
 *   service.loadData();
 *
 *   // Query one day's timeline:
 *   List<Schedulable> items = service.getTimeline(today, today);
 *
 * ─────────────────────────────────────────────────────────────────────────────
 */
public class ScheduleService {

    private final ScheduleRepository repository;
    private final TimetableExpander  timetableExpander;
    private final TimelineBuilder    timelineBuilder;
    private       ScheduleData       data;

    public ScheduleService(ScheduleRepository repository,
                            TimetableExpander  timetableExpander,
                            TimelineBuilder    timelineBuilder) {
        this.repository        = Objects.requireNonNull(repository,        "repository");
        this.timetableExpander = Objects.requireNonNull(timetableExpander, "timetableExpander");
        this.timelineBuilder   = Objects.requireNonNull(timelineBuilder,   "timelineBuilder");
        this.data              = new ScheduleData();
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    /** Load all data from storage. Call once at application start. */
    public void loadData() {
        data = normalise(repository.loadAll());
    }

    /** Persist all data to storage. Called automatically by mutating methods. */
    public void saveData() {
        repository.saveAll(data);
    }

    // ── Calendar events ───────────────────────────────────────────────────────

    public void addCalendarEvent(CalendarEvent event) {
        Objects.requireNonNull(event, "event must not be null");
        data.getCalendarEvents().add(event);
        saveData();
    }

    public void updateCalendarEvent(CalendarEvent target, CalendarEvent replacement) {
        Objects.requireNonNull(target, "target must not be null");
        Objects.requireNonNull(replacement, "replacement must not be null");
        target.setTitle(replacement.getTitle());
        target.setStartTime(replacement.getStartTime());
        target.setEndTime(replacement.getEndTime());
        target.setLocation(replacement.getLocation());
        target.setDescription(replacement.getDescription());
        target.setColor(replacement.getColor());
        target.setTag(replacement.getTag());
        saveData();
    }

    public void deleteCalendarEvent(CalendarEvent event) {
        if (event != null && data.getCalendarEvents().remove(event)) {
            saveData();
        }
    }

    // ── Timetable entries ─────────────────────────────────────────────────────

    public void addTimetableEntry(TimetableEntry entry) {
        Objects.requireNonNull(entry, "entry must not be null");
        data.getTimetableEntries().add(entry);
        saveData();
    }

    public void addTimetable(TimetableData timetable) {
        Objects.requireNonNull(timetable, "timetable must not be null");
        if (findTimetable(timetable.getId()).isPresent()) {
            return;
        }
        data.getTimetables().add(timetable);
        saveData();
    }

    public void addClassToTimetable(String timetableId, TimetableEntry entry) {
        Objects.requireNonNull(timetableId, "timetableId must not be null");
        Objects.requireNonNull(entry, "entry must not be null");
        TimetableData timetable = findTimetable(timetableId)
            .orElseThrow(() -> new IllegalArgumentException("Unknown timetable id: " + timetableId));
        timetable.getClasses().add(entry);
        saveData();
    }

    public void updateTimetable(String timetableId, String title,
                                LocalDate startDate, LocalDate endDate) {
        Objects.requireNonNull(timetableId, "timetableId must not be null");
        TimetableData timetable = findTimetable(timetableId)
            .orElseThrow(() -> new IllegalArgumentException("Unknown timetable id: " + timetableId));
        timetable.setTitle(title);
        timetable.setStartDate(startDate);
        timetable.setEndDate(endDate);
        saveData();
    }

    public void deleteTimetable(String timetableId) {
        Objects.requireNonNull(timetableId, "timetableId must not be null");
        boolean removed = data.getTimetables().removeIf(timetable ->
            timetable != null && timetableId.equals(timetable.getId()));
        if (removed) {
            saveData();
        }
    }

    public void replaceTimetableClasses(String timetableId, List<TimetableEntry> entries) {
        Objects.requireNonNull(timetableId, "timetableId must not be null");
        TimetableData timetable = findTimetable(timetableId)
            .orElseThrow(() -> new IllegalArgumentException("Unknown timetable id: " + timetableId));
        timetable.setClasses(entries == null ? new ArrayList<>() : new ArrayList<>(entries));
        saveData();
    }

    // ── To-do tasks ───────────────────────────────────────────────────────────

    public void addToDoTask(ToDoTask task) {
        Objects.requireNonNull(task, "task must not be null");
        data.getTodoTasks().add(task);
        saveData();
    }

    public void updateToDoTask(ToDoTask target, ToDoTask replacement) {
        Objects.requireNonNull(target, "target must not be null");
        Objects.requireNonNull(replacement, "replacement must not be null");
        target.setTitle(replacement.getTitle());
        target.setCompleted(replacement.isCompleted());
        target.setScheduledDate(replacement.getScheduledDate());
        target.setScheduledTime(replacement.getScheduledTime());
        saveData();
    }

    public void updateToDoStatus(int index, boolean completed) {
        data.getTodoTasks().get(index).setCompleted(completed);
        saveData();
    }

    public void deleteToDoTask(int index) {
        data.getTodoTasks().remove(index);
        saveData();
    }

    public void deleteToDoTask(ToDoTask task) {
        if (task != null && data.getTodoTasks().remove(task)) {
            saveData();
        }
    }

    public List<ToDoTask> getToDoTasks() {
        return data.getTodoTasks();
    }

    // ── Timeline query ────────────────────────────────────────────────────────

    /**
     * Returns all schedulable events (calendar + expanded timetable) whose
     * start date falls within [{@code from}, {@code to}], sorted by startTime.
     *
     * This is the primary method called by DashboardController to populate
     * the daily or weekly timeline view.
     */
    public List<Schedulable> getTimeline(LocalDate from, LocalDate to) {
        Objects.requireNonNull(from, "from must not be null");
        Objects.requireNonNull(to,   "to must not be null");
        if (from.isAfter(to)) return List.of();

        List<CalendarEvent>          calEvents = calendarEventsInRange(from, to);
        List<ExpandedTimetableEvent> expanded  =
            timetableExpander.expand(allTimetableEntries(), from, to);

        return timelineBuilder.buildTimeline(calEvents, expanded);
    }

    /** Convenience: single-day query. */
    public List<Schedulable> getTimelineForDay(LocalDate date) {
        return getTimeline(date, date);
    }

    public ScheduleData getData() { return data; }

    // ── Private helpers ───────────────────────────────────────────────────────

    private List<CalendarEvent> calendarEventsInRange(LocalDate from, LocalDate to) {
        return data.getCalendarEvents().stream()
            .filter(Objects::nonNull)
            .filter(e -> e.getStartTime() != null)
            .filter(e -> {
                LocalDate d = e.getStartTime().toLocalDate();
                return !d.isBefore(from) && !d.isAfter(to);
            })
            .collect(Collectors.toList());
    }

    private Optional<TimetableData> findTimetable(String timetableId) {
        return data.getTimetables().stream()
            .filter(Objects::nonNull)
            .filter(timetable -> timetableId.equals(timetable.getId()))
            .findFirst();
    }

    private List<TimetableEntry> allTimetableEntries() {
        List<TimetableEntry> entries = new ArrayList<>();

        for (TimetableData timetable : data.getTimetables()) {
            if (timetable != null && timetable.getClasses() != null) {
                entries.addAll(timetable.getClasses());
            }
        }

        if (entries.isEmpty()) {
            entries.addAll(data.getTimetableEntries());
        }

        return entries;
    }

    /** Ensures no list inside ScheduleData is null (guards against bad JSON). */
    private ScheduleData normalise(ScheduleData loaded) {
        ScheduleData d = loaded != null ? loaded : new ScheduleData();
        if (d.getCalendarEvents()   == null) d.setCalendarEvents(new ArrayList<>());
        if (d.getTodoTasks()        == null) d.setTodoTasks(new ArrayList<>());
        if (d.getTimetableEntries() == null) d.setTimetableEntries(new ArrayList<>());
        if (d.getTimetables()       == null) d.setTimetables(new ArrayList<>());
        for (TimetableData timetable : d.getTimetables()) {
            if (timetable != null && timetable.getClasses() == null) {
                timetable.setClasses(new ArrayList<>());
            }
        }
        return d;
    }
}
