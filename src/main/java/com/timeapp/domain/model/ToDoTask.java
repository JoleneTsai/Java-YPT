package com.timeapp.domain.model;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * A to-do task ??the domain / persistence model.
 *
 * Plain POJO: no JavaFX imports. To-do tasks do not implement Schedulable
 * because they have no end time; the UI places them on the timeline using
 * their date and startTime.
 *
 * The UI counterpart is {@code com.timeapp.ui.model.TodoEntry}, which
 * wraps this data with a JavaFX BooleanProperty for checkbox binding.
 */
public class ToDoTask {

    private String  title;
    private boolean completed;
    private LocalDate date;
    private LocalTime startTime;

    public ToDoTask() {}

    public ToDoTask(String title, boolean completed) {
        this(title, completed, null, null);
    }

    public ToDoTask(String title, boolean completed, LocalDate date, LocalTime startTime) {
        this.title     = title;
        this.completed = completed;
        this.date      = date;
        this.startTime = startTime;
    }

    public String  getTitle()                { return title; }
    public boolean isCompleted()             { return completed; }
    public LocalDate getDate()               { return date; }
    public LocalTime getStartTime()          { return startTime; }
    public void    setTitle(String title)    { this.title     = title; }
    public void    setCompleted(boolean v)   { this.completed = v; }
    public void    setDate(LocalDate date)   { this.date      = date; }
    public void    setStartTime(LocalTime t) { this.startTime = t; }
}
