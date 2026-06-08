package com.timeapp.repository;

import com.timeapp.model.ScheduleData;

/**
 * Storage abstraction for the entire schedule dataset.
 * Implementations decide WHERE data lives (JSON file, SQLite, cloud …).
 */
public interface ScheduleRepository {
    /** Load from storage. Must never return null. */
    ScheduleData loadAll();
    /** Persist to storage. */
    void saveAll(ScheduleData data);
}
