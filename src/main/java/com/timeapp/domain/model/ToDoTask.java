package com.timeapp.domain.model;

/**
 * A to-do task — the domain / persistence model.
 *
 * Plain POJO: no JavaFX imports, no LocalTime anchor.
 * To-do tasks do not implement Schedulable because they have no
 * fixed time range; they appear on the timeline at a chosen hour
 * as a convenience display choice made by the UI layer.
 *
 * The UI counterpart is {@code com.timeapp.model.TodoEntry}, which
 * wraps this data with a JavaFX BooleanProperty for checkbox binding.
 */
public class ToDoTask {

    private String  title;
    private boolean completed;

    public ToDoTask() {}

    public ToDoTask(String title, boolean completed) {
        this.title     = title;
        this.completed = completed;
    }

    public String  getTitle()                { return title; }
    public boolean isCompleted()             { return completed; }
    public void    setTitle(String title)    { this.title     = title; }
    public void    setCompleted(boolean v)   { this.completed = v; }
}
