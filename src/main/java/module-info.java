module com.timeapp {
    // JavaFX
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics;
    requires javafx.base;

    // Jackson core + JSR-310 (LocalDate/LocalDateTime support)
    requires com.fasterxml.jackson.databind;
    requires com.fasterxml.jackson.datatype.jsr310;

    // ── JavaFX UI packages ────────────────────────────────────────────────────
    opens com.timeapp                    to javafx.fxml;
    opens com.timeapp.controller         to javafx.fxml;
    opens com.timeapp.model              to javafx.fxml, com.fasterxml.jackson.databind;
    opens com.timeapp.view               to javafx.fxml;
    opens com.timeapp.view.timetable     to javafx.fxml;

    // Jackson needs reflective access to repository/service POJOs at runtime
    opens com.timeapp.repository         to com.fasterxml.jackson.databind;
    opens com.timeapp.service            to com.fasterxml.jackson.databind;

    exports com.timeapp;
    exports com.timeapp.controller;
    exports com.timeapp.model;
    exports com.timeapp.repository;
    exports com.timeapp.service;
    exports com.timeapp.view;
    exports com.timeapp.view.timetable;
}
