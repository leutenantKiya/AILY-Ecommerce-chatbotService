package com.aily.controller;

import com.aily.App;
import com.aily.model.Product;
import com.aily.service.ApiService;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

import java.io.ByteArrayInputStream;
import java.net.URL;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.ResourceBundle;

public class GuestChatController implements Initializable {

    @FXML private VBox messageContainer;
    @FXML private ScrollPane scrollPane;
    @FXML private TextField messageInput;
    @FXML private Button sendButton;

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH.mm");
    // Guest uses a fixed guest ID for NLP-only calls
    private static final String GUEST_USER_ID = "0";

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        messageInput.setOnAction(e -> handleSend());
        addBotMessage("Halo, Guest! 👋\nSaya AILY, siap membantu kamu.\nCoba ketik nama produk yang ingin kamu cari!");
    }

    @FXML
    private void handleSend() {
        String text = messageInput.getText().trim();
        if (text.isEmpty()) return;

        addUserMessage(text);
        messageInput.clear();
        sendButton.setDisable(true);

        new Thread(() -> {
            try {
                JsonObject response = ApiService.sendMessage(GUEST_USER_ID, text);
                Platform.runLater(() -> {
                    sendButton.setDisable(false);
                    if (response.has("status") && response.get("status").getAsInt() == 200) {
                        JsonObject data = asJsonObject(response.get("data"));
                        System.out.println("DEBUG Response: " + response);
                        System.out.println("DEBUG Data: " + data);
                        String intent = "";
                        if (data != null && data.has("nlp_result")) {
                            JsonObject nlp = asJsonObject(data.get("nlp_result"));
                            if (nlp != null) intent = asString(nlp.get("intent"), "");
                        }
                        if (data != null && data.has("action_data") && !data.get("action_data").isJsonNull()) {
                            JsonElement actionData = normalize(data.get("action_data"));
                            if (actionData != null && actionData.isJsonObject()) {
                                JsonObject actionObj = actionData.getAsJsonObject();
                                // Handle FAQ/bantuan result
                                if (actionObj.has("result")) {
                                    JsonArray result = actionObj.getAsJsonArray("result");
                                    if (result != null && !result.isEmpty()) {
                                        addBotMessage(formatFaqResult(result));
                                        return;
                                    }
                                }
                                // Handle product search
                                if (intent.equalsIgnoreCase("mencari")) {
                                    JsonArray products = extractProducts(actionData);
                                    if (products != null) { addBotProductMessage(products); return; }
                                }
                            }
                        }
                        addBotMessage(buildReply(data, intent));
                    } else {
                        addBotMessage("Maaf, terjadi kesalahan. Coba lagi.");
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    sendButton.setDisable(false);
                    addBotMessage("Tidak dapat terhubung ke server.");
                });
            }
        }).start();
    }

    private String formatFaqResult(JsonArray result) {
        StringBuilder sb = new StringBuilder("📋 Informasi Bantuan:\n\n");
        for (JsonElement e : result) {
            if (e.isJsonArray()) {
                JsonArray tuple = e.getAsJsonArray();
                if (tuple.size() >= 2) {
                    String topic = asString(tuple.get(0), "");
                    String answer = asString(tuple.get(1), "");
                    sb.append("• ").append(topic.toUpperCase()).append("\n");
                    sb.append("  ").append(answer).append("\n\n");
                }
            }
        }
        return sb.toString().trim();
    }

    private String buildReply(JsonObject data, String intent) {
        if (data != null) {
            // Cek response dari action_data dulu (untuk FAQ, bantuan, tentang toko)
            if (data.has("action_data") && !data.get("action_data").isJsonNull()) {
                JsonElement actionData = normalize(data.get("action_data"));
                if (actionData != null && actionData.isJsonObject()) {
                    JsonObject actionObj = actionData.getAsJsonObject();
                    String resp = asString(actionObj.get("response"), "");
                    if (!resp.isBlank()) return resp;
                }
            }
            // Fallback ke nlp_result
            if (data.has("nlp_result")) {
                JsonObject nlp = asJsonObject(data.get("nlp_result"));
                if (nlp != null) {
                    String resp = asString(nlp.get("respons"), "");
                    if (!resp.isBlank()) return resp;
                }
            }
        }
        return switch (intent) {
            case "mencari"        -> "Sebutkan nama produk yang ingin kamu cari!";
            case "salam"          -> "Halo! Ada yang bisa saya bantu?";
            case "terima_kasih"   -> "Sama-sama! 😊";
            case "selamat_tinggal"-> "Sampai jumpa!";
            default               -> "Maaf, saya belum paham. Coba ulangi dengan kata lain.";
        };
    }

    @FXML private void chipCariProduk() { messageInput.setText("carikan aku produk"); messageInput.requestFocus(); }
    @FXML private void chipFaq()        { messageInput.setText("informasi toko");      messageInput.requestFocus(); }
    @FXML private void chipPromo()      { messageInput.setText("ada promo apa hari ini"); messageInput.requestFocus(); }

    @FXML private void clearChat() {
        messageContainer.getChildren().clear();
        addBotMessage("Chat dibersihkan. Ada yang bisa saya bantu?");
    }

    @FXML private void goToLogin() { try { App.switchScene("login"); } catch (Exception ignored) {} }
    @FXML private void goBack()    { try { App.switchScene("landing"); } catch (Exception ignored) {} }

    // ── UI helpers ──────────────────────────────────────────────────────────

    private void addUserMessage(String text) {
        messageContainer.getChildren().add(buildBubble(text, true));
        scrollToBottom();
    }

    private void addBotMessage(String text) {
        messageContainer.getChildren().add(buildBubble(text, false));
        scrollToBottom();
    }

    private HBox buildBubble(String text, boolean isUser) {
        String time = LocalTime.now().format(TIME_FMT);
        Text msgText = new Text(text);
        msgText.setWrappingWidth(340);
        msgText.setFill(isUser ? Color.web("#07161E") : Color.web("#E8F0F3"));
        msgText.setStyle("-fx-font-size: 13px;");

        TextFlow flow = new TextFlow(msgText);
        flow.getStyleClass().add(isUser ? "bubble-user" : "bubble-bot");
        flow.setPadding(new Insets(10, 14, 10, 14));

        Label timeLabel = new Label(time);
        timeLabel.getStyleClass().add("time-label");

        VBox content = new VBox(2, flow, timeLabel);

        StackPane avatar = new StackPane();
        avatar.getStyleClass().add(isUser ? "msg-avatar" : "bot-msg-avatar");
        Label avLbl = new Label(isUser ? "G" : "A");
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

    private void addBotProductMessage(JsonArray products) {
        if (products.isEmpty()) { addBotMessage("Produk tidak ditemukan. Coba kata kunci lain."); return; }
        String time = LocalTime.now().format(TIME_FMT);
        VBox cards = new VBox(8);
        cards.getChildren().add(styledText("Ditemukan " + products.size() + " produk:\n", "#c2d6f6", true));

        int shown = 0;
        for (int i = 0; i < products.size() && shown < 5; i++) {
            Product p = parseProduct(products.get(i));
            if (p == null) continue;
            shown++;

            HBox card = new HBox(12);
            card.setAlignment(Pos.CENTER_LEFT);
            card.setPadding(new Insets(10));
            card.setStyle("-fx-background-color: #0d2232; -fx-background-radius: 8;");

            ImageView iv = new ImageView();
            iv.setFitWidth(100); iv.setFitHeight(100); iv.setPreserveRatio(false);
            if (p.getImage() != null) {
                try { iv.setImage(new Image(new ByteArrayInputStream(Base64.getDecoder().decode(p.getImage())))); }
                catch (Exception ignored) {}
            }

            Label name  = new Label(p.getName());  name.setStyle("-fx-font-size:14px;-fx-font-weight:bold;-fx-text-fill:white;");
            Label price = new Label(p.formattedPrice()); price.setStyle("-fx-font-size:13px;-fx-text-fill:#1abc9c;-fx-font-weight:bold;");
            Label desc  = new Label("Stok: " + p.getStock()); desc.setStyle("-fx-font-size:11px;-fx-text-fill:#8b9eb0;");

            VBox info = new VBox(4, name, price, desc);
            HBox.setHgrow(info, Priority.ALWAYS);
            card.getChildren().addAll(iv, info);
            cards.getChildren().add(card);
        }
        if (products.size() - shown > 0)
            cards.getChildren().add(styledText("... dan " + (products.size() - shown) + " produk lainnya.", "#c2d6f6", false));

        VBox bubble = new VBox(cards);
        bubble.getStyleClass().add("bubble-bot");
        bubble.setPadding(new Insets(10, 14, 10, 14));

        Label timeLabel = new Label(time); timeLabel.getStyleClass().add("time-label");
        VBox content = new VBox(2, bubble, timeLabel);

        StackPane avatar = new StackPane();
        avatar.getStyleClass().add("bot-msg-avatar");
        Label avLbl = new Label("A"); avLbl.setStyle("-fx-font-size:11px;-fx-font-weight:bold;-fx-text-fill:#07161E;");
        avatar.getChildren().add(avLbl); avatar.setMinSize(30,30); avatar.setMaxSize(30,30);

        HBox row = new HBox(8, avatar, content);
        row.setPadding(new Insets(4, 20, 4, 20));
        row.setAlignment(Pos.CENTER_LEFT);
        messageContainer.getChildren().add(row);
        scrollToBottom();
    }

    private Text styledText(String s, String color, boolean bold) {
        Text t = new Text(s);
        t.setStyle("-fx-font-size:14px;-fx-fill:" + color + ";" + (bold ? "-fx-font-weight:bold;" : ""));
        return t;
    }

    private void scrollToBottom() {
        Platform.runLater(() -> {
            messageContainer.layout();
            scrollPane.layout();
            Platform.runLater(() -> scrollPane.setVvalue(1.0));
        });
    }

    // ── JSON helpers ─────────────────────────────────────────────────────────

    private JsonArray extractProducts(JsonElement src) {
        JsonElement n = normalize(src);
        if (n == null || n.isJsonNull()) return null;
        if (n.isJsonObject()) {
            JsonObject o = n.getAsJsonObject();
            return o.has("products") ? extractProducts(o.get("products")) : null;
        }
        if (!n.isJsonArray()) return null;
        JsonArray arr = n.getAsJsonArray();
        if (arr.isEmpty()) return arr;
        return looksLikeProduct(arr.get(0)) ? arr : null;
    }

    private boolean looksLikeProduct(JsonElement e) {
        JsonElement n = normalize(e);
        if (n == null || n.isJsonNull()) return false;
        if (n.isJsonObject()) { JsonObject o = n.getAsJsonObject(); return o.has("name") && o.has("price"); }
        return n.isJsonArray() && n.getAsJsonArray().size() >= 4;
    }

    private Product parseProduct(JsonElement e) {
        JsonElement n = normalize(e);
        if (n == null || n.isJsonNull()) return null;
        if (n.isJsonObject()) {
            JsonObject o = n.getAsJsonObject();
            return new Product(asString(o.get("id"),""), asString(o.get("name"),"?"),
                    asString(o.get("gender"),""), asLong(o.get("price"),0),
                    asInt(o.get("stock"),0), asString(o.get("description"),"-"),
                    asNullable(o.get("image")));
        }
        if (n.isJsonArray()) {
            JsonArray a = n.getAsJsonArray();
            return new Product(asString(get(a,0),""), asString(get(a,1),"?"),
                    asString(get(a,6),""), asLong(get(a,2),0),
                    asInt(get(a,3),0), asString(get(a,5),"-"), asNullable(get(a,4)));
        }
        return null;
    }

    private JsonElement normalize(JsonElement e) {
        if (e == null || e.isJsonNull()) return null;
        if (e.isJsonPrimitive() && e.getAsJsonPrimitive().isString()) {
            String s = e.getAsString().trim();
            if ((s.startsWith("{") && s.endsWith("}")) || (s.startsWith("[") && s.endsWith("]"))) {
                try { return JsonParser.parseString(s); } catch (Exception ignored) {}
            }
        }
        return e;
    }

    private JsonObject asJsonObject(JsonElement e) {
        JsonElement n = normalize(e); return n != null && n.isJsonObject() ? n.getAsJsonObject() : null;
    }
    private String asString(JsonElement e, String fb) {
        JsonElement n = normalize(e); if (n == null || n.isJsonNull()) return fb;
        try { return n.getAsString(); } catch (Exception ignored) { return fb; }
    }
    private long asLong(JsonElement e, long fb) {
        JsonElement n = normalize(e); if (n == null || n.isJsonNull()) return fb;
        try { return n.getAsLong(); } catch (Exception ignored) { return fb; }
    }
    private int asInt(JsonElement e, int fb) {
        JsonElement n = normalize(e); if (n == null || n.isJsonNull()) return fb;
        try { return n.getAsInt(); } catch (Exception ignored) { return fb; }
    }
    private String asNullable(JsonElement e) {
        String s = asString(e, null); return (s == null || s.isBlank()) ? null : s;
    }
    private JsonElement get(JsonArray a, int i) {
        return (a == null || i < 0 || i >= a.size()) ? null : a.get(i);
    }
}
