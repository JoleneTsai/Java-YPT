package model;

import java.util.ArrayList;
import java.util.List;

public class ScheduleData {
    private List<CalendarEvent> calendarEvents = new ArrayList<>();
    private List<ToDoTask> todoTasks = new ArrayList<>();
    private List<TimetableEntry> timetableEntries = new ArrayList<>();

    public ScheduleData() {
    }

    public ScheduleData(List<CalendarEvent> calendarEvents,
                        List<ToDoTask> todoTasks,
                        List<TimetableEntry> timetableEntries) {
        this.calendarEvents = calendarEvents;
        this.todoTasks = todoTasks;
        this.timetableEntries = timetableEntries;
    }

    public List<CalendarEvent> getCalendarEvents() {
        return calendarEvents;
    }

    public List<ToDoTask> getTodoTasks() {
        return todoTasks;
    }

    public List<TimetableEntry> getTimetableEntries() {
        return timetableEntries;
    }

    public void setCalendarEvents(List<CalendarEvent> calendarEvents) {
        this.calendarEvents = calendarEvents;
    }

    public void setTodoTasks(List<ToDoTask> todoTasks) {
        this.todoTasks = todoTasks;
    }

    public void setTimetableEntries(List<TimetableEntry> timetableEntries) {
        this.timetableEntries = timetableEntries;
    }
}
