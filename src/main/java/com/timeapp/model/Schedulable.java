package com.timeapp.model;

import java.time.LocalDateTime;

/**
 * Contract shared by every schedulable item in the unified timeline.
 * Uses LocalDateTime so the business layer carries full date + time
 * without relying on a separate "selected day" context variable.
 */
public interface Schedulable {
    LocalDateTime getStartTime();
    LocalDateTime getEndTime();
    String        getTitle();
    EventType     getType();
}
