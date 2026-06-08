package com.timeapp.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import java.time.LocalDateTime;

/**
 * A to-do item placed on the timeline at a specific date + time.
 *
 * ── Unified model design ──────────────────────────────────────────────────────
 * The single class serves BOTH roles:
 *
 *   Persistence: Jackson serializes the plain boolean `completed` field.
 *     The BooleanProperty is annotated @JsonIgnore so Jackson never touches it.
 *
 *   UI binding: JavaFX CheckBox binds to `completedProperty()`.
 *     A listener keeps the plain boolean in sync whenever the property changes,
 *     so the next saveAll() call persists the correct value.
 *
 * startTime = the exact date+time the task sits on the timeline.
 * endTime   = startTime + 30 minutes (visual slot height only, not persisted
 *             as a meaningful deadline — use a CalendarEvent for that).
 * ─────────────────────────────────────────────────────────────────────────────
 */
public class TodoEntry extends TimeEntry {

    /** Persisted by Jackson. */
    private boolean completed;

    /**
     * JavaFX binding target. @JsonIgnore prevents Jackson from touching it.
     * Initialised lazily by completedProperty() so the no-arg Jackson
     * constructor doesn't need to create it.
     */
    @JsonIgnore
    private transient BooleanProperty completedProperty;

    /** No-arg constructor for Jackson deserialization. */
    public TodoEntry() {}

    /** Constructor used by the UI (quick creation without date). */
    public TodoEntry(LocalDateTime time, String title) {
        super(time, time.plusMinutes(30), title);
        this.completed = false;
    }

    /** Full constructor used by Jackson (@JsonCreator). */
    @JsonCreator
    public TodoEntry(
            @JsonProperty("startTime") LocalDateTime startTime,
            @JsonProperty("endTime")   LocalDateTime endTime,
            @JsonProperty("title")     String title,
            @JsonProperty("completed") boolean completed) {
        super(startTime, endTime, title);
        this.completed = completed;
    }

    // ── Plain boolean accessors (Jackson + service layer) ─────────────────────

    public boolean isCompleted()           { return completed; }
    public void    setCompleted(boolean v) {
        this.completed = v;
        // Keep the JavaFX property in sync if it has been initialised
        if (completedProperty != null) completedProperty.set(v);
    }

    // ── JavaFX property accessor (UI layer only) ──────────────────────────────

    /** Lazily initialised so the Jackson no-arg constructor stays fast. */
    public BooleanProperty completedProperty() {
        if (completedProperty == null) {
            completedProperty = new SimpleBooleanProperty(completed);
            // Sync: property → plain boolean (for future saves)
            completedProperty.addListener((obs, o, n) -> this.completed = n);
        }
        return completedProperty;
    }

    @Override public EventType getType() { return EventType.TODO; }
}
