package com.aily.controller;

import com.aily.App;
import com.aily.Session;
import com.aily.service.ApiService;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

public class AdminStoreController implements Initializable {

    @FXML private TextField questionField;
    @FXML private TextArea answerField;
    @FXML private Label statusLabel;
    @FXML private Label countLabel;
    @FXML private Button saveButton;
    @FXML private VBox storeInfoRowsBox;

    private final List<StoreInfo> storeInfos = new ArrayList<>();
    private StoreInfo editingInfo = null;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        loadStoreInfo();
    }

    @FXML
    private void handleSave() {
        String question = questionField.getText().trim();
        String answer = answerField.getText().trim();

        if (question.isEmpty() || answer.isEmpty()) {
            statusLabel.setStyle("-fx-text-fill: #E05252;");
            statusLabel.setText("Judul dan isi informasi wajib diisi.");
            return;
        }

        saveButton.setDisable(true);
        new Thread(() -> {
            try {
                JsonObject response = editingInfo == null
                        ? ApiService.addStoreInfo(question, answer)
                        : ApiService.updateStoreInfo(editingInfo.id(), question, answer);

                Platform.runLater(() -> {
                    saveButton.setDisable(false);
                    if (response.has("status") && response.get("status").getAsInt() == 200) {
                        statusLabel.setStyle("-fx-text-fill: #00D4A3;");
                        statusLabel.setText(editingInfo == null
                                ? "Informasi toko berhasil ditambahkan."
                                : "Informasi toko berhasil diperbarui.");
                        handleClear();
                        loadStoreInfo();
                    } else {
                        statusLabel.setStyle("-fx-text-fill: #E05252;");
                        statusLabel.setText(response.has("error") ? response.get("error").getAsString() : "Gagal menyimpan informasi toko.");
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
    private void handleClear() {
        editingInfo = null;
        questionField.clear();
        answerField.clear();
        saveButton.setText("Simpan Info");
    }

    private void loadStoreInfo() {
        new Thread(() -> {
            try {
                JsonObject response = ApiService.getStoreInfoAdmin();
                Platform.runLater(() -> {
                    storeInfos.clear();
                    if (response.has("status") && response.get("status").getAsInt() == 200) {
                        JsonObject data = response.getAsJsonObject("data");
                        JsonArray array = data.getAsJsonArray("store_info");
                        for (JsonElement itemEl : array) {
                            JsonObject item = itemEl.getAsJsonObject();
                            storeInfos.add(new StoreInfo(
                                    item.get("id").getAsInt(),
                                    item.get("question").getAsString(),
                                    item.get("answer").getAsString()
                            ));
                        }
                    }
                    refreshRows();
                });
            } catch (Exception e) {
                Platform.runLater(this::refreshRows);
            }
        }).start();
    }

    private void refreshRows() {
        storeInfoRowsBox.getChildren().clear();
        countLabel.setText(storeInfos.size() + " info");
        for (StoreInfo info : storeInfos) {
            storeInfoRowsBox.getChildren().add(buildRow(info));
        }
    }

    private HBox buildRow(StoreInfo info) {
        Label id = new Label(String.valueOf(info.id()));
        id.getStyleClass().add("table-cell-text");
        id.setPrefWidth(80);

        Label question = new Label(info.question());
        question.getStyleClass().add("table-cell-bold");
        question.setPrefWidth(220);
        question.setWrapText(true);

        Label answer = new Label(info.answer());
        answer.getStyleClass().add("table-cell-text");
        answer.setPrefWidth(520);
        answer.setWrapText(true);
        HBox.setHgrow(answer, Priority.ALWAYS);

        Button editBtn = new Button("Edit");
        editBtn.getStyleClass().add("btn-gray");
        editBtn.setOnAction(e -> startEdit(info));

        Button deleteBtn = new Button("Hapus");
        deleteBtn.getStyleClass().add("btn-danger");
        deleteBtn.setOnAction(e -> deleteInfo(info));

        HBox actions = new HBox(6, editBtn, deleteBtn);
        actions.setPrefWidth(140);

        HBox row = new HBox(id, question, answer, actions);
        row.getStyleClass().add("table-row");
        row.setPadding(new Insets(10, 0, 10, 0));
        return row;
    }

    private void startEdit(StoreInfo info) {
        editingInfo = info;
        questionField.setText(info.question());
        answerField.setText(info.answer());
        saveButton.setText("Update Info");
        statusLabel.setText("");
    }

    private void deleteInfo(StoreInfo info) {
        new Thread(() -> {
            try {
                ApiService.deleteStoreInfo(info.id());
                Platform.runLater(this::loadStoreInfo);
            } catch (Exception ignored) {
            }
        }).start();
    }

    @FXML private void goOverview()     { try { App.switchScene("admin_overview"); } catch (Exception ignored) {} }
    @FXML private void goProducts()     { try { App.switchScene("admin_products"); } catch (Exception ignored) {} }
    @FXML private void goStoreInfo()    { }
    @FXML private void goTransactions() { try { App.switchScene("admin_transactions"); } catch (Exception ignored) {} }
    @FXML private void goChatHistory()  { try { App.switchScene("admin_chat"); } catch (Exception ignored) {} }

    @FXML
    private void handleLogout() {
        Session.clear();
        try { App.switchScene("landing"); } catch (Exception ignored) {}
    }

    private record StoreInfo(int id, String question, String answer) { }
}
