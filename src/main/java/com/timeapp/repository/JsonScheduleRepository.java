package com.timeapp.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.timeapp.model.ScheduleData;

import java.io.File;
import java.io.IOException;

/**
 * JSON file-backed implementation of ScheduleRepository using Jackson.
 *
 * The data file path defaults to the user's home directory:
 *   ~/timeflow-data.json
 *
 * Jackson configuration:
 *   • JavaTimeModule: handles LocalDate, LocalTime, LocalDateTime.
 *   • WRITE_DATES_AS_TIMESTAMPS disabled: dates are written as ISO-8601
 *     strings (e.g. "2025-09-01") rather than numeric arrays.
 *   • @JsonTypeInfo on TimeEntry: polymorphic list entries are tagged
 *     with a "type" field so Jackson knows which subclass to instantiate.
 */
public class JsonScheduleRepository implements ScheduleRepository {

    private static final String DEFAULT_PATH =
            System.getProperty("user.home") + File.separator + "timeflow-data.json";

    private final String       filePath;
    private final ObjectMapper mapper;

    public JsonScheduleRepository() {
        this(DEFAULT_PATH);
    }

    public JsonScheduleRepository(String filePath) {
        this.filePath = filePath;
        this.mapper   = buildMapper();
    }

    @Override
    public ScheduleData loadAll() {
        File file = new File(filePath);
        if (!file.exists()) {
            System.out.println("[JsonRepo] No data file found at: " + filePath
                + " — starting with empty data.");
            return new ScheduleData();
        }
        try {
            ScheduleData data = mapper.readValue(file, ScheduleData.class);
            System.out.println("[JsonRepo] Loaded data from: " + filePath);
            return normalise(data);
        } catch (IOException e) {
            System.err.println("[JsonRepo] ERROR reading " + filePath + ": " + e.getMessage());
            return new ScheduleData();
        }
    }

    @Override
    public void saveAll(ScheduleData data) {
        try {
            // Create parent directories if they don't exist
            File file = new File(filePath);
            if (file.getParentFile() != null) file.getParentFile().mkdirs();

            mapper.writerWithDefaultPrettyPrinter().writeValue(file, data);
            System.out.println("[JsonRepo] Saved data to: " + filePath);
        } catch (IOException e) {
            System.err.println("[JsonRepo] ERROR writing " + filePath + ": " + e.getMessage());
        }
    }

    public String getFilePath() { return filePath; }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static ObjectMapper buildMapper() {
        return new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    /** Ensure no list inside ScheduleData is null (guards against bad/old JSON). */
    private static ScheduleData normalise(ScheduleData d) {
        if (d == null) return new ScheduleData();
        if (d.getSemesters()        == null) d.setSemesters(new java.util.ArrayList<>());
        if (d.getCalendarEvents()   == null) d.setCalendarEvents(new java.util.ArrayList<>());
        if (d.getTodoEntries()      == null) d.setTodoEntries(new java.util.ArrayList<>());
        if (d.getTimetableEntries() == null) d.setTimetableEntries(new java.util.ArrayList<>());
        return d;
    }
}
