package javafx.com.yasbu_dekstop.controllers;

import javafx.application.Platform;
import javafx.com.yasbu_dekstop.services.UserService;
import javafx.com.yasbu_dekstop.models.UserJson;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.event.ActionEvent;
import javafx.concurrent.Task;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.regex.Pattern;

public class LoginController implements Initializable {

    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private Button loginButton;
    @FXML private Hyperlink forgotPasswordLink;
    @FXML private Label errorLabel;

    private UserService userService = new UserService();
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9+_.-]+@([A-Za-z0-9.-]+\\.[A-Za-z]{2,})$"
    );

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupValidation();
        Platform.runLater(() -> emailField.requestFocus());
    }

    private void setupValidation() {
        emailField.textProperty().addListener((observable, oldValue, newValue) -> {
            clearFieldError(emailField);
            errorLabel.setText("");
        });

        passwordField.textProperty().addListener((observable, oldValue, newValue) -> {
            clearFieldError(passwordField);
            errorLabel.setText("");
        });

        emailField.textProperty().addListener((obs, oldText, newText) -> updateLoginButton());
        passwordField.textProperty().addListener((obs, oldText, newText) -> updateLoginButton());
    }

    private void updateLoginButton() {
        boolean isFormValid = !emailField.getText().trim().isEmpty() &&
                !passwordField.getText().isEmpty();
        loginButton.setDisable(!isFormValid);
    }

    @FXML
    private void handleLogin(ActionEvent event) {
        if (!validateForm()) {
            return;
        }

        loginButton.setDisable(true);
        loginButton.setText("Signing in...");
        errorLabel.setText("");

        Task<UserJson> loginTask = new Task<UserJson>() {
            @Override
            protected UserJson call() throws Exception {
                String email = emailField.getText().trim();
                String password = passwordField.getText();

                System.out.println("Attempting login for: " + email); // Debug log

                // Login dan dapatkan user data
                UserJson loginResult = userService.login(email, password);
                System.out.println("Login successful for user ID: " + loginResult.getUserId()); // Debug log

                // Ambil data user lengkap
                UserJson fullUser = userService.getUser(loginResult.getUserId());
                System.out.println("Full user data retrieved: " + fullUser.getUsername()); // Debug log

                return fullUser;
            }

            @Override
            protected void succeeded() {
                Platform.runLater(() -> {
                    try {
                        UserJson user = getValue();
                        System.out.println("Navigating to home for user: " + user.getUsername()); // Debug log
                        navigateToHome(user);
                    } catch (Exception e) {
                        System.err.println("Error navigating to home: " + e.getMessage());
                        e.printStackTrace();
                        handleLoginError("Failed to navigate to home: " + e.getMessage());
                    } finally {
                        resetLoginButton();
                    }
                });
            }

            @Override
            protected void failed() {
                Platform.runLater(() -> {
                    Throwable exception = getException();
                    System.err.println("Login failed: " + exception.getMessage());
                    exception.printStackTrace();
                    handleLoginError(exception.getMessage());
                    resetLoginButton();
                });
            }
        };

        new Thread(loginTask).start();
    }

    private void navigateToHome(UserJson user) {
        try {
            System.out.println("Loading HomeView.fxml..."); // Debug log

            // Load home view
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/javafx/com/yasbu_dekstop/home-view.fxml"));
            Parent root = loader.load();

            System.out.println("HomeView.fxml loaded successfully"); // Debug log

            // Get HomeController and set user data
            HomeController homeController = loader.getController();
            if (homeController != null) {
                homeController.setUserData(user);
                System.out.println("User data set to HomeController"); // Debug log
            } else {
                System.err.println("HomeController is null!");
            }

            // Get current stage
            Stage currentStage = (Stage) loginButton.getScene().getWindow();

            // Create new scene and set it to current stage
            Scene homeScene = new Scene(root);
            currentStage.setScene(homeScene);
            currentStage.setTitle("Yasbu Desktop - Home");

            System.out.println("Navigation to home completed"); // Debug log

        } catch (IOException e) {
            System.err.println("Failed to load HomeView.fxml: " + e.getMessage());
            e.printStackTrace();
            handleLoginError("Failed to load home view: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Unexpected error during navigation: " + e.getMessage());
            e.printStackTrace();
            handleLoginError("Unexpected error: " + e.getMessage());
        }
    }

    private void handleLoginError(String message) {
        errorLabel.setText(message);
        passwordField.clear();
        emailField.requestFocus();
    }

    private boolean validateForm() {
        boolean isValid = true;

        String email = emailField.getText().trim();
        if (email.isEmpty()) {
            setFieldError(emailField, "Email is required");
            isValid = false;
        } else if (!EMAIL_PATTERN.matcher(email).matches()) {
            setFieldError(emailField, "Please enter a valid email address");
            isValid = false;
        }

        String password = passwordField.getText();
        if (password.isEmpty()) {
            setFieldError(passwordField, "Password is required");
            isValid = false;
        } else if (password.length() < 6) {
            setFieldError(passwordField, "Password must be at least 6 characters");
            isValid = false;
        }

        return isValid;
    }

    private void setFieldError(TextField field, String message) {
        field.getStyleClass().add("error-field");
        errorLabel.setText(message);
    }

    private void clearFieldError(TextField field) {
        field.getStyleClass().remove("error-field");
    }

    private void resetLoginButton() {
        loginButton.setText("Sign In");
        updateLoginButton();
    }

    @FXML
    private void handleForgotPassword(ActionEvent event) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Forgot Password");
        alert.setHeaderText("Password Recovery");
        alert.setContentText("Please check your email for password reset instructions.");
        alert.showAndWait();
    }
}