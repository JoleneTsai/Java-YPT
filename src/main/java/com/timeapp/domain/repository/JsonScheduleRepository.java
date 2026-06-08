package com.timeapp.domain.repository;

import com.timeapp.domain.model.ScheduleData;

/**
 * JSON file-backed implementation of {@link ScheduleRepository}.
 *
 * Current status: skeleton — loadAll() returns empty data so the
 * rest of the application runs without a real file present.
 *
 * ── TODO: implement JSON serialisation ───────────────────────────────────────
 * Recommended library: Jackson with JavaTimeModule
 *
 *   <dependency>
 *     <groupId>com.fasterxml.jackson.core</groupId>
 *     <artifactId>jackson-databind</artifactId>
 *     <version>2.17.0</version>
 *   </dependency>
 *   <dependency>
 *     <groupId>com.fasterxml.jackson.datatype</groupId>
 *     <artifactId>jackson-datatype-jsr310</artifactId>
 *     <version>2.17.0</version>
 *   </dependency>
 *
 * Minimal wiring:
 *   ObjectMapper mapper = new ObjectMapper()
 *       .registerModule(new JavaTimeModule())
 *       .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
 *
 *   // loadAll:
 *   return mapper.readValue(new File(filePath), ScheduleData.class);
 *
 *   // saveAll:
 *   mapper.writerWithDefaultPrettyPrinter().writeValue(new File(filePath), data);
 * ─────────────────────────────────────────────────────────────────────────────
 */
public class JsonScheduleRepository implements ScheduleRepository {

    private final String filePath;

    /**
     * @param filePath absolute or relative path to the JSON data file,
     *                 e.g. {@code System.getProperty("user.home") + "/timeflow-data.json"}
     */
    public JsonScheduleRepository(String filePath) {
        this.filePath = filePath;
    }

    /**
     * {@inheritDoc}
     *
     * Returns empty {@link ScheduleData} until JSON parsing is implemented.
     * The service layer normalises null lists, so this is safe.
     */
    @Override
    public ScheduleData loadAll() {
        // TODO: read filePath, deserialise with Jackson/Gson, return result
        return new ScheduleData();
    }

    /**
     * {@inheritDoc}
     *
     * No-op until JSON serialisation is implemented.
     */
    @Override
    public void saveAll(ScheduleData data) {
        // TODO: serialise data to JSON and write to filePath
    }

    public String getFilePath() { return filePath; }
}
