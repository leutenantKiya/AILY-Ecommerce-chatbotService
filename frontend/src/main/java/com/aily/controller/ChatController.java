package com.aily.controller;

import com.aily.App;
import com.aily.Session;
import com.aily.model.CartItem;
import com.aily.model.Product;
import com.aily.model.User;
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
import javafx.scene.layout.Priority;
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

public class ChatController implements Initializable {
    @FXML private VBox messageContainer;
    @FXML private ScrollPane scrollPane;
    @FXML private TextField messageInput;
    @FXML private Button sendButton;
    @FXML private Label usernameLabel;
    @FXML private Label roleLabel;
    @FXML private Label avatarLabel;
    @FXML private Label chatModeLabel;
    @FXML private Button toggleModeButton;

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH.mm");
    private static final String WELCOME_MESSAGE =
            "Selamat datang di AILY \uD83D\uDC4B\uD83D\uDE0A\n" +
            "Saya siap membantu kamu berbelanja.\n" +
            "Coba ketik nama produk yang ingin kamu cari!";

    private boolean adminChatMode = false;
    private Timeline autoRefreshTimer;
    private int lastMessageCount = 0;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        User user = Session.currentUser;
        if (user != null) {
            usernameLabel.setText(user.getUsername());
            roleLabel.setText(user.isAdmin() ? "Admin" : "Pembeli");
            avatarLabel.setText(user.getUsername()
                    .substring(0, Math.min(2, user.getUsername().length()))
                    .toUpperCase());

            new Thread(() -> loadChatHistory(user)).start();
        } else {
            showWelcomeMessage();
        }

        messageInput.setOnAction(e -> handleSend());
        updateModeUI();
        startAutoRefresh();
    }

    private void startAutoRefresh() {
        autoRefreshTimer = new Timeline(new KeyFrame(Duration.seconds(5), e -> refreshIfAdminMode()));
        autoRefreshTimer.setCycleCount(Timeline.INDEFINITE);
        autoRefreshTimer.play();
    }

    private void stopAutoRefresh() {
        if (autoRefreshTimer != null) {
            autoRefreshTimer.stop();
        }
    }

    private void refreshIfAdminMode() {
        if (!adminChatMode) return;
        User user = Session.currentUser;
        if (user == null) return;

        new Thread(() -> {
            try {
                JsonObject response = ApiService.getChatHistory(user.getId());
                String status = asString(response.get("status"), "");
                Platform.runLater(() -> {
                    if ("200".equals(status) || "ok".equalsIgnoreCase(status)) {
                        JsonObject data = asJsonObject(response.get("data"));
                        JsonArray chats = data == null ? null : asJsonArray(data.get("chat_history"));
                        int newCount = chats != null ? chats.size() : 0;
                        if (newCount > lastMessageCount) {
                            // Only append new messages starting from lastMessageCount
                            for (int i = lastMessageCount; i < newCount; i++) {
                                JsonObject chatObj = asJsonObject(chats.get(i));
                                if (chatObj == null) continue;
                                String role = asString(chatObj.get("role"), "");
                                JsonElement message = chatObj.get("message");
                                if ("bot".equalsIgnoreCase(role)) {
                                    renderBotHistoryMessage(message);
                                } else if (message != null && !message.isJsonNull()) {
                                    addUserMessage(asString(message, ""));
                                }
                            }
                            lastMessageCount = newCount;
                            scrollToBottom();
                        }
                    }
                });
            } catch (Exception ignored) {}
        }).start();
    }

    @FXML
    private void toggleChatMode() {
        adminChatMode = !adminChatMode;
        updateModeUI();
        messageContainer.getChildren().clear();
        lastMessageCount = 0;
        User user = Session.currentUser;
        if (user != null) {
            if (adminChatMode) {
                addBotMessage("Mode: Chat ke Admin.\nPesan kamu akan dikirim ke admin.");
            } else {
                new Thread(() -> loadChatHistory(user)).start();
            }
        }
    }

    private void updateModeUI() {
        if (chatModeLabel != null) {
            chatModeLabel.setText(adminChatMode ? "Mode: Admin" : "Mode: NLP Bot");
        }
        if (toggleModeButton != null) {
            toggleModeButton.setText(adminChatMode ? "💬 Chat ke NLP" : "👤 Chat ke Admin");
        }
    }

    private void loadChatHistory(User user) {
        try {
            JsonObject response = ApiService.getChatHistory(user.getId());
            String status = asString(response.get("status"), "");
            System.out.println(status);

            Platform.runLater(() -> {
                if ("200".equals(status) || "ok".equalsIgnoreCase(status)) {
                    JsonObject data = asJsonObject(response.get("data"));
                    JsonArray chats = data == null ? null : asJsonArray(data.get("chat_history"));

                    if (chats != null && !chats.isEmpty()) {
                        for (JsonElement chatElement : chats) {
                            JsonObject chatObj = asJsonObject(chatElement);
                            if (chatObj == null) {
                                continue;
                            }

                            String role = asString(chatObj.get("role"), "");
                            JsonElement message = chatObj.get("message");

                            if ("bot".equalsIgnoreCase(role)) {
                                renderBotHistoryMessage(message);
                            } else if (message != null && !message.isJsonNull()) {
                                addUserMessage(asString(message, ""));
                            }
                        }
                        lastMessageCount = chats.size();
                        scrollToBottom();
                    } else {
                        lastMessageCount = 0;
                        showWelcomeMessage();
                    }
                } else {
                    addBotMessage("Gagal memuat riwayat chat.");
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
            Platform.runLater(this::showWelcomeMessage);
        }
    }

    @FXML
    private void handleSend() {
        String text = messageInput.getText().trim();
        if (text.isEmpty()) {
            return;
        }

        addUserMessage(text);
        messageInput.clear();
        sendButton.setDisable(true);

        User user = Session.currentUser;
        if (user == null) {
            sendButton.setDisable(false);
            addBotMessage("Sesi habis. Silakan login ulang.");
            return;
        }

        if (adminChatMode) {
            // Send message to admin via saveChatMessage
            new Thread(() -> {
                try {
                    ApiService.saveChatMessage(user.getId(), user.getUsername(), "user", text);
                    Platform.runLater(() -> {
                        sendButton.setDisable(false);
                        lastMessageCount++; // track locally so refresh doesn't re-render immediately
                    });
                } catch (Exception e) {
                    Platform.runLater(() -> {
                        sendButton.setDisable(false);
                        addBotMessage("Gagal mengirim pesan ke admin.");
                    });
                }
            }).start();
            return;
        }

        // Normal NLP mode
        new Thread(() -> {
            try {
                JsonObject response = ApiService.sendMessage(user.getId(), text);
                Platform.runLater(() -> {
                    sendButton.setDisable(false);

                    if (response.has("status") && response.get("status").getAsInt() == 200) {
                        JsonObject data = asJsonObject(response.get("data"));
                        String intent = "";

                        if (data != null) {
                            JsonObject nlp = asJsonObject(data.get("nlp_result"));
                            if (nlp != null && nlp.has("intent")) {
                                intent = asString(nlp.get("intent"), "");
                            }
                        }

                        if (data != null
                                && intent.equalsIgnoreCase("mencari")
                                && data.has("action_data")
                                && !data.get("action_data").isJsonNull()) {
                            try {
                                JsonArray products = extractProducts(data.get("action_data"));
                                if (products != null) {
                                    addBotProductMessage(products);
                                } else {
                                    addBotMessage(buildBotReply(data, intent));
                                }
                            } catch (Exception ex) {
                                addBotMessage("Maaf, terjadi kesalahan saat menampilkan produk.");
                            }
                        } else {
                            addBotMessage(buildBotReply(data, intent));
                        }
                    } else {
                        String msg = response.has("error")
                                ? response.get("error").getAsString()
                                : "Maaf, terjadi kesalahan.";
                        addBotMessage(msg);
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    sendButton.setDisable(false);
                    addBotMessage("Tidak dapat terhubung ke server. Pastikan server berjalan.");
                });
            }
        }).start();
    }

    private String buildBotReply(JsonObject data, String intent) {
        if (intent != null && intent.equalsIgnoreCase("help")) {
            String formatted = formatHelpResponse(data);
            if (!formatted.isBlank()) {
                return formatted;
            }
        }

        if (data != null && data.has("action_data") && !data.get("action_data").isJsonNull()) {
            try {
                JsonArray products = extractProducts(data.get("action_data"));
                if (products != null) {
                    if (products.isEmpty()) {
                        return "Maaf, produk yang kamu cari tidak ditemukan. Coba kata kunci lain.";
                    }

                    StringBuilder sb = new StringBuilder();
                    sb.append("Ditemukan ").append(products.size()).append(" produk:\n\n");

                    int displayed = 0;
                    for (JsonElement productEl : products) {
                        if (displayed >= 5) {
                            break;
                        }

                        Product product = parseProduct(productEl);
                        if (product == null) {
                            continue;
                        }

                        displayed++;
                        sb.append(displayed).append(". ").append(product.getName())
                          .append("\n   Harga: ").append(product.formattedPrice())
                          .append(" | Stok: ").append(product.getStock()).append("\n");
                    }

                    int remaining = products.size() - displayed;
                    if (remaining > 0) {
                        sb.append("\n... dan ").append(remaining).append(" produk lainnya.");
                    }

                    return sb.toString();
                }

                JsonObject action = asJsonObject(data.get("action_data"));
                if (action != null) {
                    if (intent.equalsIgnoreCase("tanya_toko") || intent.equalsIgnoreCase("faq")) {
                        JsonObject specificData = asJsonObject(action.get("data"));
                        JsonArray pairResults = specificData == null ? null : asJsonArray(specificData.get("result"));
                        if (pairResults != null) {
                            if (pairResults.isEmpty()) {
                                return "Info toko belum tersedia.";
                            }
                            return formatPairResults(pairResults, "Informasi Toko AILY:");
                        }

                        JsonArray qaResults = asJsonArray(action.get("result"));
                        if (qaResults != null) {
                            if (qaResults.isEmpty()) {
                                return "Info toko belum tersedia.";
                            }
                            return formatQuestionAnswerResults(qaResults, "Informasi Toko AILY:");
                        }
                    }

                    if (action.has("message")) {
                        return asString(action.get("message"), "Pesan dikirim dari sistem.");
                    }
                }
            } catch (Exception e) {
                System.out.println("Error parsing action_data: " + e.getMessage());
            }
        }

        if (data != null && data.has("nlp_result") && !data.get("nlp_result").isJsonNull()) {
            JsonObject nlp = asJsonObject(data.get("nlp_result"));
            if (nlp != null && nlp.has("respons") && !asString(nlp.get("respons"), "").isEmpty()) {
                return asString(nlp.get("respons"), "");
            }
        }

        return switch (intent) {
            case "mencari" -> "Baik, saya akan bantu cari produk. Sebutkan nama atau kategori produknya!";
            case "checkout" -> "Mengarahkan ke proses checkout...";
            case "lacak_kiriman" -> "Silakan masukkan nomor resi untuk melacak kiriman kamu.";
            case "status_pesanan" -> "Saya akan periksa status pesanan kamu.";
            case "batal_pesanan" -> "Untuk membatalkan pesanan, masukkan nomor pesanan kamu.";
            case "faq", "tanya_toko" -> "Hubungi kami untuk info toko lebih lanjut.";
            case "salam" -> "Halo juga! Ada yang bisa saya bantu hari ini?";
            case "terima_kasih" -> "Sama-sama! Jangan ragu untuk bertanya lagi.";
            case "selamat_tinggal" -> "Sampai jumpa! Semoga belanja kamu menyenangkan.";
            case "tidak_diketahui" -> "Maaf, saya belum paham maksud kamu. Coba ulangi dengan kata lain.";
            default -> "Pesan kamu diterima! (intent: " + intent + ")";
        };
    }

    private String formatHelpResponse(JsonObject data) {
        if (data == null) {
            return "";
        }

        StringBuilder sb = new StringBuilder();

        JsonObject nlp = asJsonObject(data.get("nlp_result"));
        if (nlp != null) {
            String header = asString(nlp.get("respons"), "").trim();
            if (!header.isBlank()) {
                sb.append(header);
            }

            JsonArray konten = asJsonArray(nlp.get("konten"));
            if (konten != null && !konten.isEmpty()) {
                if (sb.length() > 0) sb.append("\n\n");
                sb.append("Menu bantuan:\n");
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
                if (idx > 0) {
                    // trim last newline
                    sb.setLength(sb.length() - 1);
                }
            }
        }

        // Optional: FAQ cepat dari action_data.data.result (pairs)
        JsonObject action = asJsonObject(data.get("action_data"));
        JsonObject actionData = action == null ? null : asJsonObject(action.get("data"));
        JsonArray pairs = actionData == null ? null : asJsonArray(actionData.get("result"));
        if (pairs != null && !pairs.isEmpty()) {
            if (sb.length() > 0) sb.append("\n\n");
            sb.append("FAQ cepat:\n");
            for (JsonElement pairEl : pairs) {
                JsonArray pair = asJsonArray(pairEl);
                if (pair == null || pair.size() < 2) continue;
                String key = asString(pair.get(0), "").trim();
                String val = asString(pair.get(1), "").trim();
                if (key.isBlank() && val.isBlank()) continue;
                sb.append("- ").append(key.isBlank() ? "-" : key).append(": ").append(val).append("\n");
            }
            // trim last newline
            if (sb.length() > 0 && sb.charAt(sb.length() - 1) == '\n') {
                sb.setLength(sb.length() - 1);
            }
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

    @FXML private void chipCariProduk() { sendChip("carikan aku baju"); }
    @FXML private void chipFaq() { sendChip("informasi toko"); }
    @FXML private void chipKeranjang() { sendChip("lihat keranjang"); }
    @FXML private void chipStatusPesanan() { sendChip("status pesanan saya"); }
    @FXML private void chipPromo() { sendChip("ada promo apa hari ini"); }

    private void sendChip(String text) {
        messageInput.setText(text);
        messageInput.requestFocus();
        messageInput.end();
    }

    @FXML private void showChat() { }
    @FXML private void showCart() { stopAutoRefresh(); try { App.switchScene("cart"); } catch (Exception ignored) { } }
    @FXML private void showOrders() { stopAutoRefresh(); try { App.switchScene("orders"); } catch (Exception ignored) { } }
    @FXML private void showProfile() { stopAutoRefresh(); try { App.switchScene("profile"); } catch (Exception ignored) { } }

    @FXML
    private void handleLogout() {
        stopAutoRefresh();
        Session.clear();
        try {
            App.switchScene("landing");
        } catch (Exception ignored) { }
    }

    @FXML
    private void clearChat() {
        User user = Session.currentUser;
        if (user != null) {
            new Thread(() -> {
                try {
                    ApiService.deleteChat(user.getId());
                    Platform.runLater(() -> {
                        messageContainer.getChildren().clear();
                        addBotMessage("Chat berhasil dibersihkan dari server. Ada yang bisa saya bantu?");
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                    Platform.runLater(() -> addBotMessage("Gagal menghapus chat dari server."));
                }
            }).start();
        } else {
            messageContainer.getChildren().clear();
            addBotMessage("Chat dibersihkan. Ada yang bisa saya bantu?");
        }
    }

    private void addBotProductMessage(JsonArray products) {
        if (products.isEmpty()) {
            addBotMessage("Maaf, produk yang kamu cari tidak ditemukan. Coba kata kunci lain.");
            return;
        }

        messageContainer.getChildren().add(buildProductBubbleRow(products));
        scrollToBottom();
    }

    private HBox buildProductBubbleRow(JsonArray products) {
        String time = LocalTime.now().format(TIME_FMT);

        VBox cardsContainer = new VBox(8);

        Text headerText = new Text("Ditemukan " + products.size() + " produk:\n");
        headerText.setStyle("-fx-font-size: 16px; -fx-fill: #c2d6f6; -fx-font-weight: bold;");
        cardsContainer.getChildren().add(headerText);

        int displayed = 0;
        for (int i = 0; i < products.size() && displayed < 5; i++) {
            JsonElement productEl = products.get(i);
            try {
                Product product = parseProduct(productEl);
                if (product == null) {
                    continue;
                }

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
                    } catch (Exception imgEx) {
                        System.out.println("Gagal decode gambar produk: " + product.getName());
                    }
                }

                VBox infoBox = new VBox(4);
                Label nameLabel = new Label(product.getName());
                nameLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: white;");

                Label priceLabel = new Label(product.formattedPrice());
                priceLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #1abc9c; -fx-font-weight: bold;");

                Label descLabel = new Label("Stok: " + product.getStock() + " | " + product.getDescription());
                descLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #8b9eb0;");
                descLabel.setWrapText(true);
                descLabel.setMaxWidth(200);

                infoBox.getChildren().addAll(nameLabel, priceLabel, descLabel);
                HBox.setHgrow(infoBox, Priority.ALWAYS);

                Button addToCartBtn = new Button(" + Keranjang ");
                addToCartBtn.setStyle("-fx-background-color: #1abc9c; -fx-text-fill: #07161E; -fx-font-weight: bold; -fx-background-radius: 4; -fx-cursor: hand;");
                addToCartBtn.setOnAction(e -> addProductToCart(product, addToCartBtn));

                productCard.getChildren().addAll(imageView, infoBox, addToCartBtn);
                cardsContainer.getChildren().add(productCard);
            } catch (Exception e) {
                System.out.println("Error parsing produk index " + i + ": " + e.getMessage());
            }
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

    private void addUserMessage(String text) {
        messageContainer.getChildren().add(buildBubbleRow(text, true));
        scrollToBottom();
    }

    private void addBotMessage(String text) {
        messageContainer.getChildren().add(buildBubbleRow(text, false));
        scrollToBottom();
    }

    private HBox buildBubbleRow(String text, boolean isUser) {
        String time = LocalTime.now().format(TIME_FMT);

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
                ? (Session.currentUser != null
                    ? Session.currentUser.getUsername()
                        .substring(0, Math.min(2, Session.currentUser.getUsername().length()))
                        .toUpperCase()
                    : "ME")
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

    private void scrollToBottom() {
        Platform.runLater(() -> scrollPane.setVvalue(1.0));
    }

    private void showWelcomeMessage() {
        addBotMessage(WELCOME_MESSAGE);
    }

    private void renderBotHistoryMessage(JsonElement messageElement) {
        JsonElement normalizedMessage = normalizeJsonElement(messageElement);
        if (normalizedMessage == null || normalizedMessage.isJsonNull()) {
            addBotMessage("Pesanan / info diproses.");
            return;
        }

        JsonArray products = extractProducts(normalizedMessage);
        if (products != null) {
            addBotProductMessage(products);
            return;
        }

        JsonObject messageObject = asJsonObject(normalizedMessage);
        if (messageObject != null) {
            JsonArray nestedProducts = extractProducts(messageObject.get("products"));
            if (nestedProducts != null) {
                addBotProductMessage(nestedProducts);
                return;
            }

            JsonObject dataObject = asJsonObject(messageObject.get("data"));
            JsonArray pairResults = dataObject == null ? null : asJsonArray(dataObject.get("result"));
            if (pairResults != null && !pairResults.isEmpty()) {
                addBotMessage(formatPairResults(pairResults, null));
                return;
            }

            if (messageObject.has("message")) {
                addBotMessage(asString(messageObject.get("message"), "Pesan dikirim dari sistem."));
                return;
            }

            addBotMessage("Pesan dikirim dari sistem.");
            return;
        }

        if (normalizedMessage.isJsonArray()) {
            addBotMessage(normalizedMessage.toString());
            return;
        }

        addBotMessage(asString(normalizedMessage, "Pesan dikirim dari sistem."));
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

    private String formatPairResults(JsonArray results, String header) {
        StringBuilder sb = new StringBuilder();
        if (header != null && !header.isBlank()) {
            sb.append(header).append("\n\n");
        }

        for (JsonElement resultEl : results) {
            JsonArray pair = asJsonArray(resultEl);
            if (pair == null || pair.size() < 2) {
                continue;
            }

            sb.append("- ")
              .append(asString(pair.get(0), "-"))
              .append(": ")
              .append(asString(pair.get(1), "-"))
              .append("\n");
        }

        return sb.toString().trim();
    }

    private String formatQuestionAnswerResults(JsonArray results, String header) {
        StringBuilder sb = new StringBuilder();
        if (header != null && !header.isBlank()) {
            sb.append(header).append("\n\n");
        }

        for (JsonElement resultEl : results) {
            JsonObject qa = asJsonObject(resultEl);
            if (qa == null) {
                continue;
            }

            sb.append("- ")
              .append(asString(qa.get("question"), "-"))
              .append(": ")
              .append(asString(qa.get("answer"), "-"))
              .append("\n");
        }

        return sb.toString().trim();
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

    private void addProductToCart(Product product, Button addToCartBtn) {
        addProductToLocalSessionCart(product, 1);
        markCartButtonAdded(addToCartBtn);

        if (Session.currentUser == null) {
            return;
        }

        new Thread(() -> {
            try {
                ApiService.addToCart(Session.currentUser.getId(), Integer.parseInt(product.getId()), 1);
            } catch (Exception ex) {
                Platform.runLater(() -> addBotMessage("Produk masuk ke keranjang lokal, tapi sinkronisasi ke server gagal."));
            }
        }).start();
    }

    private void addProductToLocalSessionCart(Product product, int quantity) {
        for (CartItem item : Session.cart) {
            if (item.getProduct().getId().equals(product.getId())) {
                item.setQuantity(item.getQuantity() + quantity);
                return;
            }
        }

        Session.cart.add(new CartItem(product, quantity));
    }

    private void markCartButtonAdded(Button addToCartBtn) {
        addToCartBtn.setText("Ditambahkan \u2713");
        addToCartBtn.setStyle("-fx-background-color: #2ecc71; -fx-text-fill: #07161E; -fx-font-weight: bold; -fx-background-radius: 4;");
    }
}
