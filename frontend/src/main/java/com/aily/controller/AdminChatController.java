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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.Set;

public class AdminChatController implements Initializable {

    @FXML private VBox chatHistoryBox;
    @FXML private Label adminNotificationLabel;

    private Timeline autoRefreshTimer;
    private final Map<String, Integer> knownChatCounts = new HashMap<>();
    private final Set<String> unreadUserIds = new HashSet<>();
    private boolean chatHistoryLoaded = false;

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

                            int unreadCount = 0;
                            for (int u = 0; u < users.size(); u++) {
                                JsonObject userObj = users.get(u).getAsJsonObject();
                                String userId = userObj.has("user_id") ? userObj.get("user_id").getAsString() : "";
                                String username = userObj.has("username") ? userObj.get("username").getAsString() : "Unknown";
                                boolean openConnection = userObj.has("open_connection") && userObj.get("open_connection").getAsBoolean();
                                boolean adminNeeded = userObj.has("admin_needed") && userObj.get("admin_needed").getAsBoolean();
                                boolean adminConnected = userObj.has("admin_connected") && userObj.get("admin_connected").getAsBoolean();
                                int chatCount = countChats(userObj);
                                int previousCount = knownChatCounts.getOrDefault(userId, chatCount);
                                String lastRole = getLastRole(userObj);
                                boolean newIncomingMessage = chatHistoryLoaded
                                        && chatCount > previousCount
                                        && "user".equalsIgnoreCase(lastRole);

                                if (adminNeeded || newIncomingMessage) {
                                    unreadUserIds.add(userId);
                                } else if (chatCount > previousCount && "bot".equalsIgnoreCase(lastRole)) {
                                    unreadUserIds.remove(userId);
                                }

                                boolean hasNotification = unreadUserIds.contains(userId) || adminNeeded;
                                if (hasNotification) {
                                    unreadCount++;
                                }
                                knownChatCounts.put(userId, chatCount);

                                chatHistoryBox.getChildren().add(buildUserBubble(username, userId, openConnection, adminNeeded, adminConnected, hasNotification));
                            }
                            chatHistoryLoaded = true;
                            updateAdminNotification(unreadCount);
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
        updateAdminNotification(0);
    }

    private HBox buildUserBubble(String username, String userId, boolean openConnection, boolean adminNeeded, boolean adminConnected, boolean hasNotification) {
        String displayText = username + (userId.isBlank() ? "" : "  -  ID " + userId);
        Label userLabel = new Label(displayText);
        userLabel.getStyleClass().add("table-cell-bold");
        userLabel.setWrapText(true);
        HBox.setHgrow(userLabel, Priority.ALWAYS);

        Label statusLabel = new Label(buildConnectionLabel(openConnection, adminNeeded, adminConnected));
        statusLabel.getStyleClass().add("connection-indicator");
        if (adminConnected) {
            statusLabel.getStyleClass().add("indicator-open");
        } else if (adminNeeded || openConnection) {
            statusLabel.getStyleClass().add("indicator-waiting");
        }

        Label notificationLabel = new Label("Pesan baru");
        notificationLabel.getStyleClass().add("notification-badge");
        notificationLabel.setVisible(hasNotification);
        notificationLabel.setManaged(hasNotification);

        Button renameBtn = new Button("✎");
        renameBtn.getStyleClass().add("clear-btn");
        renameBtn.setStyle("-fx-font-size: 12px; -fx-padding: 4 10 4 10;");
        renameBtn.setOnAction(e -> {
            TextInputDialog dialog = new TextInputDialog(username);
            dialog.setTitle("Rename User");
            dialog.setHeaderText("Ganti username untuk " + username);
            dialog.setContentText("Username baru:");
            Optional<String> result = dialog.showAndWait();
            result.ifPresent(newName -> {
                String trimmed = newName.trim();
                if (trimmed.isEmpty() || trimmed.equals(username)) return;
                new Thread(() -> {
                    try {
                        JsonObject resp = ApiService.renameUser(userId, trimmed);
                        Platform.runLater(() -> {
                            if (resp.has("status") && resp.get("status").getAsInt() == 200) {
                                loadChatHistory();
                            } else {
                                String err = resp.has("error") ? resp.get("error").getAsString() : "Gagal rename";
                                new Alert(Alert.AlertType.ERROR, err, ButtonType.OK).showAndWait();
                            }
                        });
                    } catch (Exception ex) {
                        Platform.runLater(() -> {
                            new Alert(Alert.AlertType.ERROR, "Gagal terhubung ke server", ButtonType.OK).showAndWait();
                        });
                    }
                }).start();
            });
        });

        Button lihatBtn = new Button("Lihat");
        lihatBtn.getStyleClass().add("btn-teal");
        lihatBtn.setStyle("-fx-font-size: 12px; -fx-padding: 6 18 6 18;");
        lihatBtn.setOnAction(e -> {
            unreadUserIds.remove(userId);
            Session.adminSelectedChatUserId = userId;
            Session.adminSelectedChatUsername = username;
            try { App.switchScene("admin_chat_detail"); } catch (Exception ignored) {}
        });

        HBox row = new HBox(12, userLabel, statusLabel, notificationLabel, renameBtn, lihatBtn);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("order-card");
        row.setPadding(new Insets(14, 16, 14, 16));
        return row;
    }

    private int countChats(JsonObject userObj) {
        if (userObj == null || !userObj.has("groups") || !userObj.get("groups").isJsonArray()) {
            return 0;
        }
        int count = 0;
        JsonArray groups = userObj.getAsJsonArray("groups");
        for (int i = 0; i < groups.size(); i++) {
            JsonObject group = groups.get(i).getAsJsonObject();
            if (group.has("chats") && group.get("chats").isJsonArray()) {
                count += group.getAsJsonArray("chats").size();
            }
        }
        return count;
    }

    private String getLastRole(JsonObject userObj) {
        if (userObj == null || !userObj.has("groups") || !userObj.get("groups").isJsonArray()) {
            return "";
        }
        JsonArray groups = userObj.getAsJsonArray("groups");
        for (int g = groups.size() - 1; g >= 0; g--) {
            JsonObject group = groups.get(g).getAsJsonObject();
            if (!group.has("chats") || !group.get("chats").isJsonArray()) {
                continue;
            }
            JsonArray chats = group.getAsJsonArray("chats");
            if (!chats.isEmpty()) {
                JsonObject chat = chats.get(chats.size() - 1).getAsJsonObject();
                return chat.has("role") ? chat.get("role").getAsString() : "";
            }
        }
        return "";
    }

    private void updateAdminNotification(int unreadCount) {
        if (adminNotificationLabel == null) {
            return;
        }
        boolean visible = unreadCount > 0;
        adminNotificationLabel.setVisible(visible);
        adminNotificationLabel.setManaged(visible);
        adminNotificationLabel.setText(visible
                ? unreadCount + " chat membutuhkan perhatian admin"
                : "");
    }

    private String buildConnectionLabel(boolean openConnection, boolean adminNeeded, boolean adminConnected) {
        if (adminConnected) {
            return "Admin terhubung";
        }
        if (adminNeeded || openConnection) {
            return "Butuh admin";
        }
        return "Bot";
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
