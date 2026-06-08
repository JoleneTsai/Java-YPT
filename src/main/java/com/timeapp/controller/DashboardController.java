package com.timeapp.controller;

import com.timeapp.model.*;
import com.timeapp.repository.JsonScheduleRepository;
import com.timeapp.service.*;
import javafx.animation.*;
import javafx.beans.property.*;
import javafx.collections.*;
import javafx.scene.layout.Pane;
import javafx.util.Duration;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Central controller for the Dashboard — the single source of truth.
 *
 * ── Architecture change summary ───────────────────────────────────────────────
 * Previously this class contained hardcoded sample data and LocalTime-based
 * entry construction.  After the full integration:
 *
 *   • Data comes from ScheduleService → JsonScheduleRepository → timeflow-data.json
 *   • loadEntriesForDay() calls service.getTimelineForDay(date) which handles
 *     all filtering, date-gating, and timetable expansion automatically.
 *   • Every mutating action (saveClass, saveNewTimetable, addCalendarEvent…)
 *     delegates to ScheduleService, which auto-saves to JSON.
 *   • All TimeEntry subclasses now use LocalDateTime.
 *
 * ── Timeline filtering ────────────────────────────────────────────────────────
 * TimetableExpander (inside ScheduleService) respects each TimetableEntry's
 * semesterStart/End gate, so classes from past or future semesters never
 * appear in the Timeline without any additional code in this controller.
 * ─────────────────────────────────────────────────────────────────────────────
 */
public class DashboardController {

    // ── Services ──────────────────────────────────────────────────────────────

    private final ScheduleService service;

    // ── Timeline state ────────────────────────────────────────────────────────

    private final ObjectProperty<LocalDate>  weekStart  =
            new SimpleObjectProperty<>(sundayOf(LocalDate.now()));
    private final ObjectProperty<LocalDate>  selectedDay =
            new SimpleObjectProperty<>(LocalDate.now());
    private final BooleanProperty            sidebarOpen = new SimpleBooleanProperty(false);
    private final BooleanProperty            fabExpanded = new SimpleBooleanProperty(false);
    private final ObservableList<TimeEntry>  dayEntries  =
            FXCollections.observableArrayList();

    // ── Navigation state ──────────────────────────────────────────────────────

    private final StringProperty activePanel         = new SimpleStringProperty("TIMELINE");
    private final StringProperty activeTimetablePane = new SimpleStringProperty("MAIN");

    // ── Timetable UI state ────────────────────────────────────────────────────

    private final ObservableList<TimeTable>  timetableList   =
            FXCollections.observableArrayList();
    private final ObjectProperty<TimeTable>  activeTimetable =
            new SimpleObjectProperty<>();

    // ── Sidebar animation wiring ──────────────────────────────────────────────

    private Pane     sidebarPane;
    private Timeline sidebarTimeline;
    private static final double SIDEBAR_WIDTH = 260;

    public static final DateTimeFormatter HEADER_FMT =
            DateTimeFormatter.ofPattern("EEE, MMM d");

    // ── Constructor ───────────────────────────────────────────────────────────

    public DashboardController() {
        service = new ScheduleService(
            new JsonScheduleRepository(),
            new TimetableExpander(),
            new TimelineBuilder());
        service.loadData();

        if (service.isEmpty()) seedDemoData();

        loadTimeTables();
        loadEntriesForDay(selectedDay.get());

        selectedDay.addListener((obs, o, n) -> loadEntriesForDay(n));
        activeTimetable.addListener((obs, o, n) -> loadEntriesForDay(selectedDay.get()));
    }

    // ── Timeline actions ──────────────────────────────────────────────────────

    public void toggleSidebar() { sidebarOpen.set(!sidebarOpen.get()); animateSidebar(sidebarOpen.get()); }
    public void closeSidebar()  { if (sidebarOpen.get()) { sidebarOpen.set(false); animateSidebar(false); } }

    public void backToToday() {
        LocalDate today = LocalDate.now();
        selectedDay.set(today);
        weekStart.set(sundayOf(today));
    }

    public void nextWeek() { weekStart.set(weekStart.get().plusWeeks(1)); }
    public void prevWeek() { weekStart.set(weekStart.get().minusWeeks(1)); }

    public void selectDay(LocalDate date) {
        selectedDay.set(date);
        closeSidebar();
        collapseFab();
    }

    public void toggleFab()   { fabExpanded.set(!fabExpanded.get()); }
    public void collapseFab() { fabExpanded.set(false); }

    // ── Calendar / ToDo quick-add (from Timeline FAB) ─────────────────────────

    public void onAddSchedule() {
        collapseFab();
        // TODO: open a "Add Calendar Event" dialog; for now add a sample
        LocalDate   day   = selectedDay.get();
        LocalDateTime s   = LocalDateTime.of(day, LocalTime.of(10, 0));
        LocalDateTime e   = LocalDateTime.of(day, LocalTime.of(11, 0));
        CalendarEvent ev  = new CalendarEvent(s, e, "New Event", "TBD");
        service.addCalendarEvent(ev);
        loadEntriesForDay(day);
        System.out.println("[Action] Added calendar event for " + day);
    }

    public void onAddTodo() {
        collapseFab();
        // TODO: open a "Add To-Do" dialog; for now add a sample
        LocalDate   day = selectedDay.get();
        LocalDateTime t = LocalDateTime.of(day, LocalTime.of(9, 0));
        TodoEntry todo  = new TodoEntry(t, "New To-Do");
        service.addTodoEntry(todo);
        loadEntriesForDay(day);
        System.out.println("[Action] Added to-do for " + day);
    }

    // ── Navigation ────────────────────────────────────────────────────────────

    public void navigateTo(String viewName) {
        closeSidebar();
        collapseFab();
        switch (viewName) {
            case "TimeTable" -> { activePanel.set("TIMETABLE"); activeTimetablePane.set("MAIN"); }
            case "TimeLine"  ->   activePanel.set("TIMELINE");
            case "Calendar"  -> { activePanel.set("TIMELINE"); System.out.println("[Nav] Calendar not yet built"); }
            default          ->   System.out.println("[Nav] Unknown: " + viewName);
        }
    }

    public void showAddClassPane()    { activeTimetablePane.set("ADD");    closeSidebar(); collapseFab(); }
    public void showTimetableList()   { activeTimetablePane.set("LIST");   closeSidebar(); collapseFab(); }
    public void showCreateTimetable() { activeTimetablePane.set("CREATE"); }
    public void backToTimetableMain() { activeTimetablePane.set("MAIN"); }
    public void backToTimetableList() { activeTimetablePane.set("LIST"); }

    // ── Timetable domain actions ──────────────────────────────────────────────

    /**
     * Saves a new course record into the active timetable.
     *
     * Converts TimetableClassRecord → List<TimetableEntry> (one per time slot),
     * persists via ScheduleService, updates the UI TimeTable's class list,
     * and refreshes the current day's timeline.
     */
    public void saveClass(TimetableClassRecord record) {
        if (record == null) return;
        TimeTable tt = getActiveTimetable();
        if (tt == null) { System.err.println("[Error] No active timetable"); return; }

        // Convert UI record → persistence entries
        List<TimetableEntry> entries = record.toTimetableEntries(
            tt.getStartDate(), tt.getEndDate());
        service.addTimetableEntries(entries);

        // Keep UI TimeTable in sync
        tt.getClasses().add(record);

        loadEntriesForDay(selectedDay.get());
        backToTimetableMain();
        System.out.println("[Save] Class: " + record.getSubject() + " → " + tt.getTitle());
    }

    /**
     * Creates a new semester, persists it, and makes it the active timetable.
     */
    public void saveNewTimetable(String title, LocalDate start, LocalDate end) {
        SemesterInfo info = new SemesterInfo(title, start, end);
        service.addSemester(info);

        TimeTable ui = TimeTable.fromSemesterInfo(info);
        timetableList.add(ui);
        setActiveTimetable(ui);

        backToTimetableList();
        System.out.println("[Save] TimeTable: " + title + "  " + start + " – " + end);
    }

    /** Switches the active timetable and reloads the current day. */
    public void setActiveTimetable(TimeTable tt) {
        activeTimetable.set(tt);
        // listener fires loadEntriesForDay automatically
    }

    // ── Sidebar animation ─────────────────────────────────────────────────────

    public void wireSidebarPane(Pane sidebar) {
        this.sidebarPane = sidebar;
        sidebar.setTranslateX(-SIDEBAR_WIDTH);
    }

    private void animateSidebar(boolean open) {
        if (sidebarPane == null) return;
        if (sidebarTimeline != null) sidebarTimeline.stop();
        double target = open ? 0 : -SIDEBAR_WIDTH;
        sidebarTimeline = new Timeline(
            new KeyFrame(Duration.millis(280),
                new KeyValue(sidebarPane.translateXProperty(), target, Interpolator.EASE_BOTH)));
        sidebarTimeline.play();
    }

    // ── Entry loading ─────────────────────────────────────────────────────────

    /**
     * Delegates entirely to ScheduleService.
     * Semester date-gating and timetable expansion are handled inside
     * TimetableExpander — this method has no filtering logic of its own.
     */
    private void loadEntriesForDay(LocalDate date) {
        dayEntries.clear();
        if (date == null) return;
        List<TimeEntry> entries = service.getTimelineForDay(date);
        dayEntries.addAll(entries);
    }

    // ── TimeTable UI initialisation ───────────────────────────────────────────

    /**
     * Rebuilds the JavaFX timetableList from persisted SemesterInfo + TimetableEntry data.
     * Called once at startup after service.loadData().
     */
    private void loadTimeTables() {
        timetableList.clear();
        for (SemesterInfo info : service.getAllSemesters()) {
            TimeTable ui = TimeTable.fromSemesterInfo(info);

            // Re-attach TimetableClassRecord objects for the Timetable panels
            for (TimetableEntry entry : service.getEntriesForSemester(info)) {
                ui.getClasses().add(TimetableClassRecord.fromTimetableEntry(entry));
            }
            timetableList.add(ui);
        }

        // Set first semester as active (or null if none)
        if (!timetableList.isEmpty()) activeTimetable.set(timetableList.get(0));
    }

    // ── Demo seed data (first launch only) ───────────────────────────────────

    private void seedDemoData() {
        LocalDate today = LocalDate.now();

        // Semester
        SemesterInfo sem = new SemesterInfo(
            "Current Semester",
            today.withDayOfMonth(1),
            today.withDayOfMonth(1).plusMonths(5).minusDays(1));
        service.addSemester(sem);

        // Two recurring classes
        service.addTimetableEntries(List.of(
            new TimetableEntry("Algorithms & Data Structures",
                DayOfWeek.MONDAY, LocalTime.of(9,0), LocalTime.of(10,30),
                "Room 301", "Prof. Chen Wei", "PURPLE", null,
                sem.getStartDate(), sem.getEndDate()),
            new TimetableEntry("Linear Algebra",
                DayOfWeek.WEDNESDAY, LocalTime.of(14,0), LocalTime.of(15,30),
                "Room 205", "Prof. Sarah Kim", "BLUE", null,
                sem.getStartDate(), sem.getEndDate())
        ));

        // A calendar event today
        service.addCalendarEvent(new CalendarEvent(
            LocalDateTime.of(today, LocalTime.of(13,0)),
            LocalDateTime.of(today, LocalTime.of(14,0)),
            "Team Sprint Planning", "Zoom"));

        // A to-do today
        service.addTodoEntry(new TodoEntry(
            LocalDateTime.of(today, LocalTime.of(8,0)), "Review lecture notes"));

        System.out.println("[Seed] Demo data written to JSON.");
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    public static LocalDate sundayOf(LocalDate date) {
        return date.minusDays(date.getDayOfWeek().getValue() % 7);
    }

    // ── Property accessors ────────────────────────────────────────────────────

    public ObjectProperty<LocalDate>   weekStartProperty()          { return weekStart; }
    public ObjectProperty<LocalDate>   selectedDayProperty()        { return selectedDay; }
    public BooleanProperty             sidebarOpenProperty()        { return sidebarOpen; }
    public BooleanProperty             fabExpandedProperty()        { return fabExpanded; }
    public StringProperty              activePanelProperty()        { return activePanel; }
    public StringProperty              activeTimetablePaneProperty(){ return activeTimetablePane; }
    public ObjectProperty<TimeTable>   activeTimetableProperty()    { return activeTimetable; }
    public ObservableList<TimeEntry>   getDayEntries()              { return dayEntries; }
    public ObservableList<TimeTable>   getTimetableList()           { return timetableList; }

    public LocalDate  getWeekStart()        { return weekStart.get(); }
    public LocalDate  getSelectedDay()      { return selectedDay.get(); }
    public boolean    isSidebarOpen()       { return sidebarOpen.get(); }
    public String     getActivePanel()      { return activePanel.get(); }
    public TimeTable  getActiveTimetable()  { return activeTimetable.get(); }
}
