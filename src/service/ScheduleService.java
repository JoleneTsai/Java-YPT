package service;

import model.CalendarEvent;
import model.ExpandedTimetableEvent;
import model.ScheduleData;
import model.Schedulable;
import model.TimetableEntry;
import model.ToDoTask;
import repository.ScheduleRepository;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Business facade for schedule operations used by the UI layer.
 */
public class ScheduleService {
    private final ScheduleRepository repository;
    private final TimetableExpander timetableExpander;
    private final TimelineBuilder timelineBuilder;
    private ScheduleData data;

    public ScheduleService(ScheduleRepository repository,
                           TimetableExpander timetableExpander,
                           TimelineBuilder timelineBuilder) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
        this.timetableExpander = Objects.requireNonNull(timetableExpander, "timetableExpander must not be null");
        this.timelineBuilder = Objects.requireNonNull(timelineBuilder, "timelineBuilder must not be null");
        this.data = new ScheduleData();
    }

    public void loadData() {
        data = normalizeData(repository.loadAll());
    }

    public void saveData() {
        repository.saveAll(data);
    }

    public void addCalendarEvent(CalendarEvent event) {
        Objects.requireNonNull(event, "event must not be null");
        data.getCalendarEvents().add(event);
        saveData();
    }

    public void addTimetableEntry(TimetableEntry entry) {
        Objects.requireNonNull(entry, "entry must not be null");
        data.getTimetableEntries().add(entry);
        saveData();
    }

    public void addToDoTask(ToDoTask task) {
        Objects.requireNonNull(task, "task must not be null");
        data.getTodoTasks().add(task);
        saveData();
    }

    public void updateToDoStatus(int index, boolean completed) {
        getToDoTaskAt(index).setCompleted(completed);
        saveData();
    }

    public void deleteToDoTask(int index) {
        data.getTodoTasks().remove(index);
        saveData();
    }

    public List<ToDoTask> getToDoTasks() {
        return data.getTodoTasks();
    }

    /**
     * Returns scheduled calendar and timetable events whose start date is inside the date range.
     */
    public List<Schedulable> getTimeline(LocalDate from, LocalDate to) {
        Objects.requireNonNull(from, "from must not be null");
        Objects.requireNonNull(to, "to must not be null");

        if (from.isAfter(to)) {
            return List.of();
        }

        List<CalendarEvent> calendarEvents = getCalendarEventsStartingBetween(from, to);
        List<ExpandedTimetableEvent> expanded =
                timetableExpander.expand(data.getTimetableEntries(), from, to);

        return timelineBuilder.buildTimeline(
                calendarEvents,
                expanded
        );
    }

    public ScheduleData getData() {
        return data;
    }

    private ToDoTask getToDoTaskAt(int index) {
        return data.getTodoTasks().get(index);
    }

    private List<CalendarEvent> getCalendarEventsStartingBetween(LocalDate from, LocalDate to) {
        return data.getCalendarEvents().stream()
                .filter(Objects::nonNull)
                .filter(event -> event.getStartTime() != null && event.getEndTime() != null)
                .filter(event -> isDateInRange(event.getStartTime().toLocalDate(), from, to))
                .collect(Collectors.toList());
    }

    private boolean isDateInRange(LocalDate date, LocalDate from, LocalDate to) {
        return !date.isBefore(from) && !date.isAfter(to);
    }

    private ScheduleData normalizeData(ScheduleData loadedData) {
        ScheduleData normalized = loadedData == null ? new ScheduleData() : loadedData;

        if (normalized.getCalendarEvents() == null) {
            normalized.setCalendarEvents(new ArrayList<>());
        }
        if (normalized.getTodoTasks() == null) {
            normalized.setTodoTasks(new ArrayList<>());
        }
        if (normalized.getTimetableEntries() == null) {
            normalized.setTimetableEntries(new ArrayList<>());
        }

        return normalized;
    }
}
