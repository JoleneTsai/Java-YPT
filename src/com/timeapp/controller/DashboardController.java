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
 */
public class DashboardController {

    // ── State ────────────────────────────────────────────────────────────────

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

    /** Navigation destination requested by sidebar. */
    private final StringProperty navigationRequest = new SimpleStringProperty("");

    /** All entries for the currently selected day. */
    private final ObservableList<TimeEntry> dayEntries =
            FXCollections.observableArrayList();

    /** All semester timetables created in the UI. */
    private final ObservableList<TimeTable> timetableList =
            FXCollections.observableArrayList();

    /** The timetable currently shown in the timetable section. */
    private final ObjectProperty<TimeTable> activeTimetable =
            new SimpleObjectProperty<>();

    /** Current timetable sub-panel: MAIN, ADD, LIST, or CREATE. */
    private final StringProperty activeTimetablePane =
            new SimpleStringProperty("MAIN");

    // ── Formatters ───────────────────────────────────────────────────────────

    public static final DateTimeFormatter HEADER_FMT =
            DateTimeFormatter.ofPattern("EEE, MMM d");

    // ── Sidebar animation target (set by DashboardView) ──────────────────────
    private Pane sidebarPane;
    private Timeline sidebarTimeline;
    private static final double SIDEBAR_WIDTH = 260;

    // ── Constructor ──────────────────────────────────────────────────────────

    public DashboardController() {
        seedTimetables();
        loadEntriesForDay(selectedDay.get());
        selectedDay.addListener((obs, o, n) -> loadEntriesForDay(n));
        activeTimetable.addListener((obs, o, n) -> loadEntriesForDay(selectedDay.get()));
    }

    // ── Public API (called by View) ───────────────────────────────────────────

    /** Toggle the sidebar drawer open/closed. */
    public void toggleSidebar() {
        sidebarOpen.set(!sidebarOpen.get());
        animateSidebar(sidebarOpen.get());
    }

    /** Close the sidebar (e.g., when clicking the overlay). */
    public void closeSidebar() {
        if (sidebarOpen.get()) {
            sidebarOpen.set(false);
            animateSidebar(false);
        }
    }

    /** Jump selected day back to today and center the week. */
    public void backToToday() {
        LocalDate today = LocalDate.now();
        selectedDay.set(today);
        weekStart.set(sundayOf(today));
    }

    /** Advance the visible week strip by one week. */
    public void nextWeek() {
        weekStart.set(weekStart.get().plusWeeks(1));
    }

    /** Retreat the visible week strip by one week. */
    public void prevWeek() {
        weekStart.set(weekStart.get().minusWeeks(1));
    }

    /** Select a specific day from the week strip. */
    public void selectDay(LocalDate date) {
        selectedDay.set(date);
        closeSidebar();
        collapseFab();
    }

    /** Toggle the FAB speed-dial. */
    public void toggleFab() {
        fabExpanded.set(!fabExpanded.get());
    }

    /** Collapse the FAB speed-dial without toggling. */
    public void collapseFab() {
        fabExpanded.set(false);
    }

    /** Called when user picks "Add Schedule" from FAB menu. */
    public void onAddSchedule() {
        collapseFab();
        System.out.println("[Action] Open Add Schedule dialog");
        // TODO: open AddScheduleDialog
    }

    /** Called when user picks "Add To-do" from FAB menu. */
    public void onAddTodo() {
        collapseFab();
        System.out.println("[Action] Open Add To-Do dialog");
        // TODO: open AddTodoDialog
    }

    /** Navigate to a named view (Timeline / TimeTable / Calendar). */
    public void navigateTo(String viewName) {
        closeSidebar();
        collapseFab();
        navigationRequest.set(viewName);
        System.out.println("[Navigation] → " + viewName);
        // TODO: swap center content or push new scene
    }

    /** Opens the timetable list sub-panel. */
    public void showTimetableList() {
        activeTimetablePane.set("LIST");
        closeSidebar();
        collapseFab();
    }

    /** Opens the class creation sub-panel. */
    public void showAddClassPane() {
        ensureActiveTimetable();
        activeTimetablePane.set("ADD");
        closeSidebar();
        collapseFab();
    }

    /** Opens the new timetable form. */
    public void showCreateTimetable() {
        activeTimetablePane.set("CREATE");
        closeSidebar();
        collapseFab();
    }

    /** Returns from a sub-panel to the timetable main grid. */
    public void backToTimetableMain() {
        activeTimetablePane.set("MAIN");
    }

    /** Returns from the create form to the timetable list. */
    public void backToTimetableList() {
        activeTimetablePane.set("LIST");
    }

    /** Saves a class record into the active timetable. */
    public void saveClass(TimetableClassRecord record) {
        if (record == null) {
            return;
        }
        ensureActiveTimetable();
        activeTimetable.get().getClasses().add(record);
        loadEntriesForDay(selectedDay.get());
        backToTimetableMain();
    }

    /** Creates a new timetable and makes it active. */
    public void saveNewTimetable(String title, LocalDate start, LocalDate end) {
        TimeTable timetable = new TimeTable(title, start, end);
        timetableList.add(timetable);
        setActiveTimetable(timetable);
        backToTimetableList();
    }

    /** Changes the active timetable shown by timetable views. */
    public void setActiveTimetable(TimeTable timetable) {
        activeTimetable.set(timetable);
        loadEntriesForDay(selectedDay.get());
    }

    // ── Sidebar animation wiring ──────────────────────────────────────────────

    /**
     * Called once by DashboardView after the sidebar pane is built,
     * so the controller can drive its translateX.
     */
    public void wireSidebarPane(Pane sidebar) {
        this.sidebarPane = sidebar;
        sidebar.setTranslateX(-SIDEBAR_WIDTH); // start hidden
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

    // ── Sample data ───────────────────────────────────────────────────────────

    private void loadEntriesForDay(LocalDate date) {
        dayEntries.clear();
        // Seed deterministic sample data based on weekday
        List<TimeEntry> samples = buildSampleEntries(date);
        dayEntries.addAll(samples);
    }

    private List<TimeEntry> buildSampleEntries(LocalDate date) {
        List<TimeEntry> list = new ArrayList<>();
        DayOfWeek dow = date.getDayOfWeek();

        // Always: a morning to-do
        list.add(new TodoEntry(LocalTime.of(8, 0),  "Review lecture notes"));
        list.add(new TodoEntry(LocalTime.of(8, 30), "Reply to group-project emails"));

        // Mon / Wed / Fri — timetable classes
        if (dow == DayOfWeek.MONDAY || dow == DayOfWeek.WEDNESDAY || dow == DayOfWeek.FRIDAY) {
            list.add(new TimetableClass(
                LocalTime.of(9, 0), LocalTime.of(10, 30),
                "Algorithms & Data Structures",
                "Room 301, Eng. Building",
                "Prof. Chen Wei"));
            list.add(new TimetableClass(
                LocalTime.of(14, 0), LocalTime.of(15, 30),
                "Linear Algebra",
                "Room 205, Math Block",
                "Prof. Sarah Kim"));
        }

        addActiveTimetableClasses(list, date);

        // Tue / Thu — calendar events
        if (dow == DayOfWeek.TUESDAY || dow == DayOfWeek.THURSDAY) {
            list.add(new CalendarEvent(
                LocalTime.of(10, 0), LocalTime.of(11, 0),
                "Team Sprint Planning",
                "Zoom — link in calendar"));
            list.add(new CalendarEvent(
                LocalTime.of(15, 0), LocalTime.of(16, 30),
                "Library Study Session",
                "Central Library, 3F"));
        }

        // Every day: afternoon to-do + evening event
        list.add(new TodoEntry(LocalTime.of(12, 0), "Lunch & short walk"));
        list.add(new CalendarEvent(
            LocalTime.of(19, 0), LocalTime.of(20, 0),
            "Gym — Cardio Day",
            "University Sports Centre"));

        return list;
    }

    private void addActiveTimetableClasses(List<TimeEntry> list, LocalDate date) {
        TimeTable timetable = activeTimetable.get();
        if (timetable == null || !timetable.isDateInRange(date)) {
            return;
        }

        for (TimetableClassRecord record : timetable.getClasses()) {
            for (TimetableClassRecord.ClassTimeSlot slot : record.getTimeSlots()) {
                if (slot.getDayOfWeek() == date.getDayOfWeek()) {
                    list.add(new TimetableClass(
                        slot.getStartTime(),
                        slot.getEndTime(),
                        record.getSubject(),
                        record.getClassroom(),
                        record.getTeacher()
                    ));
                }
            }
        }
    }

    private void seedTimetables() {
        if (!timetableList.isEmpty()) {
            return;
        }

        LocalDate semesterStart = LocalDate.now().withDayOfMonth(1);
        TimeTable current = new TimeTable(
            "Current Semester",
            semesterStart,
            semesterStart.plusMonths(5).minusDays(1)
        );
        current.getClasses().add(TimetableClassRecord.of(
            "Algorithms & Data Structures",
            "Prof. Chen Wei",
            "Room 301",
            TimetableClassRecord.AccentColor.PURPLE,
            DayOfWeek.MONDAY,
            LocalTime.of(9, 0),
            LocalTime.of(10, 30)
        ));
        current.getClasses().add(TimetableClassRecord.of(
            "Linear Algebra",
            "Prof. Sarah Kim",
            "Room 205",
            TimetableClassRecord.AccentColor.BLUE,
            DayOfWeek.WEDNESDAY,
            LocalTime.of(14, 0),
            LocalTime.of(15, 30)
        ));

        timetableList.add(current);
        activeTimetable.set(current);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /** Returns the Sunday that starts the ISO week containing {@code date}. */
    public static LocalDate sundayOf(LocalDate date) {
        return date.minusDays(date.getDayOfWeek().getValue() % 7);
    }

    // ── Property accessors ────────────────────────────────────────────────────

    public ObjectProperty<LocalDate>    weekStartProperty()       { return weekStart; }
    public ObjectProperty<LocalDate>    selectedDayProperty()     { return selectedDay; }
    public BooleanProperty              sidebarOpenProperty()     { return sidebarOpen; }
    public BooleanProperty              fabExpandedProperty()     { return fabExpanded; }
    public StringProperty               navigationRequestProperty(){ return navigationRequest; }
    public ObservableList<TimeEntry>    getDayEntries()           { return dayEntries; }
    public ObservableList<TimeTable>    getTimetableList()        { return timetableList; }
    public ObjectProperty<TimeTable>    activeTimetableProperty() { return activeTimetable; }
    public StringProperty               activeTimetablePaneProperty() { return activeTimetablePane; }

    public LocalDate getWeekStart()  { return weekStart.get(); }
    public LocalDate getSelectedDay(){ return selectedDay.get(); }
    public boolean   isSidebarOpen() { return sidebarOpen.get(); }
    public boolean   isFabExpanded() { return fabExpanded.get(); }
    public TimeTable getActiveTimetable() { return activeTimetable.get(); }

    private void ensureActiveTimetable() {
        if (activeTimetable.get() == null) {
            seedTimetables();
        }
    }
}
