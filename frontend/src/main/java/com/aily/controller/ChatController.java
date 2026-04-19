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

import java.net.URL;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;

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
            // initials for avatar
            avatarLabel.setText(user.getUsername().substring(0, Math.min(2, user.getUsername().length())).toUpperCase());
        }
        messageInput.setOnAction(e -> handleSend());
        addBotMessage("Selamat datang di AILY \uD83D\uDC4B\uD83D\uDE0A\nSaya siap membantu kamu berbelanja.\nCoba ketik nama produk yang ingin kamu cari!");
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
                        addBotMessage(buildBotReply(data, intent));
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
        handleSend();
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
        messageContainer.getChildren().clear();
        addBotMessage("Chat dibersihkan. Ada yang bisa saya bantu?");
    }

    // ── Bubble builders ───────────────────────────────────────────

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
