module javafx.com.yasbu_dekstop {
    requires javafx.controls;
    requires javafx.fxml;

    requires com.dlsc.formsfx;
    requires net.synedra.validatorfx;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.bootstrapfx.core;
    requires static lombok;
    requires com.fasterxml.jackson.datatype.jsr310;
    requires com.fasterxml.jackson.databind;
    requires java.logging;

    // Export packages yang dibutuhkan
    exports javafx.com.yasbu_dekstop;
    exports javafx.com.yasbu_dekstop.models to com.fasterxml.jackson.databind;

    // Open packages untuk JavaFX FXML
    opens javafx.com.yasbu_dekstop to javafx.fxml;
    opens javafx.com.yasbu_dekstop.controllers to javafx.fxml;

    // PENTING: Open package models untuk Jackson reflection
    opens javafx.com.yasbu_dekstop.models to com.fasterxml.jackson.databind, com.fasterxml.jackson.core;
}