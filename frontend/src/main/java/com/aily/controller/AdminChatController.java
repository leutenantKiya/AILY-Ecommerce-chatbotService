package com.aily.controller;

import com.aily.App;
import com.aily.Session;
import com.aily.service.ApiService;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.net.URL;
import java.util.ResourceBundle;

public class AdminChatController implements Initializable {

    @FXML private VBox chatHistoryBox;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        loadChatHistory();
    }

    private void loadChatHistory() {
        chatHistoryBox.getChildren().clear();

        // Load chat history for current admin user
        if (Session.currentUser == null) return;

        new Thread(() -> {
            try {
                JsonObject response = ApiService.loadChatHistory(Session.currentUser.getId());
                Platform.runLater(() -> {
                    if (response.has("status") && response.get("status").getAsInt() == 200) {
                        JsonObject data = response.getAsJsonObject("data");
                        if (data.has("chat_history")) {
                            JsonArray chats = data.getAsJsonArray("chat_history");
                            if (chats.isEmpty()) {
                                showEmpty();
                                return;
                            }
                            for (int i = 0; i < chats.size(); i++) {
                                JsonObject chat = chats.get(i).getAsJsonObject();
                                String user = chat.has("username") ? chat.get("username").getAsString() : "Unknown";
                                String msg = chat.has("message") ? formatMessage(chat.get("message")) : "";
                                String role = chat.has("role") ? chat.get("role").getAsString() : "user";
                                String time = chat.has("time") ? chat.get("time").getAsString() : "";
                                chatHistoryBox.getChildren().add(buildRow(user, msg, role, time));
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

    private String formatMessage(com.google.gson.JsonElement element) {
        if (element.isJsonPrimitive()) {
            return element.getAsString();
        } else if (element.isJsonObject()) {
            JsonObject obj = element.getAsJsonObject();
            if (obj.has("message")) return obj.get("message").getAsString();
            return obj.toString();
        }
        return element.toString();
    }

<<<<<<< Updated upstream
    private void showEmpty() {
        chatHistoryBox.getChildren().clear();
        Label empty = new Label("Belum ada riwayat chat.");
        empty.getStyleClass().add("text-gray");
        empty.setPadding(new Insets(20));
        chatHistoryBox.getChildren().add(empty);
    }
=======
//    private JsonObject getData(Session session) {
//        User user = Session.currentUser;
//
//    }
>>>>>>> Stashed changes

    private HBox buildRow(String user, String lastMsg, String role, String time) {
        VBox info = new VBox(4);
        Label userName = new Label(user);
        userName.getStyleClass().add("table-cell-bold");
        Label msg = new Label(lastMsg);
        msg.getStyleClass().add("text-gray");
        msg.setMaxWidth(400);
        msg.setWrapText(true);
        info.getChildren().addAll(userName, msg);
        HBox.setHgrow(info, Priority.ALWAYS);

        VBox rightBox = new VBox(4);
        rightBox.setAlignment(Pos.CENTER_RIGHT);

        Label badge = new Label("bot".equals(role) ? "Bot" : "User");
        badge.getStyleClass().add("bot".equals(role) ? "status-terjawab" : "status-belum-terjawab");

        Label timeLabel = new Label(time);
        timeLabel.getStyleClass().add("text-gray");

        rightBox.getChildren().addAll(badge, timeLabel);

        HBox row = new HBox(info, rightBox);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("order-card");
        row.setPadding(new Insets(14, 16, 14, 16));
        return row;
    }

    @FXML private void goOverview()     { try { App.switchScene("admin_overview",     1920, 1080); } catch (Exception ignored) {} }
    @FXML private void goProducts()     { try { App.switchScene("admin_products",     1920, 1080); } catch (Exception ignored) {} }
    @FXML private void goTransactions() { try { App.switchScene("admin_transactions", 1920, 1080); } catch (Exception ignored) {} }
    @FXML private void goChatHistory()  { /* already here */ }

    @FXML
    private void handleLogout() {
        Session.clear();
        try { App.switchScene("landing", 1000, 700); } catch (Exception ignored) {}
    }
}
