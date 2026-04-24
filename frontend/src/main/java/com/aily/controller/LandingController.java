package com.aily.controller;

import com.aily.App;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

public class LandingController {

    @FXML private Label toastLabel;
    @FXML private VBox  toastBox;

    @FXML
    private void goToUserLogin() {
        try {
            App.switchScene("login");
        } catch (Exception e) {
            showToast("Gagal membuka halaman login.");
        }
    }

    @FXML
    private void goToAdminLogin() {
        try {
            App.switchScene("login_admin");
        } catch (Exception e) {
            showToast("Gagal membuka halaman admin.");
        }
    }

    @FXML
    private void goToRegister() {
        try {
            App.switchScene("register");
        } catch (Exception e) {
            showToast("Gagal membuka halaman register.");
        }
    }

    private void showToast(String msg) {
        if (toastLabel != null) {
            toastLabel.setText(msg);
            toastBox.setVisible(true);
            toastBox.setManaged(true);
        }
    }
}
