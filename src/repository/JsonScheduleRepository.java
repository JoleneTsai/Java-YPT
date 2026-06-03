package repository;

import model.ScheduleData;

/*
 * Skeleton only.
 *
 * Suggested JSON library:
 *   Gson:   new GsonBuilder().registerTypeAdapter(...).create()
 *   Jackson: ObjectMapper + JavaTimeModule
 *
 * Because this project uses LocalDateTime, LocalDate, LocalTime, and DayOfWeek,
 * plain JSON parsing needs a date/time adapter or manual parsing.
 */
public class JsonScheduleRepository implements ScheduleRepository {
    private final String filePath;

    public JsonScheduleRepository(String filePath) {
        this.filePath = filePath;
    }

    @Override
    public ScheduleData loadAll() {
        // TODO A: read schedule-data.json and convert it to ScheduleData.
        // Return empty data first to keep UI/service runnable.
        return new ScheduleData();
    }

    @Override
    public void saveAll(ScheduleData data) {
        // TODO A: convert ScheduleData to JSON and write it back to filePath.
    }

    public String getFilePath() {
        return filePath;
    }
}
