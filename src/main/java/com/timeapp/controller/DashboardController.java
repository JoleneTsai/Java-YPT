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

    /** Bumped whenever persisted data changes so views can refresh summaries. */
    private final IntegerProperty dataRevision =
            new SimpleIntegerProperty(0);

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

    /** Current timeline sub-panel: "MAIN", "ADD_SCHEDULE", or "ADD_TODO". */
    private final StringProperty activeTimelinePane =
            new SimpleStringProperty("MAIN");

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
        showAddSchedulePane();
    }

    public void onAddTodo() {
        collapseFab();
        showAddTodoPane();
    }

    public void showAddSchedulePane() {
        activeTimelinePane.set("ADD_SCHEDULE");
        closeSidebar();
        collapseFab();
    }

    public void showAddTodoPane() {
        activeTimelinePane.set("ADD_TODO");
        closeSidebar();
        collapseFab();
    }

    public void backToTimelineMain() {
        activeTimelinePane.set("MAIN");
    }

    public void saveCalendarEvent(com.timeapp.domain.model.CalendarEvent event) {
        if (event == null) {
            return;
        }
        scheduleService.addCalendarEvent(event);
        dataRevision.set(dataRevision.get() + 1);
        if (event.getStartTime() != null) {
            selectedDay.set(event.getStartTime().toLocalDate());
            weekStart.set(sundayOf(event.getStartTime().toLocalDate()));
        }
        loadEntriesForDay(selectedDay.get());
        backToTimelineMain();
    }

    public void saveTodoTask(String title, LocalDate date, LocalTime startTime) {
        if (title == null || title.trim().isEmpty()) {
            return;
        }
        scheduleService.addToDoTask(new ToDoTask(title.trim(), false, date, startTime));
        dataRevision.set(dataRevision.get() + 1);
        if (date != null) {
            selectedDay.set(date);
            weekStart.set(sundayOf(date));
        }
        loadEntriesForDay(selectedDay.get());
        backToTimelineMain();
    }

    private void showAddScheduleDialog() {
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
        DatePicker datePicker = new DatePicker(selectedDay.get());
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
        });
    }

    private void showAddTodoDialog() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Add To-Do");
        dialog.setHeaderText("Add To-Do");
        dialog.setContentText("Title");

        dialog.showAndWait()
            .map(String::trim)
            .filter(title -> !title.isEmpty())
            .ifPresent(title -> {
                scheduleService.addToDoTask(new ToDoTask(title, false));
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
            case "TimeLine"  -> {
                activePanel.set("TIMELINE");
                activeTimelinePane.set("MAIN");
            }
            case "Calendar"  -> {
                activePanel.set("CALENDAR");
                activeTimelinePane.set("MAIN");
            }
            default -> System.out.println("[Navigation] Unknown view: " + viewName);
        }
    }

    // ?? Timetable sub-panel routing ???????????????????????????????????????????

    /** FAB "TimeTable" ??show Panel 3. */
    public void showTimetableList() {
        activeTimetablePane.set("LIST");
        closeSidebar();
        collapseFab();
    }

    /** FAB "Add Class" ??show Panel 2. */
    public void showAddClassPane() {
        if (activeTimetable.get() == null) {
            showCreateTimetable();
            return;
        }
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
        if (activeTimetable.get() == null) return;
        if (!hasAnyValidTimeSlot(record)) return;
        activeTimetable.get().getClasses().add(record);
        persistClassRecord(record, activeTimetable.get());
        dataRevision.set(dataRevision.get() + 1);
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
        dataRevision.set(dataRevision.get() + 1);
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
            dataRevision.set(dataRevision.get() + 1);
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
        dataRevision.set(dataRevision.get() + 1);

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
            dataRevision.set(dataRevision.get() + 1);
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
        dataRevision.set(dataRevision.get() + 1);
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
        dayEntries.addAll(buildEntriesForDate(date));
    }

    private List<TimeEntry> buildEntriesForDate(LocalDate date) {
        List<TimeEntry> list = new ArrayList<>();
        addPersistedTodos(list, date);
        addActiveTimetableClasses(list, date);
        addPersistedCalendarEvents(list, date);
        return list;
    }

    private void addPersistedTodos(List<TimeEntry> list, LocalDate date) {
        List<ToDoTask> tasks = scheduleService.getToDoTasks();
        if (tasks == null || tasks.isEmpty()) {
            return;
        }

        for (ToDoTask task : tasks) {
            if (task == null || task.getTitle() == null || task.getTitle().isBlank()
                || task.isCompleted()
                || task.getDate() == null
                || task.getStartTime() == null
                || !task.getDate().equals(date)) {
                continue;
            }
            list.add(new TodoEntry(task.getStartTime(), task.getTitle()));
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

    public List<ToDoTask> getIncompleteTodosOnDate(LocalDate date) {
        List<ToDoTask> result = new ArrayList<>();
        if (date == null || scheduleService.getToDoTasks() == null) {
            return result;
        }

        for (ToDoTask task : scheduleService.getToDoTasks()) {
            if (task == null || task.isCompleted()
                || task.getDate() == null || task.getStartTime() == null
                || !task.getDate().equals(date)) {
                continue;
            }
            result.add(task);
        }
        result.sort(Comparator.comparing(ToDoTask::getStartTime));
        return result;
    }

    public Set<LocalDate> getIncompleteTodoDates() {
        Set<LocalDate> dates = new HashSet<>();
        List<ToDoTask> tasks = scheduleService.getToDoTasks();
        if (tasks == null) {
            return dates;
        }

        for (ToDoTask task : tasks) {
            if (task != null && !task.isCompleted() && task.getDate() != null) {
                dates.add(task.getDate());
            }
        }
        return dates;
    }

    public List<com.timeapp.domain.model.CalendarEvent> getCalendarEventsOnDate(LocalDate date) {
        List<com.timeapp.domain.model.CalendarEvent> result = new ArrayList<>();
        ScheduleData data = scheduleService.getData();
        if (date == null || data == null || data.getCalendarEvents() == null) {
            return result;
        }

        for (com.timeapp.domain.model.CalendarEvent event : data.getCalendarEvents()) {
            if (event == null || event.getStartTime() == null
                || !event.getStartTime().toLocalDate().equals(date)) {
                continue;
            }
            result.add(event);
        }
        result.sort(Comparator.comparing(com.timeapp.domain.model.CalendarEvent::getStartTime));
        return result;
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
        // Intentionally empty: a fresh install should start with no demo data.
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
        // No automatic demo timetable: a new install should open empty.
    }

    // ?? Property accessors ????????????????????????????????????????????????????

    public ObjectProperty<LocalDate>    weekStartProperty()          { return weekStart; }
    public ObjectProperty<LocalDate>    selectedDayProperty()        { return selectedDay; }
    public BooleanProperty              sidebarOpenProperty()        { return sidebarOpen; }
    public BooleanProperty              fabExpandedProperty()        { return fabExpanded; }
    public IntegerProperty              dataRevisionProperty()       { return dataRevision; }
    public ObservableList<TimeEntry>    getDayEntries()              { return dayEntries; }
    public ObservableList<TimeTable>    getTimetableList()           { return timetableList; }
    public ObjectProperty<TimeTable>    activeTimetableProperty()    { return activeTimetable; }
    public StringProperty               activeTimetablePaneProperty(){ return activeTimetablePane; }
    public StringProperty               activePanelProperty()        { return activePanel; }
    public StringProperty               activeTimelinePaneProperty() { return activeTimelinePane; }

    public LocalDate  getWeekStart()         { return weekStart.get(); }
    public LocalDate  getSelectedDay()       { return selectedDay.get(); }
    public boolean    isSidebarOpen()        { return sidebarOpen.get(); }
    public boolean    isFabExpanded()        { return fabExpanded.get(); }
    public int        getDataRevision()      { return dataRevision.get(); }
    public TimeTable  getActiveTimetable()   { return activeTimetable.get(); }
    public String     getActivePanel()       { return activePanel.get(); }
    public String     getActiveTimelinePane(){ return activeTimelinePane.get(); }

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
