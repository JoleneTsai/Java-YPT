package com.timeapp.domain.model;

import java.time.LocalDateTime;

/**
 * Contract that every schedulable event must fulfil.
 *
 * Uses LocalDateTime (date + time) so the business layer can work
 * with concrete dates — distinct from the UI-layer TimeEntry hierarchy
 * which uses LocalTime only (date is carried by the selected day in
 * DashboardController).
 */
public interface Schedulable {
    LocalDateTime getStartTime();
    LocalDateTime getEndTime();
    String        getTitle();
    EventType     getType();
}
