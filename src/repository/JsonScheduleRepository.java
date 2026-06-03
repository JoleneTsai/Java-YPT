package repository;

import model.CalendarEvent;
import model.ScheduleData;
import model.TimetableEntry;
import model.ToDoTask;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class JsonScheduleRepository implements ScheduleRepository {
    private final String filePath;

    public JsonScheduleRepository(String filePath) {
        this.filePath = filePath;
    }

    @Override
    public ScheduleData loadAll() {
        Path path = Paths.get(filePath);
        if (!Files.exists(path)) {
            return new ScheduleData();
        }

        try {
            String json = Files.readString(path);
            if (json.trim().isEmpty()) {
                return new ScheduleData();
            }

            Object parsed = new JsonParser(json).parse();
            if (!(parsed instanceof Map)) {
                throw new IllegalStateException("Schedule data root must be a JSON object.");
            }

            Map<String, Object> root = castObject(parsed);
            return new ScheduleData(
                    parseCalendarEvents(root.get("calendarEvents")),
                    parseToDoTasks(root.get("todoTasks")),
                    parseTimetableEntries(root.get("timetableEntries"))
            );
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read schedule data from " + filePath, e);
        }
    }

    @Override
    public void saveAll(ScheduleData data) {
        Path path = Paths.get(filePath);
        Path parent = path.getParent();

        try {
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.writeString(path, toJson(data == null ? new ScheduleData() : data));
        } catch (IOException e) {
            throw new IllegalStateException("Failed to write schedule data to " + filePath, e);
        }
    }

    public String getFilePath() {
        return filePath;
    }

    private List<CalendarEvent> parseCalendarEvents(Object value) {
        List<CalendarEvent> events = new ArrayList<>();
        for (Object item : castArray(value)) {
            Map<String, Object> object = castObject(item);
            events.add(new CalendarEvent(
                    getString(object, "title"),
                    parseLocalDateTime(getString(object, "startTime")),
                    parseLocalDateTime(getString(object, "endTime")),
                    getString(object, "location"),
                    getString(object, "description"),
                    getString(object, "color"),
                    getString(object, "tag")
            ));
        }
        return events;
    }

    private List<ToDoTask> parseToDoTasks(Object value) {
        List<ToDoTask> tasks = new ArrayList<>();
        for (Object item : castArray(value)) {
            Map<String, Object> object = castObject(item);
            tasks.add(new ToDoTask(
                    getString(object, "title"),
                    getBoolean(object, "completed")
            ));
        }
        return tasks;
    }

    private List<TimetableEntry> parseTimetableEntries(Object value) {
        List<TimetableEntry> entries = new ArrayList<>();
        for (Object item : castArray(value)) {
            Map<String, Object> object = castObject(item);
            entries.add(new TimetableEntry(
                    getString(object, "title"),
                    parseDayOfWeek(getString(object, "dayOfWeek")),
                    parseLocalTime(getString(object, "startTime")),
                    parseLocalTime(getString(object, "endTime")),
                    getString(object, "room"),
                    getString(object, "teacher"),
                    getString(object, "color"),
                    getString(object, "tag"),
                    parseLocalDate(getString(object, "semesterStart")),
                    parseLocalDate(getString(object, "semesterEnd"))
            ));
        }
        return entries;
    }

    private String toJson(ScheduleData data) {
        StringBuilder json = new StringBuilder();
        json.append("{\n");
        appendCalendarEvents(json, nullToEmpty(data.getCalendarEvents()));
        json.append(",\n");
        appendToDoTasks(json, nullToEmpty(data.getTodoTasks()));
        json.append(",\n");
        appendTimetableEntries(json, nullToEmpty(data.getTimetableEntries()));
        json.append("\n}\n");
        return json.toString();
    }

    private void appendCalendarEvents(StringBuilder json, List<CalendarEvent> events) {
        json.append("  \"calendarEvents\": [");
        for (int i = 0; i < events.size(); i++) {
            CalendarEvent event = events.get(i);
            appendObjectStart(json, i);
            appendStringField(json, "title", event.getTitle(), true);
            appendStringField(json, "startTime", format(event.getStartTime()), true);
            appendStringField(json, "endTime", format(event.getEndTime()), true);
            appendStringField(json, "location", event.getLocation(), true);
            appendStringField(json, "description", event.getDescription(), true);
            appendStringField(json, "color", event.getColor(), true);
            appendStringField(json, "tag", event.getTag(), false);
            appendObjectEnd(json, i, events.size());
        }
        if (!events.isEmpty()) {
            json.append("\n  ");
        }
        json.append("]");
    }

    private void appendToDoTasks(StringBuilder json, List<ToDoTask> tasks) {
        json.append("  \"todoTasks\": [");
        for (int i = 0; i < tasks.size(); i++) {
            ToDoTask task = tasks.get(i);
            appendObjectStart(json, i);
            appendStringField(json, "title", task.getTitle(), true);
            appendBooleanField(json, "completed", task.isCompleted(), false);
            appendObjectEnd(json, i, tasks.size());
        }
        if (!tasks.isEmpty()) {
            json.append("\n  ");
        }
        json.append("]");
    }

    private void appendTimetableEntries(StringBuilder json, List<TimetableEntry> entries) {
        json.append("  \"timetableEntries\": [");
        for (int i = 0; i < entries.size(); i++) {
            TimetableEntry entry = entries.get(i);
            appendObjectStart(json, i);
            appendStringField(json, "title", entry.getTitle(), true);
            appendStringField(json, "dayOfWeek", format(entry.getDayOfWeek()), true);
            appendStringField(json, "startTime", format(entry.getStartTime()), true);
            appendStringField(json, "endTime", format(entry.getEndTime()), true);
            appendStringField(json, "room", entry.getRoom(), true);
            appendStringField(json, "teacher", entry.getTeacher(), true);
            appendStringField(json, "color", entry.getColor(), true);
            appendStringField(json, "tag", entry.getTag(), true);
            appendStringField(json, "semesterStart", format(entry.getSemesterStart()), true);
            appendStringField(json, "semesterEnd", format(entry.getSemesterEnd()), false);
            appendObjectEnd(json, i, entries.size());
        }
        if (!entries.isEmpty()) {
            json.append("\n  ");
        }
        json.append("]");
    }

    private void appendObjectStart(StringBuilder json, int index) {
        if (index == 0) {
            json.append("\n");
        }
        json.append("    {\n");
    }

    private void appendObjectEnd(StringBuilder json, int index, int size) {
        json.append("\n    }");
        if (index < size - 1) {
            json.append(",");
        }
        json.append("\n");
    }

    private void appendStringField(StringBuilder json, String name, String value, boolean comma) {
        json.append("      \"").append(name).append("\": ");
        if (value == null) {
            json.append("null");
        } else {
            json.append("\"").append(escape(value)).append("\"");
        }
        if (comma) {
            json.append(",");
        }
        json.append("\n");
    }

    private void appendBooleanField(StringBuilder json, String name, boolean value, boolean comma) {
        json.append("      \"").append(name).append("\": ").append(value);
        if (comma) {
            json.append(",");
        }
        json.append("\n");
    }

    private String escape(String value) {
        StringBuilder escaped = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '"':
                    escaped.append("\\\"");
                    break;
                case '\\':
                    escaped.append("\\\\");
                    break;
                case '\b':
                    escaped.append("\\b");
                    break;
                case '\f':
                    escaped.append("\\f");
                    break;
                case '\n':
                    escaped.append("\\n");
                    break;
                case '\r':
                    escaped.append("\\r");
                    break;
                case '\t':
                    escaped.append("\\t");
                    break;
                default:
                    if (c < 0x20) {
                        escaped.append(String.format("\\u%04x", (int) c));
                    } else {
                        escaped.append(c);
                    }
            }
        }
        return escaped.toString();
    }

    private String getString(Map<String, Object> object, String key) {
        Object value = object.get(key);
        if (value == null) {
            return null;
        }
        if (!(value instanceof String)) {
            throw new IllegalStateException(key + " must be a string.");
        }
        return (String) value;
    }

    private boolean getBoolean(Map<String, Object> object, String key) {
        Object value = object.get(key);
        if (value == null) {
            return false;
        }
        if (!(value instanceof Boolean)) {
            throw new IllegalStateException(key + " must be a boolean.");
        }
        return (Boolean) value;
    }

    private LocalDateTime parseLocalDateTime(String value) {
        return value == null || value.isEmpty() ? null : LocalDateTime.parse(value);
    }

    private LocalDate parseLocalDate(String value) {
        return value == null || value.isEmpty() ? null : LocalDate.parse(value);
    }

    private LocalTime parseLocalTime(String value) {
        return value == null || value.isEmpty() ? null : LocalTime.parse(value);
    }

    private DayOfWeek parseDayOfWeek(String value) {
        return value == null || value.isEmpty() ? null : DayOfWeek.valueOf(value);
    }

    private String format(LocalDateTime value) {
        return value == null ? null : value.toString();
    }

    private String format(LocalDate value) {
        return value == null ? null : value.toString();
    }

    private String format(LocalTime value) {
        return value == null ? null : value.toString();
    }

    private String format(DayOfWeek value) {
        return value == null ? null : value.name();
    }

    private <T> List<T> nullToEmpty(List<T> list) {
        return list == null ? Collections.emptyList() : list;
    }

    private List<Object> castArray(Object value) {
        if (value == null) {
            return Collections.emptyList();
        }
        if (!(value instanceof List<?>)) {
            throw new IllegalStateException("Expected a JSON array.");
        }

        List<Object> result = new ArrayList<>();
        for (Object item : (List<?>) value) {
            result.add(item);
        }
        return result;
    }

    private static Map<String, Object> castObject(Object value) {
        if (!(value instanceof Map<?, ?>)) {
            throw new IllegalStateException("Expected a JSON object.");
        }

        java.util.LinkedHashMap<String, Object> result = new java.util.LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : ((Map<?, ?>) value).entrySet()) {
            if (!(entry.getKey() instanceof String)) {
                throw new IllegalStateException("JSON object keys must be strings.");
            }
            result.put((String) entry.getKey(), entry.getValue());
        }
        return result;
    }

    private static class JsonParser {
        private final String json;
        private int index;

        JsonParser(String json) {
            this.json = json;
        }

        Object parse() {
            Object value = parseValue();
            skipWhitespace();
            if (!isAtEnd()) {
                throw error("Unexpected trailing content.");
            }
            return value;
        }

        private Object parseValue() {
            skipWhitespace();
            if (isAtEnd()) {
                throw error("Unexpected end of JSON.");
            }

            char c = json.charAt(index);
            if (c == '{') {
                return parseObject();
            }
            if (c == '[') {
                return parseArray();
            }
            if (c == '"') {
                return parseString();
            }
            if (startsWith("true")) {
                index += 4;
                return Boolean.TRUE;
            }
            if (startsWith("false")) {
                index += 5;
                return Boolean.FALSE;
            }
            if (startsWith("null")) {
                index += 4;
                return null;
            }
            throw error("Unsupported JSON value.");
        }

        private Map<String, Object> parseObject() {
            java.util.LinkedHashMap<String, Object> object = new java.util.LinkedHashMap<>();
            expect('{');
            skipWhitespace();
            if (consume('}')) {
                return object;
            }

            do {
                skipWhitespace();
                String key = parseString();
                skipWhitespace();
                expect(':');
                object.put(key, parseValue());
                skipWhitespace();
            } while (consume(','));

            expect('}');
            return object;
        }

        private List<Object> parseArray() {
            List<Object> array = new ArrayList<>();
            expect('[');
            skipWhitespace();
            if (consume(']')) {
                return array;
            }

            do {
                array.add(parseValue());
                skipWhitespace();
            } while (consume(','));

            expect(']');
            return array;
        }

        private String parseString() {
            expect('"');
            StringBuilder value = new StringBuilder();
            while (!isAtEnd()) {
                char c = json.charAt(index++);
                if (c == '"') {
                    return value.toString();
                }
                if (c == '\\') {
                    value.append(parseEscape());
                } else {
                    value.append(c);
                }
            }
            throw error("Unterminated string.");
        }

        private char parseEscape() {
            if (isAtEnd()) {
                throw error("Unterminated escape sequence.");
            }

            char escaped = json.charAt(index++);
            switch (escaped) {
                case '"':
                case '\\':
                case '/':
                    return escaped;
                case 'b':
                    return '\b';
                case 'f':
                    return '\f';
                case 'n':
                    return '\n';
                case 'r':
                    return '\r';
                case 't':
                    return '\t';
                case 'u':
                    return parseUnicodeEscape();
                default:
                    throw error("Invalid escape sequence.");
            }
        }

        private char parseUnicodeEscape() {
            if (index + 4 > json.length()) {
                throw error("Invalid unicode escape.");
            }

            String hex = json.substring(index, index + 4);
            index += 4;
            try {
                return (char) Integer.parseInt(hex, 16);
            } catch (NumberFormatException e) {
                throw error("Invalid unicode escape.");
            }
        }

        private void skipWhitespace() {
            while (!isAtEnd() && Character.isWhitespace(json.charAt(index))) {
                index++;
            }
        }

        private boolean consume(char expected) {
            if (!isAtEnd() && json.charAt(index) == expected) {
                index++;
                return true;
            }
            return false;
        }

        private void expect(char expected) {
            if (!consume(expected)) {
                throw error("Expected '" + expected + "'.");
            }
        }

        private boolean startsWith(String value) {
            return json.startsWith(value, index);
        }

        private boolean isAtEnd() {
            return index >= json.length();
        }

        private IllegalStateException error(String message) {
            return new IllegalStateException(message + " Position: " + index);
        }
    }
}
