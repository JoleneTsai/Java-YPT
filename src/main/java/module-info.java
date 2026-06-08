module com.timeapp {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics;
    requires javafx.base;

    // ── JavaFX UI packages ────────────────────────────────────────────────────
    opens com.timeapp                to javafx.fxml;
    opens com.timeapp.controller     to javafx.fxml;
    opens com.timeapp.model          to javafx.fxml;
    opens com.timeapp.view           to javafx.fxml;
    opens com.timeapp.view.timetable to javafx.fxml;

    exports com.timeapp;
    exports com.timeapp.controller;
    exports com.timeapp.model;
    exports com.timeapp.view;
    exports com.timeapp.view.timetable;

    // ── Domain / business layer packages (pure Java — no JavaFX) ─────────────
    exports com.timeapp.domain.model;
    exports com.timeapp.domain.repository;
    exports com.timeapp.domain.service;
}
