package com.timeapp.ui.model;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import java.time.LocalTime;

/**
 * A simple checkbox to-do item placed on the timeline.
 */
public class TodoEntry extends TimeEntry {

    private final BooleanProperty completed = new SimpleBooleanProperty(false);
    private final com.timeapp.domain.model.ToDoTask sourceTask;

    public TodoEntry(LocalTime time, String title) {
        this(time, title, null);
    }

    public TodoEntry(LocalTime time, String title,
                     com.timeapp.domain.model.ToDoTask sourceTask) {
        // Todos occupy a 30-minute slot for layout purposes
        super(time, time.plusMinutes(30), title);
        this.sourceTask = sourceTask;
    }

    public boolean isCompleted()                    { return completed.get(); }
    public void    setCompleted(boolean v)           { completed.set(v); }
    public BooleanProperty completedProperty()      { return completed; }
    public com.timeapp.domain.model.ToDoTask getSourceTask() { return sourceTask; }

    @Override public Type getType() { return Type.TODO; }
}
