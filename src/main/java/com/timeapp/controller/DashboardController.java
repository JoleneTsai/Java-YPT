package com.timeapp.controller;

import com.timeapp.domain.model.ScheduleData;
import com.timeapp.domain.model.TimetableData;
import com.timeapp.domain.model.TimetableEntry;
import com.timeapp.domain.model.ToDoTask;
import com.timeapp.domain.repository.JsonScheduleRepository;
import com.timeapp.domain.service.ScheduleService;
import com.timeapp.domain.service.TimelineBuilder;
import com.timeapp.domain.service.TimetableExpander;
import com.timeapp.ui.model.*;
import javafx.animation.*;
import javafx.beans.property.*;
import javafx.collections.*;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.util.Duration;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Central controller for the Dashboard.
 * Owns all state and provides action methods called by the view.
 * The view observes properties; the controller never touches UI nodes directly.
 *
 * ?? Merge notes ???????????????????????????????????????????????????????????????
 * Base: Project A (ypt_todo_no_deadline-merged) ??authoritative logic source.
 * Added: {@code activePanel} StringProperty (from Project B) so that
 *        DashboardView can observe when to swap between the Timeline section
 *        and the Timetable section.  All timetable sub-panel routing methods
 *        (showTimetableList, showAddClassPane, etc.) and the date-range
 *        filtering in addActiveTimetableClasses() are kept exactly as in A.
 * ?????????????????????????????????????????????????????????????????????????????
 */
public class DashboardController {

    private static final String DATA_FILE_PATH = resolveDataFilePath();

    // ?? Timeline state ????????????????????????????????????????????????????????

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

    // ?? Timetable domain state ????????????????????????????????????????????????

    /** All semester timetables created in the UI. */
    private final ObservableList<TimeTable> timetableList =
            FXCollections.observableArrayList();

    /** The timetable currently shown in the timetable section. */
    private final ObjectProperty<TimeTable> activeTimetable =
            new SimpleObjectProperty<>();

    /** Current timetable sub-panel: "MAIN", "ADD", "LIST", or "CREATE". */
    private final StringProperty activeTimetablePane =
            new SimpleStringProperty("MAIN");

    // ?? Top-level panel routing ???????????????????????????????????????????????

    /**
     * Which top-level section DashboardView shows.
     *   "TIMELINE"  ??TimelinePane  (default)
     *   "TIMETABLE" ??TimetableMainPane
     *
     * DashboardView observes this property and animates the swap.
     */
    private final StringProperty activePanel =
            new SimpleStringProperty("TIMELINE");

    // -- Calendar state -------------------------------------------------------

    /** "MONTH" | "DAY" */
    private final StringProperty calendarActivePane =
            new SimpleStringProperty("MONTH");

    private final javafx.beans.property.ObjectProperty<java.time.LocalDate> selectedCalendarDate =
            new javafx.beans.property.SimpleObjectProperty<>(java.time.LocalDate.now());

    private final javafx.beans.property.ObjectProperty<java.time.YearMonth> calendarDisplayMonth =
            new javafx.beans.property.SimpleObjectProperty<>(java.time.YearMonth.now());

    private final javafx.collections.ObservableList<com.timeapp.domain.model.CalendarEvent> calendarDayEvents =
            javafx.collections.FXCollections.observableArrayList();

    private final javafx.beans.property.IntegerProperty calendarDataVersion =
            new javafx.beans.property.SimpleIntegerProperty(0);

    // ?? Formatters ????????????????????????????????????????????????????????????

    public static final DateTimeFormatter HEADER_FMT =
            DateTimeFormatter.ofPattern("EEE, MMM d");

    // ?? Sidebar animation wiring ??????????????????????????????????????????????

    private Pane     sidebarPane;
    private Timeline sidebarTimeline;
    private static final double SIDEBAR_WIDTH = 260;

    // ?? Persistence wiring ???????????????????????????????????????????????????

    private final ScheduleService scheduleService;
    private final Map<String, String> persistedTimetableIdsByUiId = new HashMap<>();

    // ?? Constructor ???????????????????????????????????????????????????????????

    public DashboardController() {
        System.out.println("[TimeFlow] Data file: " + DATA_FILE_PATH);
        scheduleService = new ScheduleService(
            new JsonScheduleRepository(DATA_FILE_PATH),
            new TimetableExpander(),
            new TimelineBuilder()
        );
        scheduleService.loadData();
        loadPersistedTimetables();
        // Demo seed disabled -- real data loads from schedule-data.json
        loadEntriesForDay(selectedDay.get());
        selectedDay.addListener((obs, o, n) -> loadEntriesForDay(n));
        activeTimetable.addListener((obs, o, n) -> loadEntriesForDay(selectedDay.get()));
    }

    // ?? Timeline actions ??????????????????????????????????????????????????????

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
        showAddScheduleDialog(selectedDay.get());
    }

    public void onAddTodo() {
        collapseFab();
        showAddTodoDialog();
    }

    private void showAddScheduleDialog(LocalDate defaultDate) {
        Dialog<com.timeapp.domain.model.CalendarEvent> dialog = new Dialog<>();
        dialog.setTitle("Add Schedule");

        ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        TextField titleField = new TextField();
        titleField.setPromptText("Title");
        TextField locationField = new TextField();
        locationField.setPromptText("Location");
        TextArea descriptionArea = new TextArea();
        descriptionArea.setPromptText("Description");
        descriptionArea.setPrefRowCount(3);
        DatePicker datePicker = new DatePicker(defaultDate != null ? defaultDate : selectedDay.get());
        ComboBox<LocalTime> startBox = buildTimeCombo();
        ComboBox<LocalTime> endBox = buildTimeCombo();
        startBox.setValue(LocalTime.of(9, 0));
        endBox.setValue(LocalTime.of(10, 0));

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new javafx.geometry.Insets(12));
        grid.addRow(0, new Label("Title"), titleField);
        grid.addRow(1, new Label("Date"), datePicker);
        grid.addRow(2, new Label("Start"), startBox);
        grid.addRow(3, new Label("End"), endBox);
        grid.addRow(4, new Label("Location"), locationField);
        grid.addRow(5, new Label("Description"), descriptionArea);
        GridPane.setHgrow(titleField, Priority.ALWAYS);
        GridPane.setHgrow(locationField, Priority.ALWAYS);
        GridPane.setHgrow(descriptionArea, Priority.ALWAYS);

        Node saveButton = dialog.getDialogPane().lookupButton(saveButtonType);
        Runnable updateSaveState = () -> {
            LocalTime start = startBox.getValue();
            LocalTime end = endBox.getValue();
            saveButton.setDisable(
                titleField.getText().trim().isEmpty()
                    || datePicker.getValue() == null
                    || start == null
                    || end == null
                    || !end.isAfter(start)
            );
        };
        titleField.textProperty().addListener((obs, o, n) -> updateSaveState.run());
        datePicker.valueProperty().addListener((obs, o, n) -> updateSaveState.run());
        startBox.valueProperty().addListener((obs, o, n) -> updateSaveState.run());
        endBox.valueProperty().addListener((obs, o, n) -> updateSaveState.run());
        updateSaveState.run();

        dialog.getDialogPane().setContent(grid);
        dialog.setResultConverter(button -> {
            if (button != saveButtonType) {
                return null;
            }
            LocalDate date = datePicker.getValue();
            return new com.timeapp.domain.model.CalendarEvent(
                titleField.getText().trim(),
                LocalDateTime.of(date, startBox.getValue()),
                LocalDateTime.of(date, endBox.getValue()),
                locationField.getText().trim(),
                descriptionArea.getText().trim(),
                "#4A9EFF",
                "schedule"
            );
        });

        dialog.showAndWait().ifPresent(event -> {
            scheduleService.addCalendarEvent(event);
            loadEntriesForDay(selectedDay.get());
            // Refresh calendar month grid dots and day list immediately
            refreshCalendarViews();
        });
    }

    private void showAddTodoDialog() {
        Dialog<ToDoTask> dialog = new Dialog<>();
        dialog.setTitle("Add To-Do");

        ButtonType saveType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveType, ButtonType.CANCEL);

        // Title
        TextField titleField = new TextField();
        titleField.setPromptText("Task title");

        // Date (defaults to the currently selected timeline day)
        DatePicker datePicker = new DatePicker(selectedDay.get());

        // Time slot (30-minute increments from 07:00 to 22:00)
        ComboBox<LocalTime> timeBox = buildTimeCombo();
        timeBox.setValue(LocalTime.of(9, 0));

        Node saveBtn = dialog.getDialogPane().lookupButton(saveType);
        saveBtn.setDisable(true);
        titleField.textProperty().addListener((o, p, n) ->
            saveBtn.setDisable(n.trim().isEmpty() || datePicker.getValue() == null));
        datePicker.valueProperty().addListener((o, p, n) ->
            saveBtn.setDisable(titleField.getText().trim().isEmpty() || n == null));

        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10);
        grid.setPadding(new javafx.geometry.Insets(12));
        grid.addRow(0, new Label("Title"),     titleField);
        grid.addRow(1, new Label("Date"),      datePicker);
        grid.addRow(2, new Label("Time slot"), timeBox);
        GridPane.setHgrow(titleField, Priority.ALWAYS);

        dialog.getDialogPane().setContent(grid);
        dialog.setResultConverter(btn -> {
            if (btn != saveType) return null;
            ToDoTask task = new ToDoTask(titleField.getText().trim(), false);
            LocalDate d = datePicker.getValue();
            LocalTime t = timeBox.getValue();
            if (d != null && t != null) task.setScheduled(d, t);
            return task;
        });

        dialog.showAndWait().ifPresent(task -> {
            scheduleService.addToDoTask(task);
            loadEntriesForDay(selectedDay.get());
        });
    }

    // ?? Navigation ????????????????????????????????????????????????????????????

    /**
     * Primary navigation ??called by SidebarDrawer when the user taps a nav item.
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
            case "Calendar" -> {
                activePanel.set("CALENDAR");
                calendarActivePane.set("MONTH");
            }
            default -> System.out.println("[Navigation] Unknown view: " + viewName);
        }
    }

    // ?? Timetable sub-panel routing ???????????????????????????????????????????

    /** FAB "TimeTable" ??show Panel 3. */
    // -- Calendar navigation ---------------------------------------------------

    public void showCalendarDayView(java.time.LocalDate date) {
        selectedCalendarDate.set(date);
        loadCalendarDayEvents(date);
        calendarActivePane.set("DAY");
    }

    public void backToCalendarMonth()    { calendarActivePane.set("MONTH"); }
    public void calendarNextMonth()      { calendarDisplayMonth.set(calendarDisplayMonth.get().plusMonths(1)); }
    public void calendarPrevMonth()      { calendarDisplayMonth.set(calendarDisplayMonth.get().minusMonths(1)); }
    public void calendarBackToThisMonth(){ calendarDisplayMonth.set(java.time.YearMonth.now()); if ("DAY".equals(calendarActivePane.get())) calendarActivePane.set("MONTH"); }

    public void onAddScheduleForDate(java.time.LocalDate date) {
        collapseFab();
        showAddScheduleDialog(date != null ? date : selectedDay.get());
    }

    private void loadCalendarDayEvents(java.time.LocalDate date) {
        calendarDayEvents.clear();
        if (date == null) return;
        com.timeapp.domain.model.ScheduleData data = scheduleService.getData();
        if (data == null || data.getCalendarEvents() == null) return;
        data.getCalendarEvents().stream()
            .filter(java.util.Objects::nonNull)
            .filter(e -> e.getStartTime() != null)
            .filter(e -> e.getStartTime().toLocalDate().equals(date))
            .sorted(java.util.Comparator.comparing(com.timeapp.domain.model.CalendarEvent::getStartTime))
            .forEach(calendarDayEvents::add);
    }

    public boolean hasCalendarEventsOnDate(java.time.LocalDate date) {
        if (date == null) return false;
        com.timeapp.domain.model.ScheduleData data = scheduleService.getData();
        if (data == null || data.getCalendarEvents() == null) return false;
        return data.getCalendarEvents().stream()
            .filter(java.util.Objects::nonNull)
            .filter(e -> e.getStartTime() != null)
            .anyMatch(e -> e.getStartTime().toLocalDate().equals(date));
    }

    private void refreshCalendarViews() {
        calendarDataVersion.set(calendarDataVersion.get() + 1);
        if ("CALENDAR".equals(activePanel.get()) && "DAY".equals(calendarActivePane.get())) {
            loadCalendarDayEvents(selectedCalendarDate.get());
        }
    }

        public void showTimetableList() {
        activeTimetablePane.set("LIST");
        closeSidebar();
        collapseFab();
    }

    /** FAB "Add Class" ??show Panel 2. */
    public void showAddClassPane() {
        ensureActiveTimetable();
        activeTimetablePane.set("ADD");
        closeSidebar();
        collapseFab();
    }

    /** Panel 3 mini-FAB ??show Panel 4. */
    public void showCreateTimetable() {
        activeTimetablePane.set("CREATE");
        closeSidebar();
        collapseFab();
    }

    /** Back arrow in Panel 2, 3 ??return to Panel 1. */
    public void backToTimetableMain() {
        activeTimetablePane.set("MAIN");
    }

    /** Back arrow in Panel 4 ??return to Panel 3. */
    public void backToTimetableList() {
        activeTimetablePane.set("LIST");
    }

    // ?? Timetable domain actions ??????????????????????????????????????????????

    /**
     * Saves a {@link TimetableClassRecord} into the active timetable,
     * refreshes the day's entry list, and navigates back to Panel 1.
     */
    public void saveClass(TimetableClassRecord record) {
        if (record == null) return;
        ensureActiveTimetable();
        if (!hasAnyValidTimeSlot(record)) return;
        activeTimetable.get().getClasses().add(record);
        persistClassRecord(record, activeTimetable.get());
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
        persistTimetable(timetable);
        backToTimetableList();
    }

    /** Changes the active timetable and re-renders the current day. */
    public void setActiveTimetable(TimeTable timetable) {
        activeTimetable.set(timetable);
        // activeTimetable listener fires loadEntriesForDay automatically
    }

    public void editTimetable(TimeTable timetable) {
        if (timetable == null) return;

        Dialog<Boolean> dialog = new Dialog<>();
        dialog.setTitle("Edit TimeTable");
        ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        TextField titleField = new TextField(timetable.getTitle());
        DatePicker startPicker = new DatePicker(timetable.getStartDate());
        DatePicker endPicker = new DatePicker(timetable.getEndDate());

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new javafx.geometry.Insets(12));
        grid.addRow(0, new Label("Title"), titleField);
        grid.addRow(1, new Label("SemStart"), startPicker);
        grid.addRow(2, new Label("SemEnd"), endPicker);
        GridPane.setHgrow(titleField, Priority.ALWAYS);

        Node saveButton = dialog.getDialogPane().lookupButton(saveButtonType);
        Runnable updateSaveState = () -> saveButton.setDisable(
            titleField.getText().trim().isEmpty()
                || startPicker.getValue() == null
                || endPicker.getValue() == null
                || !endPicker.getValue().isAfter(startPicker.getValue())
        );
        titleField.textProperty().addListener((obs, o, n) -> updateSaveState.run());
        startPicker.valueProperty().addListener((obs, o, n) -> updateSaveState.run());
        endPicker.valueProperty().addListener((obs, o, n) -> updateSaveState.run());
        updateSaveState.run();

        dialog.getDialogPane().setContent(grid);
        dialog.setResultConverter(button -> button == saveButtonType);
        dialog.showAndWait().filter(Boolean::booleanValue).ifPresent(saved -> {
            timetable.setTitle(titleField.getText().trim());
            timetable.setStartDate(startPicker.getValue());
            timetable.setEndDate(endPicker.getValue());

            String timetableId = ensurePersistedTimetable(timetable);
            scheduleService.updateTimetable(
                timetableId,
                timetable.getTitle(),
                timetable.getStartDate(),
                timetable.getEndDate()
            );
            rebuildPersistedClasses(timetable);
            loadEntriesForDay(selectedDay.get());
        });
    }

    public void deleteTimetable(TimeTable timetable) {
        if (timetable == null) return;
        if (!confirm("Delete TimeTable", "Delete \"" + timetable.getTitle() + "\"?")) {
            return;
        }

        String timetableId = persistedTimetableIdsByUiId.remove(timetable.getId());
        timetableList.remove(timetable);
        if (timetableId != null) {
            scheduleService.deleteTimetable(timetableId);
        }

        if (timetable.equals(activeTimetable.get())) {
            activeTimetable.set(timetableList.isEmpty() ? null : timetableList.get(0));
        }
        loadEntriesForDay(selectedDay.get());
    }

    public void editClass(TimetableClassRecord record) {
        TimeTable timetable = activeTimetable.get();
        if (record == null || timetable == null) return;

        Dialog<Boolean> dialog = new Dialog<>();
        dialog.setTitle("Edit Class");
        ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        TextField subjectField = new TextField(record.getSubject());
        TextField teacherField = new TextField(record.getTeacher());
        TextField classroomField = new TextField(record.getClassroom());
        ComboBox<TimetableClassRecord.AccentColor> colorBox = new ComboBox<>();
        colorBox.getItems().addAll(TimetableClassRecord.AccentColor.values());
        colorBox.setValue(record.getAccentColor());

        VBox slotBox = new VBox(8);
        List<ComboBox<DayOfWeek>> dayBoxes = new ArrayList<>();
        List<ComboBox<LocalTime>> startBoxes = new ArrayList<>();
        List<ComboBox<LocalTime>> endBoxes = new ArrayList<>();
        List<TimetableClassRecord.ClassTimeSlot> slots = record.getTimeSlots().isEmpty()
            ? List.of(new TimetableClassRecord.ClassTimeSlot(DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(10, 0)))
            : new ArrayList<>(record.getTimeSlots());

        for (TimetableClassRecord.ClassTimeSlot slot : slots) {
            ComboBox<DayOfWeek> dayBox = new ComboBox<>();
            dayBox.getItems().addAll(DayOfWeek.values());
            dayBox.setValue(slot.getDayOfWeek());
            ComboBox<LocalTime> startBox = buildTimeCombo();
            startBox.setValue(slot.getStartTime());
            ComboBox<LocalTime> endBox = buildTimeCombo();
            endBox.setValue(slot.getEndTime());

            dayBoxes.add(dayBox);
            startBoxes.add(startBox);
            endBoxes.add(endBox);
            slotBox.getChildren().add(new HBox(8, dayBox, startBox, new Label("-"), endBox));
        }

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new javafx.geometry.Insets(12));
        grid.addRow(0, new Label("Subject"), subjectField);
        grid.addRow(1, new Label("Teacher"), teacherField);
        grid.addRow(2, new Label("Classroom"), classroomField);
        grid.addRow(3, new Label("Color"), colorBox);
        grid.addRow(4, new Label("Times"), slotBox);
        GridPane.setHgrow(subjectField, Priority.ALWAYS);

        Node saveButton = dialog.getDialogPane().lookupButton(saveButtonType);
        Runnable updateSaveState = () -> saveButton.setDisable(
            subjectField.getText().trim().isEmpty()
                || !hasValidEditorSlots(dayBoxes, startBoxes, endBoxes)
        );
        subjectField.textProperty().addListener((obs, o, n) -> updateSaveState.run());
        for (int i = 0; i < dayBoxes.size(); i++) {
            dayBoxes.get(i).valueProperty().addListener((obs, o, n) -> updateSaveState.run());
            startBoxes.get(i).valueProperty().addListener((obs, o, n) -> updateSaveState.run());
            endBoxes.get(i).valueProperty().addListener((obs, o, n) -> updateSaveState.run());
        }
        updateSaveState.run();

        dialog.getDialogPane().setContent(grid);
        dialog.setResultConverter(button -> button == saveButtonType);
        dialog.showAndWait().filter(Boolean::booleanValue).ifPresent(saved -> {
            record.setSubject(subjectField.getText().trim());
            record.setTeacher(teacherField.getText().trim());
            record.setClassroom(classroomField.getText().trim());
            record.setAccentColor(colorBox.getValue());
            record.getTimeSlots().clear();
            for (int i = 0; i < dayBoxes.size(); i++) {
                record.addTimeSlot(new TimetableClassRecord.ClassTimeSlot(
                    dayBoxes.get(i).getValue(),
                    startBoxes.get(i).getValue(),
                    endBoxes.get(i).getValue()
                ));
            }

            rebuildPersistedClasses(timetable);
            loadEntriesForDay(selectedDay.get());
        });
    }

    public void deleteClass(TimetableClassRecord record) {
        TimeTable timetable = activeTimetable.get();
        if (record == null || timetable == null) return;
        if (!confirm("Delete Class", "Delete \"" + record.getSubject() + "\"?")) {
            return;
        }

        timetable.getClasses().remove(record);
        rebuildPersistedClasses(timetable);
        loadEntriesForDay(selectedDay.get());
    }

    // ?? Sidebar animation ?????????????????????????????????????????????????????

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

    // ?? Entry loading (date-range filtered) ???????????????????????????????????

    private void loadEntriesForDay(LocalDate date) {
        dayEntries.clear();
        if (date == null) return;
        dayEntries.addAll(buildSampleEntries(date));
    }

    private List<TimeEntry> buildSampleEntries(LocalDate date) {
        List<TimeEntry> list = new ArrayList<>();
        // Only real persisted data -- no hardcoded demo cards
        addPersistedTodos(list, date);
        addActiveTimetableClasses(list, date);
        addPersistedCalendarEvents(list, date);
        return list;
    }

    private void addPersistedTodos(List<TimeEntry> list, LocalDate date) {
        List<ToDoTask> tasks = scheduleService.getToDoTasks();
        if (tasks == null || tasks.isEmpty()) return;

        LocalTime fallback = LocalTime.of(9, 0);
        for (ToDoTask task : tasks) {
            if (task == null || task.getTitle() == null
                || task.getTitle().isBlank() || task.isCompleted()) continue;

            if (task.isScheduled()) {
                // Show only on the matching date at the scheduled time
                java.time.LocalDate taskDate = task.getScheduledLocalDate();
                if (!date.equals(taskDate)) continue;
                list.add(new TodoEntry(task.getScheduledLocalTime(), task.getTitle()));
            } else {
                // Legacy / unscheduled tasks: show on selectedDay at fallback time
                if (date.equals(selectedDay.get())) {
                    list.add(new TodoEntry(fallback, task.getTitle()));
                    fallback = fallback.plusMinutes(30);
                }
            }
        }
    }

    private void addPersistedCalendarEvents(List<TimeEntry> list, LocalDate date) {
        ScheduleData data = scheduleService.getData();
        if (data == null || data.getCalendarEvents() == null) {
            return;
        }

        for (com.timeapp.domain.model.CalendarEvent event : data.getCalendarEvents()) {
            if (event == null || event.getStartTime() == null || event.getEndTime() == null
                || !event.getStartTime().toLocalDate().equals(date)) {
                continue;
            }
            list.add(new CalendarEvent(
                event.getStartTime().toLocalTime(),
                event.getEndTime().toLocalTime(),
                nullToDefault(event.getTitle(), "Schedule"),
                nullToEmpty(event.getLocation())
            ));
        }
    }

    /**
     * Appends TimetableClass entries for today from the active timetable.
     *
     * ?? Date-range contract ???????????????????????????????????????????????????
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

    // ?? Sample seed data ??????????????????????????????????????????????????????

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

    private void loadPersistedTimetables() {
        ScheduleData data = scheduleService.getData();
        if (data == null) {
            return;
        }

        if (data.getTimetables() != null && !data.getTimetables().isEmpty()) {
            loadTimetableData(data.getTimetables());
            return;
        }

        loadLegacyTimetableEntries(data.getTimetableEntries());
    }

    private void loadTimetableData(List<TimetableData> timetables) {
        for (TimetableData saved : timetables) {
            if (saved == null) continue;

            LocalDate start = saved.getStartDate() != null
                ? saved.getStartDate()
                : LocalDate.now().withDayOfMonth(1);
            LocalDate end = saved.getEndDate() != null
                ? saved.getEndDate()
                : start.plusMonths(5).minusDays(1);

            TimeTable timetable = new TimeTable(nullToDefault(saved.getTitle(), "Saved Timetable"), start, end);
            timetableList.add(timetable);
            persistedTimetableIdsByUiId.put(timetable.getId(), saved.getId());

            if (saved.getClasses() == null) continue;
            Map<String, TimetableClassRecord> recordsByClass = new LinkedHashMap<>();
            for (TimetableEntry entry : saved.getClasses()) {
                if (!hasValidTime(entry)) {
                    continue;
                }

                String classKey = classIdentityKey(entry);
                TimetableClassRecord record = recordsByClass.computeIfAbsent(classKey, key -> {
                    TimetableClassRecord created = new TimetableClassRecord(
                        nullToEmpty(entry.getTitle()),
                        nullToEmpty(entry.getTeacher()),
                        nullToEmpty(entry.getRoom()),
                        accentColorFromHex(entry.getColor())
                    );
                    timetable.getClasses().add(created);
                    return created;
                });

                record.addTimeSlot(toUiTimeSlot(entry));
            }
        }

        if (!timetableList.isEmpty()) {
            activeTimetable.set(timetableList.get(0));
        }
    }

    private void loadLegacyTimetableEntries(List<TimetableEntry> entries) {
        if (entries == null || entries.isEmpty()) {
            return;
        }

        Map<String, TimeTable> timetablesByRange = new LinkedHashMap<>();
        Map<String, TimetableClassRecord> recordsByClass = new LinkedHashMap<>();
        boolean migratedLegacyEntries = false;

        for (TimetableEntry entry : entries) {
            if (!hasValidTime(entry)) {
                continue;
            }

            LocalDate start = entry.getSemesterStart() != null
                ? entry.getSemesterStart()
                : LocalDate.now().withDayOfMonth(1);
            LocalDate end = entry.getSemesterEnd() != null
                ? entry.getSemesterEnd()
                : start.plusMonths(5).minusDays(1);

            String rangeKey = start + "|" + end;
            TimeTable timetable = timetablesByRange.computeIfAbsent(rangeKey, key -> {
                TimeTable created = new TimeTable("Saved Timetable", start, end);
                timetableList.add(created);
                persistTimetable(created);
                return created;
            });

            String classKey = rangeKey + "|" + classIdentityKey(entry);

            TimetableClassRecord record = recordsByClass.computeIfAbsent(classKey, key -> {
                TimetableClassRecord created = new TimetableClassRecord(
                    nullToEmpty(entry.getTitle()),
                    nullToEmpty(entry.getTeacher()),
                    nullToEmpty(entry.getRoom()),
                    accentColorFromHex(entry.getColor())
                );
                timetable.getClasses().add(created);
                return created;
            });

            record.addTimeSlot(toUiTimeSlot(entry));
            scheduleService.addClassToTimetable(ensurePersistedTimetable(timetable), entry);
            migratedLegacyEntries = true;
        }

        if (migratedLegacyEntries) {
            scheduleService.getData().getTimetableEntries().clear();
            scheduleService.saveData();
        }

        if (!timetableList.isEmpty()) {
            activeTimetable.set(timetableList.get(0));
        }
    }

    private void persistClassRecord(TimetableClassRecord record, TimeTable timetable) {
        String timetableId = ensurePersistedTimetable(timetable);

        for (TimetableClassRecord.ClassTimeSlot slot : record.getTimeSlots()) {
            if (slot == null || slot.getDayOfWeek() == null
                || slot.getStartTime() == null || slot.getEndTime() == null) {
                continue;
            }

            scheduleService.addClassToTimetable(timetableId, new TimetableEntry(
                record.getSubject(),
                slot.getDayOfWeek(),
                slot.getStartTime(),
                slot.getEndTime(),
                record.getClassroom(),
                record.getTeacher(),
                record.getAccentColor() != null ? record.getAccentColor().strip : null,
                "隤脩?",
                timetable.getStartDate(),
                timetable.getEndDate()
            ));
        }
    }

    private void rebuildPersistedClasses(TimeTable timetable) {
        if (timetable == null) {
            return;
        }

        String timetableId = ensurePersistedTimetable(timetable);
        List<TimetableEntry> entries = new ArrayList<>();
        for (TimetableClassRecord record : timetable.getClasses()) {
            if (record == null || record.getTimeSlots() == null) {
                continue;
            }
            for (TimetableClassRecord.ClassTimeSlot slot : record.getTimeSlots()) {
                if (slot == null || slot.getDayOfWeek() == null
                    || slot.getStartTime() == null || slot.getEndTime() == null) {
                    continue;
                }
                entries.add(toDomainEntry(record, slot, timetable));
            }
        }
        scheduleService.replaceTimetableClasses(timetableId, entries);
    }

    private TimetableEntry toDomainEntry(TimetableClassRecord record,
                                         TimetableClassRecord.ClassTimeSlot slot,
                                         TimeTable timetable) {
        return new TimetableEntry(
            record.getSubject(),
            slot.getDayOfWeek(),
            slot.getStartTime(),
            slot.getEndTime(),
            record.getClassroom(),
            record.getTeacher(),
            record.getAccentColor() != null ? record.getAccentColor().strip : null,
            "class",
            timetable.getStartDate(),
            timetable.getEndDate()
        );
    }

    private boolean hasAnyValidTimeSlot(TimetableClassRecord record) {
        if (record.getTimeSlots() == null) {
            return false;
        }
        for (TimetableClassRecord.ClassTimeSlot slot : record.getTimeSlots()) {
            if (slot != null && slot.getDayOfWeek() != null
                && slot.getStartTime() != null && slot.getEndTime() != null) {
                return true;
            }
        }
        return false;
    }

    private boolean hasValidEditorSlots(List<ComboBox<DayOfWeek>> dayBoxes,
                                        List<ComboBox<LocalTime>> startBoxes,
                                        List<ComboBox<LocalTime>> endBoxes) {
        if (dayBoxes.isEmpty()) {
            return false;
        }
        for (int i = 0; i < dayBoxes.size(); i++) {
            LocalTime start = startBoxes.get(i).getValue();
            LocalTime end = endBoxes.get(i).getValue();
            if (dayBoxes.get(i).getValue() == null || start == null || end == null
                || !end.isAfter(start)) {
                return false;
            }
        }
        return true;
    }

    private ComboBox<LocalTime> buildTimeCombo() {
        ComboBox<LocalTime> combo = new ComboBox<>();
        for (int hour = 7; hour <= 22; hour++) {
            combo.getItems().add(LocalTime.of(hour, 0));
            if (hour < 22) {
                combo.getItems().add(LocalTime.of(hour, 30));
            }
        }
        combo.setPrefWidth(100);
        return combo;
    }

    private boolean confirm(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(message);
        alert.setContentText(null);
        return alert.showAndWait().filter(ButtonType.OK::equals).isPresent();
    }

    private void persistTimetable(TimeTable timetable) {
        if (timetable == null || persistedTimetableIdsByUiId.containsKey(timetable.getId())) {
            return;
        }

        TimetableData data = new TimetableData(
            timetable.getId(),
            timetable.getTitle(),
            timetable.getStartDate(),
            timetable.getEndDate(),
            new ArrayList<>()
        );
        scheduleService.addTimetable(data);
        persistedTimetableIdsByUiId.put(timetable.getId(), data.getId());
    }

    private String ensurePersistedTimetable(TimeTable timetable) {
        persistTimetable(timetable);
        return persistedTimetableIdsByUiId.get(timetable.getId());
    }

    private boolean hasValidTime(TimetableEntry entry) {
        return entry != null
            && entry.getDayOfWeek() != null
            && entry.getStartTime() != null
            && entry.getEndTime() != null;
    }

    private String classIdentityKey(TimetableEntry entry) {
        return nullToEmpty(entry.getTitle()) + "|"
            + nullToEmpty(entry.getTeacher()) + "|"
            + nullToEmpty(entry.getRoom()) + "|"
            + nullToEmpty(entry.getColor());
    }

    private TimetableClassRecord.ClassTimeSlot toUiTimeSlot(TimetableEntry entry) {
        return new TimetableClassRecord.ClassTimeSlot(
            entry.getDayOfWeek(),
            entry.getStartTime(),
            entry.getEndTime()
        );
    }

    private TimetableClassRecord.AccentColor accentColorFromHex(String hex) {
        if (hex != null) {
            for (TimetableClassRecord.AccentColor color : TimetableClassRecord.AccentColor.values()) {
                if (hex.equalsIgnoreCase(color.strip)) {
                    return color;
                }
            }
        }
        return TimetableClassRecord.AccentColor.PURPLE;
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private String nullToDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    // ?? Helpers ???????????????????????????????????????????????????????????????

    public static LocalDate sundayOf(LocalDate date) {
        return date.minusDays(date.getDayOfWeek().getValue() % 7);
    }

    private void ensureActiveTimetable() {
        // seed disabled
    }

    // ?? Property accessors ????????????????????????????????????????????????????

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

    // Calendar
    public javafx.beans.property.StringProperty             calendarActivePaneProperty()   { return calendarActivePane; }
    public javafx.beans.property.ObjectProperty<java.time.LocalDate>  selectedCalendarDateProperty() { return selectedCalendarDate; }
    public javafx.beans.property.ObjectProperty<java.time.YearMonth>  calendarDisplayMonthProperty() { return calendarDisplayMonth; }
    public javafx.collections.ObservableList<com.timeapp.domain.model.CalendarEvent> getCalendarDayEvents() { return calendarDayEvents; }
    public javafx.beans.property.IntegerProperty            calendarDataVersionProperty()  { return calendarDataVersion; }
    public java.time.LocalDate  getSelectedCalendarDate()  { return selectedCalendarDate.get(); }
    public java.time.YearMonth  getCalendarDisplayMonth()  { return calendarDisplayMonth.get(); }

    /**
     * Keeps schedule-data.json stable even when the app is launched from an IDE
     * or a nested folder with a different working directory.
     */
    private static String resolveDataFilePath() {
        String fileName = "schedule-data.json";
        Path workingFile = Paths.get(fileName).toAbsolutePath().normalize();
        if (Files.exists(workingFile)) {
            return workingFile.toString();
        }

        Path dir = Paths.get(System.getProperty("user.dir")).toAbsolutePath().normalize();
        while (dir != null) {
            if (Files.exists(dir.resolve("pom.xml"))) {
                return dir.resolve(fileName).toString();
            }
            dir = dir.getParent();
        }

        return workingFile.toString();
    }
}
