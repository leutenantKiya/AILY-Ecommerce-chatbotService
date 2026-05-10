package com.aily.controller;

import com.aily.App;
import com.aily.Session;
import com.aily.model.User;
import com.aily.service.ApiService;
import com.google.gson.JsonObject;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

import java.net.URL;
import java.util.ResourceBundle;

public class ProfileController implements Initializable {

    @FXML private TextField usernameField;
    @FXML private TextField emailField;
    @FXML private TextField phoneField;
    @FXML private TextField genderField;
    @FXML private TextArea addressField;
    @FXML private Label statusLabel;
    @FXML private Button saveButton;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        User user = Session.currentUser;
        if (user == null) {
            statusLabel.setText("Sesi habis. Silakan login ulang.");
            return;
        }

        fillFields(user);
        new Thread(this::loadProfile).start();
    }

    private void loadProfile() {
        try {
            JsonObject response = ApiService.getUserProfile(Session.currentUser.getId());
            if (response.has("status") && response.get("status").getAsInt() == 200) {
                JsonObject data = response.getAsJsonObject("data");
                Platform.runLater(() -> {
                    usernameField.setText(asString(data, "username", ""));
                    emailField.setText(asString(data, "email", ""));
                    phoneField.setText(asString(data, "phone", ""));
                    addressField.setText(asString(data, "address", ""));
                    genderField.setText(asString(data, "gender", "L"));
                });
            }
        } catch (Exception ignored) {
        }
    }

    @FXML
    private void handleSave() {
        User user = Session.currentUser;
        if (user == null) {
            statusLabel.setText("Sesi habis. Silakan login ulang.");
            return;
        }

        String username = usernameField.getText().trim();
        String email = emailField.getText().trim();
        String phone = phoneField.getText().trim();
        String address = addressField.getText().trim();
        String gender = normalizeGender(genderField.getText().trim());

        if (username.isEmpty() || email.isEmpty() || phone.isEmpty() || address.isEmpty()) {
            statusLabel.setText("Semua field wajib diisi.");
            return;
        }

        if (gender.isEmpty()) {
            statusLabel.setText("Gender harus L atau P.");
            return;
        }

        saveButton.setDisable(true);
        statusLabel.setText("");

        new Thread(() -> {
            try {
                JsonObject response = ApiService.updateUserProfile(user.getId(), new String[][] {
                        {"username", username},
                        {"email", email},
                        {"phone", phone},
                        {"address", address},
                        {"gender", gender}
                });

                Platform.runLater(() -> {
                    saveButton.setDisable(false);
                    if (response.has("status") && response.get("status").getAsInt() == 200) {
                        user.setUsername(username);
                        user.setEmail(email);
                        user.setPhone(phone);
                        user.setAddress(address);
                        user.setGender(gender);
                        statusLabel.setStyle("-fx-text-fill: #00D4A3;");
                        statusLabel.setText("Profil berhasil diperbarui.");
                    } else {
                        statusLabel.setStyle("-fx-text-fill: #E05252;");
                        statusLabel.setText(response.has("error") ? response.get("error").getAsString() : "Gagal menyimpan profil.");
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    saveButton.setDisable(false);
                    statusLabel.setStyle("-fx-text-fill: #E05252;");
                    statusLabel.setText("Tidak dapat terhubung ke server.");
                });
            }
        }).start();
    }

    @FXML
    private void goBack() {
        try { App.switchScene("chat"); } catch (Exception ignored) {}
    }

    private void fillFields(User user) {
        usernameField.setText(user.getUsername());
        emailField.setText(user.getEmail());
        phoneField.setText(user.getPhone());
        addressField.setText(user.getAddress());
        genderField.setText(user.getGender());
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

    private String asString(JsonObject data, String key, String fallback) {
        return data.has(key) && !data.get(key).isJsonNull() ? data.get(key).getAsString() : fallback;
    }
}
