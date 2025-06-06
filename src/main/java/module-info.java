module javafx.com.yasbu_dekstop {
    requires javafx.controls;
    requires javafx.fxml;

    requires com.dlsc.formsfx;
    requires net.synedra.validatorfx;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.bootstrapfx.core;

    opens javafx.com.yasbu_dekstop to javafx.fxml;
    exports javafx.com.yasbu_dekstop;
}