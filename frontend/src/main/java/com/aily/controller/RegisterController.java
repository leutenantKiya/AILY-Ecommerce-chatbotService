package com.aily.controller;

import com.aily.App;
import com.aily.service.ApiService;
import com.google.gson.JsonObject;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;

public class RegisterController {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Button registerButton;
    @FXML private Label errorLabel;

    @FXML
    private void handleRegister() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText().trim();

        if (username.isEmpty() || password.isEmpty()) {
            errorLabel.setText("Username dan password wajib diisi.");
            return;
        }

        registerButton.setDisable(true);
        errorLabel.setText("");

        new Thread(() -> {
            try {
                JsonObject response = ApiService.register(
                        username, password, "", "", "", "user");

                Platform.runLater(() -> {
                    registerButton.setDisable(false);
                    if (response.has("status") && response.get("status").getAsInt() == 200) {
                        try {
                            // Kembali ke landing, tampilkan toast di sana
                            App.switchScene("landing", 1000, 700);
                        } catch (Exception e) {
                            errorLabel.setText("Berhasil, gagal kembali ke landing.");
                        }
                    } else {
                        String msg = response.has("error")
                                ? response.get("error").getAsString() : "Registrasi gagal.";
                        errorLabel.setText(msg);
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    registerButton.setDisable(false);
                    errorLabel.setText("Tidak dapat terhubung ke server.");
                });
            }
        }).start();
    }

    @FXML
    private void goBack() {
        try { App.switchScene("landing", 1000, 700); }
        catch (Exception e) { errorLabel.setText("Gagal kembali."); }
    }
}
