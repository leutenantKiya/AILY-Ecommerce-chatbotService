module com.aily {
    requires javafx.controls;
    requires javafx.fxml;
    requires transitive com.google.gson;
    requires java.net.http;
    requires java.logging;
    requires java.desktop;

    opens com.aily to javafx.fxml;
    opens com.aily.controller to javafx.fxml;
    opens com.aily.model to com.google.gson;

    exports com.aily;
    exports com.aily.controller;
    exports com.aily.model;
    exports com.aily.service;
}
