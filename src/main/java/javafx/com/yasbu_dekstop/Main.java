package javafx.com.yasbu_dekstop;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {
    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        // Load FXML login view
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/javafx/com/yasbu_dekstop/login-view.fxml"));

        Parent root = loader.load();

        Scene scene = new Scene(root, 600, 400);
        primaryStage.setScene(scene);
        primaryStage.setTitle("Yasbu Desktop - Login");
        primaryStage.show();
    }
}
//ZA4]P@yHn)Y>7^B