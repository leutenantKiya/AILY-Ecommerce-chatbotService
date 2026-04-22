package com.aily.controller;

import com.aily.App;
import com.aily.Session;
import com.aily.model.User;
import com.aily.service.ApiService;
import com.google.gson.JsonObject;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import java.io.ByteArrayInputStream;
import java.util.Arrays;
import java.util.Base64;

import java.net.URL;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;

import javax.swing.ImageIcon;

public class ChatController implements Initializable {
    @FXML private VBox  messageContainer;
    @FXML private ScrollPane scrollPane;
    @FXML private TextField  messageInput;
    @FXML private Button     sendButton;
    @FXML private Label      usernameLabel;
    @FXML private Label      roleLabel;
    @FXML private Label      avatarLabel;

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH.mm");


    @Override
    public void initialize(URL url, ResourceBundle rb) {
        User user = Session.currentUser;
        if (user != null) {
            usernameLabel.setText(user.getUsername());
            roleLabel.setText(user.isAdmin() ? "Admin" : "Pembeli");
            avatarLabel.setText(user.getUsername().substring(0, Math.min(2, user.getUsername().length())).toUpperCase());

            new Thread(() -> {
                try {
                    JsonObject response = ApiService.getChatHistory(user.getId());
                    String status = response.get("status").getAsString();
                    System.out.println(status);
                    Platform.runLater(() -> {
                        if ("200".equals(status) || "ok".equalsIgnoreCase(status)) {
                            JsonObject data = response.getAsJsonObject("data");
                            JsonArray chats = data.getAsJsonArray("chat_history");
                            if (chats != null && chats.size() > 0) {
                                for (int i = 0; i < chats.size(); i++) {
                                    JsonObject chatObj = chats.get(i).getAsJsonObject();
                                    String role = chatObj.get("role").getAsString();
                                    
                                    if ("bot".equalsIgnoreCase(role)) {
                                        if (chatObj.get("message").isJsonNull()) {
                                            addBotMessage("Pesanan / info diproses.");
                                        } else if (chatObj.get("message").isJsonArray()) {
                                            addBotProductMessage(chatObj.getAsJsonArray("message"));
                                        } else if (chatObj.get("message").isJsonObject()) {
                                            JsonObject msgObj = chatObj.getAsJsonObject("message");
                                            if (msgObj.has("data") && msgObj.getAsJsonObject("data").has("result")) {
                                                JsonArray results = msgObj.getAsJsonObject("data").getAsJsonArray("result");
                                                StringBuilder sb = new StringBuilder();
                                                for (int j = 0; j < results.size(); j++) {
                                                    try {
                                                        JsonArray item = results.get(j).getAsJsonArray();
                                                        sb.append("- ").append(item.get(0).getAsString()).append(": ").append(item.get(1).getAsString()).append("\n");
                                                    }catch(Exception ex) {}
                                                }
                                                addBotMessage(sb.toString());
                                            } else if (msgObj.has("message")) {
                                                addBotMessage(msgObj.get("message").getAsString());
                                            } else {
                                                addBotMessage("Pesan dikirim dari sistem.");
                                            }
                                        } else {
                                            addBotMessage(chatObj.get("message").getAsString());
                                        }
                                    } else {
                                        if (!chatObj.get("message").isJsonNull()) {
                                            addUserMessage(chatObj.get("message").getAsString());
                                        }
                                    }
                                }
                                scrollToBottom();
                            } else {
                                addBotMessage("Selamat datang di AILY \uD83D\uDC4B\uD83D\uDE0A\nSaya siap membantu kamu berbelanja.\nCoba ketik nama produk yang ingin kamu cari!");
                            }
                        } else {
                            addBotMessage("Gagal memuat riwayat chat.");
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                    Platform.runLater(() -> {
                        addBotMessage("Selamat datang di AILY \uD83D\uDC4B\uD83D\uDE0A\nSaya siap membantu kamu berbelanja.\nCoba ketik nama produk yang ingin kamu cari!");
                    });
                }
            }).start();
            
        } else {
            addBotMessage("Selamat datang di AILY \uD83D\uDC4B\uD83D\uDE0A\nSaya siap membantu kamu berbelanja.\nCoba ketik nama produk yang ingin kamu cari!");
        }
        messageInput.setOnAction(e -> handleSend());
    }

    // ── Send ──────────────────────────────────────────────────────

    @FXML
    private void handleSend() {
        String text = messageInput.getText().trim();
        if (text.isEmpty()) return;
        addUserMessage(text);
        messageInput.clear();
        sendButton.setDisable(true);

        User user = Session.currentUser;
        if (user == null) { addBotMessage("Sesi habis. Silakan login ulang."); return; }

        new Thread(() -> {
            try {
                JsonObject response = ApiService.sendMessage(user.getId(), text);
                Platform.runLater(() -> {
                    sendButton.setDisable(false);
                    if (response.has("status") && response.get("status").getAsInt() == 200) {
                        JsonObject data = response.getAsJsonObject("data");
                        String intent = "";
                        if (data.has("nlp_result") && !data.get("nlp_result").isJsonNull()) {
                            JsonObject nlp = data.getAsJsonObject("nlp_result");
                            if (nlp.has("intent")) intent = nlp.get("intent").getAsString();
                        }
                        // Intent "mencari" → tampilkan produk dengan gambar
                        if (intent.equalsIgnoreCase("mencari") && data.has("action_data") && !data.get("action_data").isJsonNull()) {
                            try {
                                JsonArray products = data.getAsJsonArray("action_data");
                                addBotProductMessage(products);
                            } catch (Exception ex) {
                                addBotMessage("Maaf, terjadi kesalahan saat menampilkan produk.");
                            }
                        } else {
                            addBotMessage(buildBotReply(data, intent));
                        }
                    } else {
                        String msg = response.has("error")
                                ? response.get("error").getAsString() : "Maaf, terjadi kesalahan.";
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
        // Coba parse action_data dari backend
        if (data.has("action_data") && !data.get("action_data").isJsonNull()) {
            try {
<<<<<<< Updated upstream
                JsonObject action = data.getAsJsonObject("action_data");

                // Intent "mencari" → action_data.products = [...]
                if (action.has("products")) {
                    var products = action.getAsJsonArray("products");
                    if (products.isEmpty()) {
                        return "Maaf, produk yang kamu cari tidak ditemukan. Coba kata kunci lain.";
                    }
                    StringBuilder sb = new StringBuilder();
                    sb.append("Ditemukan ").append(products.size()).append(" produk:\n\n");
                    for (int i = 0; i < Math.min(products.size(), 5); i++) {
                        JsonObject p = products.get(i).getAsJsonObject();
                        String name = p.get("name").getAsString();
                        long price = p.get("price").getAsLong();
                        int stock = p.get("stock").getAsInt();
                        String formatted = String.format("Rp %,d", price).replace(',', '.');
                        sb.append(i + 1).append(". ").append(name)
                          .append("\n   Harga: ").append(formatted)
                          .append(" | Stok: ").append(stock).append("\n");
=======
                try{
                    // Intent "mencari" sudah di-handle di handleSend → addBotProductMessage
                    JsonArray action = data.getAsJsonArray("action_data");
                }catch (Exception e){
                    JsonObject action = data.getAsJsonObject("action_data");
                    // Intent "tanya_toko"/"faq" -> action_data.result = [{question, answer}, ...]
                    if (intent.equalsIgnoreCase("tanya_toko")) {
                        System.out.println("masuk FAQ");

                        JsonObject spesificData = action.getAsJsonObject("data");
                        JsonArray results = spesificData.getAsJsonArray("result");

                        if (results.isEmpty()) {
                            return "Info toko belum tersedia.";
                        }

                        StringBuilder sb = new StringBuilder();
                        sb.append("Informasi Toko AILY:\n\n");
                        for (int i = 0; i < results.size(); i++) {
                            JsonArray item = results.get(i).getAsJsonArray(); // tiap item = ["key", "value"]
                            String key = item.get(0).getAsString();
                            String value = item.get(1).getAsString();

                            sb.append("- ").append(key).append(": ").append(value).append("\n");
//                        System.out.println(key + ": " + value);
                        }

                        return sb.toString();
>>>>>>> Stashed changes
                    }
                    if (products.size() > 5) {
                        sb.append("\n... dan ").append(products.size() - 5).append(" produk lainnya.");
                    }
                    return sb.toString();
                }

                // Intent "tanya_toko"/"faq" -> action_data.result = [{question, answer}, ...]
                if (action.has("result") && action.get("result").isJsonArray()) {
                    var results = action.getAsJsonArray("result");
                    if (results.isEmpty()) {
                        return "Info toko belum tersedia.";
                    }
                    StringBuilder sb = new StringBuilder();
                    sb.append("Informasi Toko AILY:\n\n");
                    for (int i = 0; i < results.size(); i++) {
                        JsonObject qa = results.get(i).getAsJsonObject();
                        String q = qa.get("question").getAsString();
                        String a = qa.get("answer").getAsString();
                        sb.append("- ").append(q).append(": ").append(a).append("\n");
                    }
                    return sb.toString();
                }

                // Fallback: action_data.message (untuk intent lainnya)
                if (action.has("message")) {
                    return action.get("message").getAsString();
                }
            } catch (Exception e) {
                // action_data mungkin bukan JsonObject (misal array kosong)
            }
        }

        // Gunakan respons NLP jika ada
        if (data.has("nlp_result") && !data.get("nlp_result").isJsonNull()) {
            JsonObject nlp = data.getAsJsonObject("nlp_result");
            if (nlp.has("respons") && !nlp.get("respons").getAsString().isEmpty()) {
                return nlp.get("respons").getAsString();
            }
        }

        // Fallback terakhir berdasarkan intent
        return switch (intent) {
            case "mencari"         -> "Baik, saya akan bantu cari produk. Sebutkan nama atau kategori produknya!";
            case "checkout"        -> "Mengarahkan ke proses checkout...";
            case "lacak_kiriman"   -> "Silakan masukkan nomor resi untuk melacak kiriman kamu.";
            case "status_pesanan"  -> "Saya akan periksa status pesanan kamu.";
            case "batal_pesanan"   -> "Untuk membatalkan pesanan, masukkan nomor pesanan kamu.";
            case "faq","tanya_toko"-> "Hubungi kami untuk info toko lebih lanjut.";
            case "salam"           -> "Halo juga! Ada yang bisa saya bantu hari ini?";
            case "terima_kasih"    -> "Sama-sama! Jangan ragu untuk bertanya lagi.";
            case "selamat_tinggal" -> "Sampai jumpa! Semoga belanja kamu menyenangkan.";
            case "tidak_diketahui" -> "Maaf, saya belum paham maksud kamu. Coba ulangi dengan kata lain.";
            default                -> "Pesan kamu diterima! (intent: " + intent + ")";
        };
    }
    
    // ── Chips ─────────────────────────────────────────────────────

    @FXML private void chipCariProduk()    { sendChip("carikan aku baju"); }
    @FXML private void chipFaq()           { sendChip("informasi toko"); }
    @FXML private void chipKeranjang()     { sendChip("lihat keranjang"); }
    @FXML private void chipStatusPesanan() { sendChip("status pesanan saya"); }
    @FXML private void chipPromo()         { sendChip("ada promo apa hari ini"); }

    private void sendChip(String text) {
        messageInput.setText(text);
        messageInput.requestFocus();
        messageInput.end();
//        messageInput.setText(text);
    }

    // ── Nav ───────────────────────────────────────────────────────

    @FXML private void showChat()   { /* already on chat */ }
    @FXML private void showCart()   { try { App.switchScene("cart",   1280, 880); } catch (Exception ignored) {} }
    @FXML private void showOrders() { try { App.switchScene("orders", 1280, 880); } catch (Exception ignored) {} }

    @FXML
    private void handleLogout() {
        Session.clear();
        try { App.switchScene("landing", 1000, 700); } catch (Exception ignored) {}
    }

    @FXML
    private void clearChat() {
        User user = Session.currentUser;
        if (user != null) {
            new Thread(() -> {
                try {
                    JsonObject response = ApiService.deleteChat(user.getId());
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

    // ── Bubble builders ───────────────────────────────────────────

    /**
     * Menampilkan pesan bot khusus untuk hasil pencarian produk, lengkap dengan gambar.
     */
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

        // Container utama untuk semua kartu produk
        VBox cardsContainer = new VBox(8);

        // Header text
        Text headerText = new Text("Ditemukan " + products.size() + " produk:\n");
        headerText.setStyle("-fx-font-size: 16px; -fx-fill: #c2d6f6; -fx-font-weight: bold;");
        cardsContainer.getChildren().add(headerText);

        for (int i = 0; i < Math.min(products.size(), 5); i++) {
            try {
                JsonArray p = products.get(i).getAsJsonArray();

                // [id, name, price, stock, image, desc, gender]
                String idStr = p.get(0).getAsString();
                String name  = p.get(1).getAsString();
                long   price = p.get(2).getAsLong();
                int    stock = p.get(3).getAsInt();
                String desc = p.get(5).isJsonNull() ? "-" : p.get(5).getAsString();
                String formatted = String.format("Rp %,d", price).replace(',', '.');

                HBox productCard = new HBox(12);
                productCard.setAlignment(Pos.CENTER_LEFT);
                productCard.setPadding(new Insets(10));
                productCard.setStyle("-fx-background-color: #0d2232; -fx-background-radius: 8;"); 

                // Decode dan tampilkan gambar
                String imageStr = p.get(4).isJsonNull() ? null : p.get(4).getAsString();
                ImageView imageView = new ImageView();
                imageView.setFitWidth(150);
                imageView.setFitHeight(150);
                imageView.setPreserveRatio(false);
                if (imageStr != null && !imageStr.isEmpty()) {
                    try {
                        byte[] imageBytes = Base64.getDecoder().decode(imageStr);
                        Image img = new Image(new ByteArrayInputStream(imageBytes));
                        imageView.setImage(img);
                    } catch (Exception imgEx) {
                        System.out.println("Gagal decode gambar produk: " + name);
                    }
                }

                // Info teks produk
                VBox infoBox = new VBox(4);
                Label nameLabel = new Label(name);
                nameLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: white;");
                Label priceLabel = new Label(formatted);
                priceLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #1abc9c; -fx-font-weight: bold;");
                Label descLabel = new Label("Stok: " + stock + " | " + desc);
                descLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #8b9eb0;");
                descLabel.setWrapText(true);
                descLabel.setMaxWidth(200);
                infoBox.getChildren().addAll(nameLabel, priceLabel, descLabel);
                HBox.setHgrow(infoBox, Priority.ALWAYS);

                Button addToCartBtn = new Button(" + Keranjang ");
                addToCartBtn.setStyle("-fx-background-color: #1abc9c; -fx-text-fill: #07161E; -fx-font-weight: bold; -fx-background-radius: 4; -fx-cursor: hand;");
                addToCartBtn.setOnAction(e -> {
                    com.aily.model.Product prod = new com.aily.model.Product(idStr, name, "CARI", price, stock, desc, imageStr);
                    boolean found = false;
                    for (com.aily.model.CartItem item : Session.cart) {
                        if (item.getProduct().getId().equals(prod.getId())) {
                            item.setQuantity(item.getQuantity() + 1);
                            found = true;
                            break;
                        }
                    }
                    if (!found) {
                        Session.cart.add(new com.aily.model.CartItem(prod, 1));
                    }
                    addToCartBtn.setText("Ditambahkan ✓");
                    addToCartBtn.setStyle("-fx-background-color: #2ecc71; -fx-text-fill: #07161E; -fx-font-weight: bold; -fx-background-radius: 4;");
                });

                productCard.getChildren().addAll(infoBox, addToCartBtn);
                cardsContainer.getChildren().add(imageView);
                cardsContainer.getChildren().add(productCard);
            } catch (Exception e) {
                System.out.println("Error parsing produk index " + i + ": " + e.getMessage());
            }
        }

        if (products.size() > 5) {
            Text moreText = new Text("... dan " + (products.size() - 5) + " produk lainnya.");
            moreText.setStyle("-fx-font-size: 14px; -fx-fill: #c2d6f6; -fx-font-style: italic;");
            cardsContainer.getChildren().add(moreText);
        }

        // Bungkus dalam VBox dengan style bubble-bot
        VBox bubbleContent = new VBox(cardsContainer);
        bubbleContent.getStyleClass().add("bubble-bot");
        bubbleContent.setPadding(new Insets(10, 14, 10, 14));

        Label timeLabel = new Label(time);
        timeLabel.getStyleClass().add("time-label");

        VBox content = new VBox(2, bubbleContent, timeLabel);

        // Avatar
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
        // Explicitly set text color — CSS inheritance is unreliable for Text in TextFlow
        msgText.setFill(isUser ? javafx.scene.paint.Color.web("#07161E") : javafx.scene.paint.Color.web("#E8F0F3"));
        msgText.setStyle("-fx-font-size: 13px;");

        TextFlow flow = new TextFlow(msgText);
        flow.getStyleClass().add(isUser ? "bubble-user" : "bubble-bot");
        flow.setPadding(new Insets(10, 14, 10, 14));

        Label timeLabel = new Label(time);
        timeLabel.getStyleClass().add("time-label");

        VBox content = new VBox(2, flow, timeLabel);

        // Avatar circle
        StackPane avatar = new StackPane();
        avatar.getStyleClass().add(isUser ? "msg-avatar" : "bot-msg-avatar");
        Label avLbl = new Label(isUser
                ? (Session.currentUser != null ? Session.currentUser.getUsername().substring(0, Math.min(2, Session.currentUser.getUsername().length())).toUpperCase() : "ME")
                : "A");
        avLbl.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #07161E;");
        avatar.getChildren().add(avLbl);
        avatar.setMinSize(30, 30);
        avatar.setMaxSize(30, 30);

        HBox row = new HBox(8);
        row.setPadding(new Insets(4, 20, 4, 20));
        if (isUser) {
            row.setAlignment(Pos.CENTER_RIGHT);
            timeLabel.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);
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
}
