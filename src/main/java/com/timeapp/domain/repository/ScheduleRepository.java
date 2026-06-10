package com.timeapp.domain.repository;

import com.timeapp.domain.model.ScheduleData;

/**
 * Storage abstraction for the entire schedule dataset.
 *
 * Implementations decide WHERE data lives (JSON file, SQLite, remote API …).
 * The service layer depends only on this interface, keeping persistence
 * details out of business logic.
 */
public interface ScheduleRepository {
    /** Load the full schedule from storage. Never returns null. */
    ScheduleData loadAll();

    /** Persist the full schedule to storage. */
    void saveAll(ScheduleData data);
}
