package com.aily.controller;

import com.aily.App;
import com.aily.Session;
import com.aily.model.User;
import com.aily.service.ApiService;
import com.google.gson.JsonObject;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;

public class LoginAdminController {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Button loginButton;
    @FXML private Label errorLabel;

    @FXML
    private void initialize() {
        
        loginButton.setDefaultButton(true);

        
        usernameField.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) {
                handleLogin();
                e.consume();
            }
        });
        passwordField.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) {
                handleLogin();
                e.consume();
            }
        });
        hideMessage();
    }

    @FXML
    private void handleLogin() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText().trim();

        if (username.isEmpty() || password.isEmpty()) {
            showError("Username dan password tidak boleh kosong.");
            return;
        }

        loginButton.setDisable(true);
        hideMessage();

        new Thread(() -> {
            try {
                JsonObject response = ApiService.login(username, password);

                Platform.runLater(() -> {
                    loginButton.setDisable(false);
                    if (response.has("status") && response.get("status").getAsInt() == 200) {
                        JsonObject data = response.getAsJsonObject("data");
                        String role = data.get("role").getAsString();

                        if (!"admin".equalsIgnoreCase(role)) {
                            showError("Akun ini bukan akun admin.");
                            return;
                        }

                        Session.currentUser = new User(
                                data.get("id").getAsString(),
                                data.get("username").getAsString(),
                                data.get("email").getAsString(),
                                data.get("phone").getAsString(),
                                data.get("address").getAsString(),
                                role,
                                data.has("gender") ? data.get("gender").getAsString() : "L"
                        );
                        try {
                            App.switchScene("admin_overview");
                        } catch (Exception e) {
                            showError("Gagal membuka dashboard admin.");
                        }
                    } else {
                        String msg = response.has("error")
                                ? response.get("error").getAsString() : "Login gagal.";
                        showError(msg);
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    loginButton.setDisable(false);
                    showError("Tidak dapat terhubung ke server.");
                });
            }
        }).start();
    }

    @FXML
    private void goToRegister() {
        try { App.switchScene("register"); }
        catch (Exception e) { showError("Gagal membuka halaman register."); }
    }

    @FXML
    private void goBack() {
        try { App.switchScene("landing"); }
        catch (Exception e) { showError("Gagal kembali."); }
    }

    @FXML
    private void handleForgotPassword() {
        showMessage("Link reset password telah dikirim ke email kamu", "#00D4A3");
    }

    private void hideMessage() {
        errorLabel.setText("");
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
    }

    private void showError(String message) {
        showMessage(message, "#E05252");
    }

    private void showMessage(String message, String color) {
        errorLabel.setStyle("-fx-text-fill: " + color + ";");
        errorLabel.setText(message);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }
}
