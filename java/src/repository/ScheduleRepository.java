package repository;

import model.ScheduleData;

public interface ScheduleRepository {
    ScheduleData loadAll();
    void saveAll(ScheduleData data);
}
