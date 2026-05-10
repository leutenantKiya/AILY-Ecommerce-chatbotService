package com.aily.controller;

import com.aily.App;
import com.aily.Session;
import com.aily.service.ApiService;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.util.Duration;

import java.net.URL;
import java.util.ResourceBundle;

public class AdminChatController implements Initializable {

    @FXML private VBox chatHistoryBox;

    private Timeline autoRefreshTimer;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        loadMessage();
        startAutoRefresh();
    }

    private void startAutoRefresh() {
        autoRefreshTimer = new Timeline(new KeyFrame(Duration.seconds(10), e -> loadChatHistory()));
        autoRefreshTimer.setCycleCount(Timeline.INDEFINITE);
        autoRefreshTimer.play();
    }

    private void stopAutoRefresh() {
        if (autoRefreshTimer != null) {
            autoRefreshTimer.stop();
        }
    }

    // Backward-compatible name with FXML/event expectations
    private void loadMessage() {
        loadChatHistory();
    }

    private void loadChatHistory() {
        chatHistoryBox.getChildren().clear();
        chatHistoryBox.setSpacing(10);

        new Thread(() -> {
            try {
                JsonObject response = ApiService.getAdminChatHistory();
                Platform.runLater(() -> {
                    if (response.has("status") && response.get("status").getAsInt() == 200) {
                        JsonObject data = response.getAsJsonObject("data");
                        if (data.has("users") && data.get("users").isJsonArray()) {
                            JsonArray users = data.getAsJsonArray("users");
                            if (users.isEmpty()) { showEmpty(); return; }

                            for (int u = 0; u < users.size(); u++) {
                                JsonObject userObj = users.get(u).getAsJsonObject();
                                String userId = userObj.has("user_id") ? userObj.get("user_id").getAsString() : "";
                                String username = userObj.has("username") ? userObj.get("username").getAsString() : "Unknown";

                                chatHistoryBox.getChildren().add(buildUserBubble(username, userId));
                            }
                        } else {
                            showEmpty();
                        }
                    } else {
                        showEmpty();
                    }
                });
            } catch (Exception e) {
                Platform.runLater(this::showEmpty);
            }
        }).start();
    }

    private void showEmpty() {
        chatHistoryBox.getChildren().clear();
        Label empty = new Label("Belum ada riwayat chat.");
        empty.getStyleClass().add("text-gray");
        empty.setPadding(new Insets(20));
        chatHistoryBox.getChildren().add(empty);
    }

    private HBox buildUserBubble(String username, String userId) {
        // Username - ID label
        String displayText = username + (userId.isBlank() ? "" : "  -  ID " + userId);
        Label userLabel = new Label(displayText);
        userLabel.getStyleClass().add("table-cell-bold");
        userLabel.setWrapText(true);
        HBox.setHgrow(userLabel, Priority.ALWAYS);

        // "Lihat" button
        Button lihatBtn = new Button("Lihat");
        lihatBtn.getStyleClass().add("btn-teal");
        lihatBtn.setStyle("-fx-font-size: 12px; -fx-padding: 6 18 6 18;");
        lihatBtn.setOnAction(e -> {
            Session.adminSelectedChatUserId = userId;
            Session.adminSelectedChatUsername = username;
            try { App.switchScene("admin_chat_detail"); } catch (Exception ignored) {}
        });

        HBox row = new HBox(12, userLabel, lihatBtn);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("order-card");
        row.setPadding(new Insets(14, 16, 14, 16));
        return row;
    }

    @FXML private void goOverview()     { stopAutoRefresh(); try { App.switchScene("admin_overview"); } catch (Exception ignored) {} }
    @FXML private void goProducts()     { stopAutoRefresh(); try { App.switchScene("admin_products"); } catch (Exception ignored) {} }
    @FXML private void goStoreInfo()    { stopAutoRefresh(); try { App.switchScene("admin_store"); } catch (Exception ignored) {} }
    @FXML private void goTransactions() { stopAutoRefresh(); try { App.switchScene("admin_transactions"); } catch (Exception ignored) {} }
    @FXML private void goChatHistory()  { /* already here */ }

    @FXML
    private void handleLogout() {
        stopAutoRefresh();
        Session.clear();
        try { App.switchScene("landing"); } catch (Exception ignored) {}
    }
}
