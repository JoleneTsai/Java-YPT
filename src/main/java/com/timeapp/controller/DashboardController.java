package com.timeapp.controller;

import com.timeapp.model.*;
import javafx.animation.*;
import javafx.beans.property.*;
import javafx.collections.*;
import javafx.scene.layout.Pane;
import javafx.util.Duration;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Central controller for the Dashboard.
 * Owns all state and provides action methods called by the view.
 * The view observes properties; the controller never touches UI nodes directly.
 *
 * ── Merge notes ───────────────────────────────────────────────────────────────
 * Base: Project A (ypt_todo_no_deadline-merged) — authoritative logic source.
 * Added: {@code activePanel} StringProperty (from Project B) so that
 *        DashboardView can observe when to swap between the Timeline section
 *        and the Timetable section.  All timetable sub-panel routing methods
 *        (showTimetableList, showAddClassPane, etc.) and the date-range
 *        filtering in addActiveTimetableClasses() are kept exactly as in A.
 * ─────────────────────────────────────────────────────────────────────────────
 */
public class DashboardController {

    // ── Timeline state ────────────────────────────────────────────────────────

    /** The week whose Sunday starts the week strip. */
    private final ObjectProperty<LocalDate> weekStart =
            new SimpleObjectProperty<>(sundayOf(LocalDate.now()));

    /** The currently selected/highlighted day. */
    private final ObjectProperty<LocalDate> selectedDay =
            new SimpleObjectProperty<>(LocalDate.now());

    /** Whether the sidebar drawer is open. */
    private final BooleanProperty sidebarOpen = new SimpleBooleanProperty(false);

    /** Whether the FAB speed-dial is expanded. */
    private final BooleanProperty fabExpanded = new SimpleBooleanProperty(false);

    /** All entries for the currently selected day. */
    private final ObservableList<TimeEntry> dayEntries =
            FXCollections.observableArrayList();

    // ── Timetable domain state ────────────────────────────────────────────────

    /** All semester timetables created in the UI. */
    private final ObservableList<TimeTable> timetableList =
            FXCollections.observableArrayList();

    /** The timetable currently shown in the timetable section. */
    private final ObjectProperty<TimeTable> activeTimetable =
            new SimpleObjectProperty<>();

    /** Current timetable sub-panel: "MAIN", "ADD", "LIST", or "CREATE". */
    private final StringProperty activeTimetablePane =
            new SimpleStringProperty("MAIN");

    // ── Top-level panel routing ───────────────────────────────────────────────

    /**
     * Which top-level section DashboardView shows.
     *   "TIMELINE"  → TimelinePane  (default)
     *   "TIMETABLE" → TimetableMainPane
     *
     * DashboardView observes this property and animates the swap.
     */
    private final StringProperty activePanel =
            new SimpleStringProperty("TIMELINE");

    // ── Formatters ────────────────────────────────────────────────────────────

    public static final DateTimeFormatter HEADER_FMT =
            DateTimeFormatter.ofPattern("EEE, MMM d");

    // ── Sidebar animation wiring ──────────────────────────────────────────────

    private Pane     sidebarPane;
    private Timeline sidebarTimeline;
    private static final double SIDEBAR_WIDTH = 260;

    // ── Constructor ───────────────────────────────────────────────────────────

    public DashboardController() {
        seedTimetables();
        loadEntriesForDay(selectedDay.get());
        selectedDay.addListener((obs, o, n) -> loadEntriesForDay(n));
        activeTimetable.addListener((obs, o, n) -> loadEntriesForDay(selectedDay.get()));
    }

    // ── Timeline actions ──────────────────────────────────────────────────────

    public void toggleSidebar() {
        sidebarOpen.set(!sidebarOpen.get());
        animateSidebar(sidebarOpen.get());
    }

    public void closeSidebar() {
        if (sidebarOpen.get()) {
            sidebarOpen.set(false);
            animateSidebar(false);
        }
    }

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

    public void onAddSchedule() {
        collapseFab();
        System.out.println("[Action] Open Add Schedule dialog");
    }

    public void onAddTodo() {
        collapseFab();
        System.out.println("[Action] Open Add To-Do dialog");
    }

    // ── Navigation ────────────────────────────────────────────────────────────

    /**
     * Primary navigation — called by SidebarDrawer when the user taps a nav item.
     *
     * Sets {@code activePanel} so DashboardView animates the section swap,
     * then resets the timetable sub-panel to MAIN whenever the user enters
     * the timetable section.
     */
    public void navigateTo(String viewName) {
        closeSidebar();
        collapseFab();
        switch (viewName) {
            case "TimeTable" -> {
                activePanel.set("TIMETABLE");
                activeTimetablePane.set("MAIN");
            }
            case "TimeLine"  -> activePanel.set("TIMELINE");
            case "Calendar"  -> {
                // Calendar section not yet implemented — stay on timeline
                activePanel.set("TIMELINE");
                System.out.println("[Navigation] Calendar — not yet implemented");
            }
            default -> System.out.println("[Navigation] Unknown view: " + viewName);
        }
    }

    // ── Timetable sub-panel routing ───────────────────────────────────────────

    /** FAB "TimeTable" → show Panel 3. */
    public void showTimetableList() {
        activeTimetablePane.set("LIST");
        closeSidebar();
        collapseFab();
    }

    /** FAB "Add Class" → show Panel 2. */
    public void showAddClassPane() {
        ensureActiveTimetable();
        activeTimetablePane.set("ADD");
        closeSidebar();
        collapseFab();
    }

    /** Panel 3 mini-FAB → show Panel 4. */
    public void showCreateTimetable() {
        activeTimetablePane.set("CREATE");
        closeSidebar();
        collapseFab();
    }

    /** Back arrow in Panel 2, 3 → return to Panel 1. */
    public void backToTimetableMain() {
        activeTimetablePane.set("MAIN");
    }

    /** Back arrow in Panel 4 → return to Panel 3. */
    public void backToTimetableList() {
        activeTimetablePane.set("LIST");
    }

    // ── Timetable domain actions ──────────────────────────────────────────────

    /**
     * Saves a {@link TimetableClassRecord} into the active timetable,
     * refreshes the day's entry list, and navigates back to Panel 1.
     */
    public void saveClass(TimetableClassRecord record) {
        if (record == null) return;
        ensureActiveTimetable();
        activeTimetable.get().getClasses().add(record);
        loadEntriesForDay(selectedDay.get());
        backToTimetableMain();
    }

    /**
     * Creates a new {@link TimeTable}, appends it to the master list,
     * makes it active, and navigates back to Panel 3.
     */
    public void saveNewTimetable(String title, LocalDate start, LocalDate end) {
        TimeTable timetable = new TimeTable(title, start, end);
        timetableList.add(timetable);
        setActiveTimetable(timetable);
        backToTimetableList();
    }

    /** Changes the active timetable and re-renders the current day. */
    public void setActiveTimetable(TimeTable timetable) {
        activeTimetable.set(timetable);
        // activeTimetable listener fires loadEntriesForDay automatically
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
                new KeyValue(sidebarPane.translateXProperty(), target,
                             Interpolator.EASE_BOTH))
        );
        sidebarTimeline.play();
    }

    // ── Entry loading (date-range filtered) ───────────────────────────────────

    private void loadEntriesForDay(LocalDate date) {
        dayEntries.clear();
        if (date == null) return;
        dayEntries.addAll(buildSampleEntries(date));
    }

    private List<TimeEntry> buildSampleEntries(LocalDate date) {
        List<TimeEntry> list = new ArrayList<>();
        DayOfWeek dow = date.getDayOfWeek();

        // Always: morning to-dos
        list.add(new TodoEntry(LocalTime.of(8,  0), "Review lecture notes"));
        list.add(new TodoEntry(LocalTime.of(8, 30), "Reply to group-project emails"));

        // Mon / Wed / Fri — static sample timetable classes (shown when no
        // active timetable is configured, so the app is demo-able immediately)
        if (activeTimetable.get() == null &&
            (dow == DayOfWeek.MONDAY || dow == DayOfWeek.WEDNESDAY || dow == DayOfWeek.FRIDAY)) {
            list.add(new TimetableClass(
                LocalTime.of(9, 0), LocalTime.of(10, 30),
                "Algorithms & Data Structures", "Room 301, Eng. Building", "Prof. Chen Wei"));
            list.add(new TimetableClass(
                LocalTime.of(14, 0), LocalTime.of(15, 30),
                "Linear Algebra", "Room 205, Math Block", "Prof. Sarah Kim"));
        }

        // Classes from the active timetable — gated by semester date range
        addActiveTimetableClasses(list, date);

        // Tue / Thu — calendar events
        if (dow == DayOfWeek.TUESDAY || dow == DayOfWeek.THURSDAY) {
            list.add(new CalendarEvent(
                LocalTime.of(10, 0), LocalTime.of(11, 0),
                "Team Sprint Planning", "Zoom — link in calendar"));
            list.add(new CalendarEvent(
                LocalTime.of(15, 0), LocalTime.of(16, 30),
                "Library Study Session", "Central Library, 3F"));
        }

        // Every day
        list.add(new TodoEntry(LocalTime.of(12, 0), "Lunch & short walk"));
        list.add(new CalendarEvent(
            LocalTime.of(19, 0), LocalTime.of(20, 0),
            "Gym — Cardio Day", "University Sports Centre"));

        return list;
    }

    /**
     * Appends TimetableClass entries for today from the active timetable.
     *
     * ── Date-range contract ───────────────────────────────────────────────────
     * Classes are included ONLY when:
     *   a) A class record has a time slot matching today's DayOfWeek.
     *   b) today falls within [activeTimetable.startDate, activeTimetable.endDate].
     * If the semester has ended, its classes silently stop appearing, keeping
     * the Timeline view accurate without any manual intervention.
     */
    private void addActiveTimetableClasses(List<TimeEntry> list, LocalDate date) {
        TimeTable timetable = activeTimetable.get();
        if (timetable == null || !timetable.isDateInRange(date)) return;

        for (TimetableClassRecord record : timetable.getClasses()) {
            for (TimetableClassRecord.ClassTimeSlot slot : record.getTimeSlots()) {
                if (slot.getDayOfWeek() == date.getDayOfWeek()) {
                    list.add(new TimetableClass(
                        slot.getStartTime(), slot.getEndTime(),
                        record.getSubject(), record.getClassroom(), record.getTeacher()
                    ));
                }
            }
        }
    }

    // ── Sample seed data ──────────────────────────────────────────────────────

    private void seedTimetables() {
        if (!timetableList.isEmpty()) return;

        LocalDate semesterStart = LocalDate.now().withDayOfMonth(1);
        TimeTable current = new TimeTable(
            "Current Semester",
            semesterStart,
            semesterStart.plusMonths(5).minusDays(1));

        current.getClasses().add(TimetableClassRecord.of(
            "Algorithms & Data Structures", "Prof. Chen Wei", "Room 301",
            TimetableClassRecord.AccentColor.PURPLE,
            DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(10, 30)));
        current.getClasses().add(TimetableClassRecord.of(
            "Linear Algebra", "Prof. Sarah Kim", "Room 205",
            TimetableClassRecord.AccentColor.BLUE,
            DayOfWeek.WEDNESDAY, LocalTime.of(14, 0), LocalTime.of(15, 30)));

        timetableList.add(current);
        activeTimetable.set(current);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    public static LocalDate sundayOf(LocalDate date) {
        return date.minusDays(date.getDayOfWeek().getValue() % 7);
    }

    private void ensureActiveTimetable() {
        if (activeTimetable.get() == null) seedTimetables();
    }

    // ── Property accessors ────────────────────────────────────────────────────

    public ObjectProperty<LocalDate>    weekStartProperty()          { return weekStart; }
    public ObjectProperty<LocalDate>    selectedDayProperty()        { return selectedDay; }
    public BooleanProperty              sidebarOpenProperty()        { return sidebarOpen; }
    public BooleanProperty              fabExpandedProperty()        { return fabExpanded; }
    public ObservableList<TimeEntry>    getDayEntries()              { return dayEntries; }
    public ObservableList<TimeTable>    getTimetableList()           { return timetableList; }
    public ObjectProperty<TimeTable>    activeTimetableProperty()    { return activeTimetable; }
    public StringProperty               activeTimetablePaneProperty(){ return activeTimetablePane; }
    public StringProperty               activePanelProperty()        { return activePanel; }

    public LocalDate  getWeekStart()         { return weekStart.get(); }
    public LocalDate  getSelectedDay()       { return selectedDay.get(); }
    public boolean    isSidebarOpen()        { return sidebarOpen.get(); }
    public boolean    isFabExpanded()        { return fabExpanded.get(); }
    public TimeTable  getActiveTimetable()   { return activeTimetable.get(); }
    public String     getActivePanel()       { return activePanel.get(); }
}
