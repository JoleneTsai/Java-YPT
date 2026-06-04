package com.timeapp.model;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import java.time.LocalTime;

/**
 * A simple checkbox to-do item placed on the timeline.
 */
public class TodoEntry extends TimeEntry {

    private final BooleanProperty completed = new SimpleBooleanProperty(false);

    public TodoEntry(LocalTime time, String title) {
        // Todos occupy a 30-minute slot for layout purposes
        super(time, time.plusMinutes(30), title);
    }

    public boolean isCompleted()                    { return completed.get(); }
    public void    setCompleted(boolean v)           { completed.set(v); }
    public BooleanProperty completedProperty()      { return completed; }

    @Override public Type getType() { return Type.TODO; }
}
