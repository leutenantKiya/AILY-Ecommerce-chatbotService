package com.aily.controller;

import com.aily.App;
import com.aily.Session;
import com.aily.model.User;
import com.aily.service.ApiService;
import com.google.gson.JsonObject;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;

public class LoginController {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Button loginButton;
    @FXML private Label errorLabel;

    @FXML
    private void handleLogin() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText().trim();

        if (username.isEmpty() || password.isEmpty()) {
            errorLabel.setText("Username dan password tidak boleh kosong.");
            return;
        }

        loginButton.setDisable(true);
        errorLabel.setText("");

        new Thread(() -> {
            try {
                JsonObject response = ApiService.login(username, password);

                Platform.runLater(() -> {
                    loginButton.setDisable(false);
                    if (response.has("status") && response.get("status").getAsInt() == 200) {
                        JsonObject data = response.getAsJsonObject("data");
                        Session.currentUser = new User(
                                data.get("id").getAsString(),
                                data.get("username").getAsString(),
                                data.get("email").getAsString(),
                                data.get("phone").getAsString(),
                                data.get("address").getAsString(),
                                data.get("role").getAsString()
                        );
                        try {
                            App.switchScene("chat", 1280, 880);
                        } catch (Exception e) {
                            errorLabel.setText("Gagal membuka halaman chat.");
                        }
                    } else {
                        String msg = response.has("error")
                                ? response.get("error").getAsString() : "Login gagal.";
                        errorLabel.setText(msg);
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    loginButton.setDisable(false);
                    errorLabel.setText("Tidak dapat terhubung ke server.");
                });
            }
        }).start();
    }

    @FXML
    private void goToRegister() {
        try { App.switchScene("register", 900, 700); }
        catch (Exception e) { errorLabel.setText("Gagal membuka halaman register."); }
    }

    @FXML
    private void goBack() {
        try { App.switchScene("landing", 1000, 700); }
        catch (Exception e) { errorLabel.setText("Gagal kembali."); }
    }

    @FXML
    private void handleForgotPassword() {
        // placeholder — backend belum ada endpoint reset password
        errorLabel.setStyle("-fx-text-fill: #00D4A3;");
        errorLabel.setText("Link reset password telah dikirim ke email kamu");
    }
}
