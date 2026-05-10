package com.aily.controller;

import com.aily.App;
import com.aily.service.ApiService;
import com.google.gson.JsonObject;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;

public class RegisterController {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private TextField emailField;
    @FXML private TextField phoneField;
    @FXML private TextField addressField;
    @FXML private TextField genderField;
    @FXML private Button registerButton;
    @FXML private Label errorLabel;

    @FXML
    private void initialize() {
        
        registerButton.setDefaultButton(true);
        
        usernameField.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) {
                handleRegister();
                e.consume();
            }
        });
        passwordField.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) {
                handleRegister();
                e.consume();
            }
        });
    }

    @FXML
    private void handleRegister() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText().trim();
        String email = emailField.getText().trim();
        String phone = phoneField.getText().trim();
        String address = addressField.getText().trim();
        String gender = normalizeGender(genderField.getText().trim());

        if (username.isEmpty() || password.isEmpty() || email.isEmpty() || phone.isEmpty() || address.isEmpty()) {
            errorLabel.setText("Username, password, email, telepon, dan alamat wajib diisi.");
            return;
        }

        if (gender.isEmpty()) {
            errorLabel.setText("Gender harus L atau P.");
            return;
        }

        registerButton.setDisable(true);
        errorLabel.setText("");

        new Thread(() -> {
            try {
                JsonObject response = ApiService.register(
                        username, password, email, phone, address, "user", gender);

                Platform.runLater(() -> {
                    registerButton.setDisable(false);
                    if (response.has("status") && response.get("status").getAsInt() == 200) {
                        try {
                            // Kembali ke landing, tampilkan toast di sana
                            App.switchScene("landing");
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
        try { App.switchScene("landing"); }
        catch (Exception e) { errorLabel.setText("Gagal kembali."); }
    }

    private String normalizeGender(String value) {
        String normalized = value.toUpperCase();
        if (normalized.equals("L") || normalized.equals("LAKI") || normalized.equals("LAKI-LAKI")) {
            return "L";
        }
        if (normalized.equals("P") || normalized.equals("PEREMPUAN") || normalized.equals("WANITA")) {
            return "P";
        }
        return "";
    }
}
