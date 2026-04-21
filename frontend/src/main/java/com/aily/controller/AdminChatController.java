package com.aily.controller;

import com.aily.App;
import com.aily.Session;
import com.aily.model.User;
import com.google.gson.JsonObject;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class AdminChatController implements Initializable {

    @FXML private VBox chatHistoryBox;

    // Dummy data — in production fetch from backend
    private record ChatEntry(String user, String lastMsg, boolean answered) {}

    private final List<ChatEntry> entries = List.of(
        new ChatEntry("Kevin S", "Apakah Laptop Asusnya masih tersedia", false),
        new ChatEntry("Kevin S", "Apakah Laptop Asusnya masih tersedia", true),
        new ChatEntry("Kevin S", "Apakah Laptop Asusnya masih tersedia", false)
    );

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        chatHistoryBox.getChildren().clear();
        for (ChatEntry entry : entries) {
            chatHistoryBox.getChildren().add(buildRow(entry));
        }
    }

    private JsonObject getData(Session session) {
        User user = Session.currentUser;

    }

    private HBox buildRow(ChatEntry e) {
        VBox info = new VBox(4);
        Label userName = new Label(e.user());
        userName.getStyleClass().add("table-cell-bold");
        Label msg = new Label("User: \"" + e.lastMsg() + "\"");
        msg.getStyleClass().add("text-gray");
        info.getChildren().addAll(userName, msg);
        HBox.setHgrow(info, Priority.ALWAYS);

        Label badge = new Label(e.answered() ? "Terjawab" : "Belum Terjawab");
        badge.getStyleClass().add(e.answered() ? "status-terjawab" : "status-belum-terjawab");

        HBox row = new HBox(info, badge);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("order-card");
        row.setPadding(new Insets(14, 16, 14, 16));
        return row;
    }

    @FXML private void goOverview()     { try { App.switchScene("admin_overview",     1280, 880); } catch (Exception ignored) {} }
    @FXML private void goProducts()     { try { App.switchScene("admin_products",     1280, 880); } catch (Exception ignored) {} }
    @FXML private void goTransactions() { try { App.switchScene("admin_transactions", 1280, 880); } catch (Exception ignored) {} }
    @FXML private void goChatHistory()  { /* already here */ }

    @FXML
    private void handleLogout() {
        Session.clear();
        try { App.switchScene("landing", 1000, 700); } catch (Exception ignored) {}
    }
}
