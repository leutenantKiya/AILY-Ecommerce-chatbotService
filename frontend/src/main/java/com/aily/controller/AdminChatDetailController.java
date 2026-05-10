package com.aily.controller;

import com.aily.App;
import com.aily.Session;
import com.aily.model.Product;
import com.aily.service.ApiService;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.util.Duration;

import java.io.ByteArrayInputStream;
import java.net.URL;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.ResourceBundle;

public class AdminChatDetailController implements Initializable {

    @FXML private Label chatWithLabel;
    @FXML private Label chatSubLabel;
    @FXML private ScrollPane scrollPane;
    @FXML private VBox messageContainer;
    @FXML private TextField messageInput;
    @FXML private Button sendButton;

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");
    private Timeline autoRefreshTimer;
    private int lastChatCount = 0;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        String username = Session.adminSelectedChatUsername != null ? Session.adminSelectedChatUsername : "User";
        String userId = Session.adminSelectedChatUserId != null ? Session.adminSelectedChatUserId : "";
        chatWithLabel.setText("Chat dengan " + username);
        chatSubLabel.setText(userId.isBlank() ? "History & reply as bot" : ("ID " + userId));

        sendButton.setDefaultButton(true);
        messageInput.setOnAction(e -> handleSend());

        refresh();
        startAutoRefresh();
    }

    private void startAutoRefresh() {
        autoRefreshTimer = new Timeline(new KeyFrame(Duration.seconds(5), e -> pollForNewMessages()));
        autoRefreshTimer.setCycleCount(Timeline.INDEFINITE);
        autoRefreshTimer.play();
    }

    private void stopAutoRefresh() {
        if (autoRefreshTimer != null) {
            autoRefreshTimer.stop();
        }
    }

    @FXML
    private void refresh() {
        messageContainer.getChildren().clear();
        lastChatCount = 0;
        if (Session.adminSelectedChatUserId == null || Session.adminSelectedChatUserId.isBlank()) {
            addSystemLine("User belum dipilih.");
            return;
        }

        new Thread(() -> {
            try {
                JsonObject response = ApiService.getAdminChatHistoryByUser(Session.adminSelectedChatUserId);
                Platform.runLater(() -> renderHistory(response));
            } catch (Exception e) {
                Platform.runLater(() -> addSystemLine("Gagal memuat chat."));
            }
        }).start();
    }

    /** Called by auto-refresh timer — only appends new messages. */
    private void pollForNewMessages() {
        if (Session.adminSelectedChatUserId == null || Session.adminSelectedChatUserId.isBlank()) return;

        new Thread(() -> {
            try {
                JsonObject response = ApiService.getAdminChatHistoryByUser(Session.adminSelectedChatUserId);
                Platform.runLater(() -> appendNewMessages(response));
            } catch (Exception ignored) {}
        }).start();
    }

    /** Flatten all groups→chats, count total. If more than lastChatCount, append only the new ones. */
    private void appendNewMessages(JsonObject response) {
        if (response == null || !response.has("status") || response.get("status").getAsInt() != 200) return;

        JsonObject data = response.getAsJsonObject("data");
        if (data == null || !data.has("groups") || !data.get("groups").isJsonArray()) return;

        JsonArray groups = data.getAsJsonArray("groups");

        // Flatten all chat entries to count total
        int totalChats = 0;
        for (int g = 0; g < groups.size(); g++) {
            JsonObject group = groups.get(g).getAsJsonObject();
            if (group.has("chats") && group.get("chats").isJsonArray()) {
                totalChats += group.getAsJsonArray("chats").size();
            }
        }

        if (totalChats <= lastChatCount) return; // no new messages

        // Walk through groups and skip the first lastChatCount entries, render the rest
        int skipped = 0;
        for (int g = 0; g < groups.size(); g++) {
            JsonObject group = groups.get(g).getAsJsonObject();
            if (!group.has("chats") || !group.get("chats").isJsonArray()) continue;
            JsonArray chats = group.getAsJsonArray("chats");

            for (int i = 0; i < chats.size(); i++) {
                if (skipped < lastChatCount) {
                    skipped++;
                    continue;
                }
                JsonObject chat = chats.get(i).getAsJsonObject();
                String role = chat.has("role") ? chat.get("role").getAsString() : "user";
                String time = chat.has("time") ? chat.get("time").getAsString() : "";
                JsonElement message = chat.get("message");

                if ("bot".equalsIgnoreCase(role)) {
                    renderBotHistoryMessage(message, time);
                } else {
                    addUserMessage(formatMessage(message), time);
                }
            }
        }

        lastChatCount = totalChats;
        scrollPane.setVvalue(1.0);
    }

    private void renderHistory(JsonObject response) {
        messageContainer.getChildren().clear();
        lastChatCount = 0;
        if (!(response != null && response.has("status") && response.get("status").getAsInt() == 200)) {
            addSystemLine("Belum ada chat.");
            return;
        }

        JsonObject data = response.getAsJsonObject("data");
        if (data == null || !data.has("groups") || !data.get("groups").isJsonArray()) {
            addSystemLine("Belum ada chat.");
            return;
        }

        JsonArray groups = data.getAsJsonArray("groups");
        if (groups.isEmpty()) {
            addSystemLine("Belum ada chat.");
            return;
        }

        for (int g = 0; g < groups.size(); g++) {
            JsonObject group = groups.get(g).getAsJsonObject();
            String date = group.has("datetime") ? group.get("datetime").getAsString() : "";
            if (!date.isBlank()) {
                Label dateHeader = new Label(date);
                dateHeader.getStyleClass().add("text-gray");
                dateHeader.setPadding(new Insets(10, 20, 6, 20));
                messageContainer.getChildren().add(dateHeader);
            }

            if (!group.has("chats") || !group.get("chats").isJsonArray()) continue;
            JsonArray chats = group.getAsJsonArray("chats");
            for (int i = 0; i < chats.size(); i++) {
                lastChatCount++;
                JsonObject chat = chats.get(i).getAsJsonObject();
                String role = chat.has("role") ? chat.get("role").getAsString() : "user";
                String time = chat.has("time") ? chat.get("time").getAsString() : "";
                JsonElement message = chat.get("message");

                if ("bot".equalsIgnoreCase(role)) {
                    renderBotHistoryMessage(message, time);
                } else {
                    addUserMessage(formatMessage(message), time);
                }
            }
        }

        Platform.runLater(() -> scrollPane.setVvalue(1.0));
    }

    @FXML
    private void handleSend() {
        String userId = Session.adminSelectedChatUserId;
        if (userId == null || userId.isBlank()) return;

        String msg = messageInput.getText() == null ? "" : messageInput.getText().trim();
        if (msg.isBlank()) return;

        messageInput.clear();
        addBotMessage(msg, LocalTime.now().format(TIME_FMT));
        lastChatCount++;
        scrollPane.setVvalue(1.0);

        new Thread(() -> {
            try {
                // Admin replies as bot into user's chat log
                ApiService.saveChatMessage(userId, "AILY Assistant", "bot", msg);
            } catch (Exception ignored) {
            }
        }).start();
    }

    private String formatMessage(JsonElement element) {
        if (element == null) return "";
        if (element.isJsonPrimitive()) return element.getAsString();
        if (element.isJsonObject()) {
            JsonObject obj = element.getAsJsonObject();
            if (obj.has("message")) return obj.get("message").getAsString();
            return obj.toString();
        }
        return element.toString();
    }

    private void addUserMessage(String text, String timeOverride) {
        messageContainer.getChildren().add(buildBubbleRow(text, true, timeOverride));
    }

    private void addBotMessage(String text, String timeOverride) {
        messageContainer.getChildren().add(buildBubbleRow(text, false, timeOverride));
    }

    private void addSystemLine(String text) {
        Label lbl = new Label(text);
        lbl.getStyleClass().add("text-gray");
        lbl.setPadding(new Insets(16));
        messageContainer.getChildren().add(lbl);
    }

    private void renderBotHistoryMessage(JsonElement messageElement, String timeOverride) {
        JsonElement normalizedMessage = normalizeJsonElement(messageElement);
        if (normalizedMessage == null || normalizedMessage.isJsonNull()) {
            addBotMessage("Pesan diproses.", timeOverride);
            return;
        }

        String helpFormatted = formatHelpPayload(normalizedMessage);
        if (!helpFormatted.isBlank()) {
            addBotMessage(helpFormatted, timeOverride);
            return;
        }

        JsonArray products = extractProducts(normalizedMessage);
        if (products != null) {
            messageContainer.getChildren().add(buildProductBubbleRow(products, timeOverride));
            return;
        }

        JsonObject messageObject = asJsonObject(normalizedMessage);
        if (messageObject != null) {
            JsonArray nestedProducts = extractProducts(messageObject.get("products"));
            if (nestedProducts != null) {
                messageContainer.getChildren().add(buildProductBubbleRow(nestedProducts, timeOverride));
                return;
            }

            if (messageObject.has("message")) {
                addBotMessage(formatMessage(messageObject.get("message")), timeOverride);
                return;
            }

            addBotMessage(messageObject.toString(), timeOverride);
            return;
        }

        if (normalizedMessage.isJsonArray()) {
            addBotMessage(normalizedMessage.toString(), timeOverride);
            return;
        }

        addBotMessage(formatMessage(normalizedMessage), timeOverride);
    }

    private String formatHelpPayload(JsonElement normalizedMessage) {
        JsonObject root = asJsonObject(normalizedMessage);
        if (root == null) {
            return "";
        }

        // Candidate sources:
        // - full conversation payload (data.nlp_result.konten + data.action_data.data.result)
        // - saved chat payload might be only action_data (Ok { data: { result: [...] } })
        JsonObject dataObj = root.has("data") && root.get("data").isJsonObject()
                ? root.getAsJsonObject("data")
                : root;

        JsonObject nlp = asJsonObject(dataObj.get("nlp_result"));
        JsonArray konten = nlp != null ? asJsonArray(nlp.get("konten")) : asJsonArray(dataObj.get("konten"));
        if (konten == null) {
            // Some payloads could store it under "help"
            konten = asJsonArray(dataObj.get("help"));
        }

        JsonArray pairs = null;
        JsonObject actionData = asJsonObject(dataObj.get("action_data"));
        if (actionData != null) {
            JsonObject d = asJsonObject(actionData.get("data"));
            pairs = d == null ? null : asJsonArray(d.get("result"));
        }
        if (pairs == null) {
            JsonObject d = asJsonObject(dataObj.get("data"));
            pairs = d == null ? null : asJsonArray(d.get("result"));
        }
        if (pairs == null) {
            pairs = asJsonArray(dataObj.get("result"));
        }

        boolean looksLikeHelp = (nlp != null && "help".equalsIgnoreCase(asString(nlp.get("intent"), "")))
                || konten != null
                || pairs != null;
        if (!looksLikeHelp) {
            return "";
        }

        StringBuilder sb = new StringBuilder();

        String header = "";
        if (nlp != null) {
            header = asString(nlp.get("respons"), "").trim();
        }
        if (header.isBlank()) {
            header = "Berikut daftar layanan yang bisa saya bantu.";
        }
        sb.append(header);

        if (konten != null && !konten.isEmpty()) {
            sb.append("\n\nMenu bantuan:\n");
            int idx = 0;
            for (JsonElement el : konten) {
                JsonObject item = asJsonObject(el);
                if (item == null) continue;
                String it = titleCase(asString(item.get("intent"), ""));
                String desc = asString(item.get("deskripsi"), "").trim();
                if (it.isBlank() && desc.isBlank()) continue;
                idx++;
                sb.append(idx).append(". ").append(it.isBlank() ? "-" : it);
                if (!desc.isBlank()) sb.append(" — ").append(desc);
                sb.append("\n");
            }
            if (idx > 0) sb.setLength(sb.length() - 1);
        }

        if (pairs != null && !pairs.isEmpty()) {
            sb.append("\n\nFAQ cepat:\n");
            for (JsonElement pairEl : pairs) {
                JsonArray pair = asJsonArray(pairEl);
                if (pair == null || pair.size() < 2) continue;
                String key = asString(pair.get(0), "").trim();
                String val = asString(pair.get(1), "").trim();
                if (key.isBlank() && val.isBlank()) continue;
                sb.append("- ").append(key.isBlank() ? "-" : key).append(": ").append(val).append("\n");
            }
            if (sb.charAt(sb.length() - 1) == '\n') sb.setLength(sb.length() - 1);
        }

        return sb.toString().trim();
    }

    private String titleCase(String raw) {
        if (raw == null) return "";
        String s = raw.trim();
        if (s.isEmpty()) return "";
        String[] parts = s.replace('_', ' ').split("\\s+");
        StringBuilder out = new StringBuilder();
        for (String p : parts) {
            if (p.isBlank()) continue;
            if (out.length() > 0) out.append(' ');
            out.append(Character.toUpperCase(p.charAt(0)));
            if (p.length() > 1) out.append(p.substring(1));
        }
        return out.toString();
    }

    private HBox buildProductBubbleRow(JsonArray products, String timeOverride) {
        String time = (timeOverride != null && !timeOverride.isBlank())
                ? timeOverride
                : LocalTime.now().format(TIME_FMT);

        VBox cardsContainer = new VBox(8);

        Text headerText = new Text("Ditemukan " + products.size() + " produk:\n");
        headerText.setStyle("-fx-font-size: 16px; -fx-fill: #c2d6f6; -fx-font-weight: bold;");
        cardsContainer.getChildren().add(headerText);

        int displayed = 0;
        for (int i = 0; i < products.size() && displayed < 5; i++) {
            JsonElement productEl = products.get(i);
            Product product = parseProduct(productEl);
            if (product == null) continue;
            displayed++;

            HBox productCard = new HBox(12);
            productCard.setAlignment(Pos.CENTER_LEFT);
            productCard.setPadding(new Insets(10));
            productCard.setStyle("-fx-background-color: #0d2232; -fx-background-radius: 8;");

            ImageView imageView = new ImageView();
            imageView.setFitWidth(150);
            imageView.setFitHeight(150);
            imageView.setPreserveRatio(false);

            String imageStr = product.getImage();
            if (imageStr != null && !imageStr.isEmpty()) {
                try {
                    byte[] imageBytes = Base64.getDecoder().decode(imageStr);
                    Image img = new Image(new ByteArrayInputStream(imageBytes));
                    imageView.setImage(img);
                } catch (Exception ignored) { }
            }

            VBox infoBox = new VBox(4);
            Label nameLabel = new Label(product.getName());
            nameLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: white;");

            Label priceLabel = new Label(product.formattedPrice());
            priceLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #1abc9c; -fx-font-weight: bold;");

            Label descLabel = new Label("Stok: " + product.getStock() + " | " + product.getDescription());
            descLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #8b9eb0;");
            descLabel.setWrapText(true);
            descLabel.setMaxWidth(260);

            infoBox.getChildren().addAll(nameLabel, priceLabel, descLabel);

            productCard.getChildren().addAll(imageView, infoBox);
            cardsContainer.getChildren().add(productCard);
        }

        int remaining = products.size() - displayed;
        if (remaining > 0) {
            Text moreText = new Text("... dan " + remaining + " produk lainnya.");
            moreText.setStyle("-fx-font-size: 14px; -fx-fill: #c2d6f6; -fx-font-style: italic;");
            cardsContainer.getChildren().add(moreText);
        }

        VBox bubbleContent = new VBox(cardsContainer);
        bubbleContent.getStyleClass().add("bubble-bot");
        bubbleContent.setPadding(new Insets(10, 14, 10, 14));

        Label timeLabel = new Label(time);
        timeLabel.getStyleClass().add("time-label");

        VBox content = new VBox(2, bubbleContent, timeLabel);

        StackPane avatar = new StackPane();
        avatar.getStyleClass().add("bot-msg-avatar");
        Label avLbl = new Label("A");
        avLbl.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #07161E;");
        avatar.getChildren().add(avLbl);
        avatar.setMinSize(30, 30);
        avatar.setMaxSize(30, 30);

        HBox row = new HBox(8);
        row.setPadding(new Insets(4, 20, 4, 20));
        row.setAlignment(Pos.CENTER_LEFT);
        row.getChildren().addAll(avatar, content);

        return row;
    }

    private JsonArray extractProducts(JsonElement source) {
        JsonElement normalized = normalizeJsonElement(source);
        if (normalized == null || normalized.isJsonNull()) {
            return null;
        }

        if (normalized.isJsonObject()) {
            JsonObject obj = normalized.getAsJsonObject();
            if (obj.has("products")) {
                return extractProducts(obj.get("products"));
            }
            return null;
        }

        if (!normalized.isJsonArray()) {
            return null;
        }

        JsonArray array = normalized.getAsJsonArray();
        if (array.isEmpty()) {
            return array;
        }

        return looksLikeProductPayload(array.get(0)) ? array : null;
    }

    private boolean looksLikeProductPayload(JsonElement element) {
        JsonElement normalized = normalizeJsonElement(element);
        if (normalized == null || normalized.isJsonNull()) {
            return false;
        }

        if (normalized.isJsonObject()) {
            JsonObject obj = normalized.getAsJsonObject();
            return obj.has("name") && obj.has("price") && obj.has("stock");
        }

        if (normalized.isJsonArray()) {
            JsonArray array = normalized.getAsJsonArray();
            return array.size() >= 4;
        }

        return false;
    }

    private Product parseProduct(JsonElement element) {
        JsonElement normalized = normalizeJsonElement(element);
        if (normalized == null || normalized.isJsonNull()) {
            return null;
        }

        if (normalized.isJsonObject()) {
            JsonObject productObj = normalized.getAsJsonObject();
            JsonElement codeElement = productObj.has("category")
                    ? productObj.get("category")
                    : productObj.get("gender");

            return new Product(
                    asString(productObj.get("id"), ""),
                    asString(productObj.get("name"), "Produk tanpa nama"),
                    asString(codeElement, "CARI"),
                    asLong(productObj.get("price"), 0L),
                    asInt(productObj.get("stock"), 0),
                    asString(productObj.get("description"), "-"),
                    asNullableString(productObj.get("image"))
            );
        }

        if (normalized.isJsonArray()) {
            JsonArray productArr = normalized.getAsJsonArray();
            return new Product(
                    asString(getArrayValue(productArr, 0), ""),
                    asString(getArrayValue(productArr, 1), "Produk tanpa nama"),
                    asString(getArrayValue(productArr, 6), "CARI"),
                    asLong(getArrayValue(productArr, 2), 0L),
                    asInt(getArrayValue(productArr, 3), 0),
                    asString(getArrayValue(productArr, 5), "-"),
                    asNullableString(getArrayValue(productArr, 4))
            );
        }

        return null;
    }

    private JsonObject asJsonObject(JsonElement element) {
        JsonElement normalized = normalizeJsonElement(element);
        return normalized != null && normalized.isJsonObject() ? normalized.getAsJsonObject() : null;
    }

    private JsonArray asJsonArray(JsonElement element) {
        JsonElement normalized = normalizeJsonElement(element);
        return normalized != null && normalized.isJsonArray() ? normalized.getAsJsonArray() : null;
    }

    private JsonElement normalizeJsonElement(JsonElement element) {
        if (element == null || element.isJsonNull()) {
            return null;
        }

        if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isString()) {
            String raw = element.getAsString().trim();
            if ((raw.startsWith("{") && raw.endsWith("}")) || (raw.startsWith("[") && raw.endsWith("]"))) {
                try {
                    return JsonParser.parseString(raw);
                } catch (Exception ignored) { }
            }
        }

        return element;
    }

    private JsonElement getArrayValue(JsonArray array, int index) {
        if (array == null || index < 0 || index >= array.size()) {
            return null;
        }
        return array.get(index);
    }

    private String asString(JsonElement element, String fallback) {
        JsonElement normalized = normalizeJsonElement(element);
        if (normalized == null || normalized.isJsonNull()) {
            return fallback;
        }

        try {
            return normalized.getAsString();
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private String asNullableString(JsonElement element) {
        String value = asString(element, null);
        return value == null || value.isBlank() ? null : value;
    }

    private long asLong(JsonElement element, long fallback) {
        JsonElement normalized = normalizeJsonElement(element);
        if (normalized == null || normalized.isJsonNull()) {
            return fallback;
        }

        try {
            return normalized.getAsLong();
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private int asInt(JsonElement element, int fallback) {
        JsonElement normalized = normalizeJsonElement(element);
        if (normalized == null || normalized.isJsonNull()) {
            return fallback;
        }

        try {
            return normalized.getAsInt();
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private HBox buildBubbleRow(String text, boolean isUser, String timeOverride) {
        String time = (timeOverride != null && !timeOverride.isBlank())
                ? timeOverride
                : LocalTime.now().format(TIME_FMT);

        Text msgText = new Text(text);
        msgText.setWrappingWidth(340);
        msgText.setFill(isUser
                ? javafx.scene.paint.Color.web("#07161E")
                : javafx.scene.paint.Color.web("#E8F0F3"));
        msgText.setStyle("-fx-font-size: 13px;");

        TextFlow flow = new TextFlow(msgText);
        flow.getStyleClass().add(isUser ? "bubble-user" : "bubble-bot");
        flow.setPadding(new Insets(10, 14, 10, 14));

        Label timeLabel = new Label(time);
        timeLabel.getStyleClass().add("time-label");

        VBox content = new VBox(2, flow, timeLabel);

        StackPane avatar = new StackPane();
        avatar.getStyleClass().add(isUser ? "msg-avatar" : "bot-msg-avatar");
        Label avLbl = new Label(isUser
                ? (Session.adminSelectedChatUsername != null && !Session.adminSelectedChatUsername.isBlank()
                    ? Session.adminSelectedChatUsername.substring(0, Math.min(2, Session.adminSelectedChatUsername.length())).toUpperCase()
                    : "US")
                : "A");
        avLbl.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #07161E;");
        avatar.getChildren().add(avLbl);
        avatar.setMinSize(30, 30);
        avatar.setMaxSize(30, 30);

        HBox row = new HBox(8);
        row.setPadding(new Insets(4, 20, 4, 20));
        if (isUser) {
            row.setAlignment(Pos.CENTER_RIGHT);
            timeLabel.setAlignment(Pos.CENTER_RIGHT);
            row.getChildren().addAll(content, avatar);
        } else {
            row.setAlignment(Pos.CENTER_LEFT);
            row.getChildren().addAll(avatar, content);
        }

        return row;
    }

    @FXML private void backToList() { stopAutoRefresh(); try { App.switchScene("admin_chat"); } catch (Exception ignored) {} }

    @FXML private void goOverview()     { stopAutoRefresh(); try { App.switchScene("admin_overview"); } catch (Exception ignored) {} }
    @FXML private void goProducts()     { stopAutoRefresh(); try { App.switchScene("admin_products"); } catch (Exception ignored) {} }
    @FXML private void goStoreInfo()    { stopAutoRefresh(); try { App.switchScene("admin_store"); } catch (Exception ignored) {} }
    @FXML private void goTransactions() { stopAutoRefresh(); try { App.switchScene("admin_transactions"); } catch (Exception ignored) {} }
    @FXML private void goChatHistory()  { stopAutoRefresh(); try { App.switchScene("admin_chat"); } catch (Exception ignored) {} }

    @FXML
    private void handleLogout() {
        stopAutoRefresh();
        Session.clear();
        try { App.switchScene("landing"); } catch (Exception ignored) {}
    }
}

