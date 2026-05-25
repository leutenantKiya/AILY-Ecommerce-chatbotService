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
    private static final String GUEST_USER_ID = "0";
    private static final String WELCOME_MESSAGE =
            "Halo, Guest! \uD83D\uDC4B\uD83D\uDE0A\n" +
            "Saya AILY, siap membantu kamu.\n" +
            "Coba ketik nama produk yang ingin kamu cari!";

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        messageInput.setOnAction(e -> handleSend());
        showWelcomeMessage();
    }

    private void showWelcomeMessage() {
        addBotMessage(WELCOME_MESSAGE);
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
                    addBotMessage("Tidak dapat terhubung ke server.");
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
                JsonObject action = asJsonObject(data.get("action_data"));
                if (action != null) {
                    if (intent.equalsIgnoreCase("tanya_toko") || intent.equalsIgnoreCase("faq")) {
                        JsonArray resultsArray = null;
                        if (action.has("result")) {
                            resultsArray = asJsonArray(action.get("result"));
                        } else if (action.has("data")) {
                            JsonObject specificData = asJsonObject(action.get("data"));
                            if (specificData != null && specificData.has("result")) {
                                resultsArray = asJsonArray(specificData.get("result"));
                            }
                        }

                        if (resultsArray != null) {
                            if (resultsArray.isEmpty()) {
                                return "Info toko belum tersedia.";
                            }

                            String header = intent.equalsIgnoreCase("faq") 
                                    ? "Berikut Layanan Yang Kami Sediakan:" 
                                    : "Berikut Informasi Toko Aily:";

                            if (resultsArray.get(0).isJsonArray()) {
                                return formatPairResults(resultsArray, header);
                            } else if (resultsArray.get(0).isJsonObject()) {
                                return formatQuestionAnswerResults(resultsArray, header);
                            }
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
            case "salam" -> "Halo juga! Ada yang bisa saya bantu hari ini?";
            case "terima_kasih" -> "Sama-sama! Jangan ragu untuk bertanya lagi.";
            case "selamat_tinggal" -> "Sampai jumpa! Semoga harimu menyenangkan.";
            case "tidak_diketahui" -> "Maaf, saya belum paham maksud kamu. Coba ulangi dengan kata lain.";
            default -> "Pesan kamu diterima! (intent: " + intent + ")";
        };
    }

    private String formatHelpResponse(JsonObject data) {
        if (data == null) return "";
        StringBuilder sb = new StringBuilder();
        JsonObject nlp = asJsonObject(data.get("nlp_result"));
        if (nlp != null) {
            String header = asString(nlp.get("respons"), "").trim();
            if (!header.isBlank()) sb.append(header);
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
                    if (!desc.isBlank()) sb.append(" \u2014 ").append(desc);
                    sb.append("\n");
                }
                if (idx > 0) sb.setLength(sb.length() - 1);
            }
        }
        return sb.toString().trim();
    }

    private String formatPairResults(JsonArray results, String header) {
        StringBuilder sb = new StringBuilder();
        if (header != null && !header.isBlank()) sb.append(header).append("\n\n");
        for (JsonElement resultEl : results) {
            JsonArray pair = asJsonArray(resultEl);
            if (pair == null || pair.size() < 2) continue;
            sb.append("- ").append(asString(pair.get(0), "-")).append(": ").append(asString(pair.get(1), "-")).append("\n");
        }
        return sb.toString().trim();
    }

    private String formatQuestionAnswerResults(JsonArray results, String header) {
        StringBuilder sb = new StringBuilder();
        if (header != null && !header.isBlank()) sb.append(header).append("\n\n");
        for (JsonElement resultEl : results) {
            JsonObject qa = asJsonObject(resultEl);
            if (qa == null) continue;
            sb.append("- ").append(asString(qa.get("question"), "-")).append(": ").append(asString(qa.get("answer"), "-")).append("\n");
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

    @FXML private void chipCariProduk() { sendChip("carikan aku kaos"); }
    @FXML private void chipFaq() { sendChip("informasi toko"); }
    @FXML private void chipPromo() { sendChip("ada promo apa hari ini"); }

    private void sendChip(String text) {
        messageInput.setText(text);
        messageInput.requestFocus();
        messageInput.end();
    }

    @FXML private void goToLogin() { try { App.switchScene("login"); } catch (Exception ignored) {} }
    @FXML private void goBack() { try { App.switchScene("landing"); } catch (Exception ignored) {} }

    @FXML
    private void clearChat() {
        messageContainer.getChildren().clear();
        addBotMessage("Chat dibersihkan. Ada yang bisa saya bantu?");
    }

    private void addBotProductMessage(JsonArray products) {
        if (products.isEmpty()) {
            addBotMessage("Maaf, produk yang kamu cari tidak ditemukan.");
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
            Product product = parseProduct(products.get(i));
            if (product == null) continue;
            displayed++;

            HBox productCard = new HBox(12);
            productCard.setAlignment(Pos.CENTER_LEFT);
            productCard.setPadding(new Insets(10));
            productCard.setStyle("-fx-background-color: #0d2232; -fx-background-radius: 8;");

            ImageView imageView = new ImageView();
            imageView.setFitWidth(150); imageView.setFitHeight(150); imageView.setPreserveRatio(false);
            if (product.getImage() != null) {
                try { imageView.setImage(new Image(new ByteArrayInputStream(Base64.getDecoder().decode(product.getImage())))); }
                catch (Exception ignored) {}
            }

            VBox infoBox = new VBox(4);
            Label nameLabel = new Label(product.getName()); nameLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: white;");
            Label priceLabel = new Label(product.formattedPrice()); priceLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #1abc9c; -fx-font-weight: bold;");
            Label descLabel = new Label("Stok: " + product.getStock() + " | " + product.getDescription());
            descLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #8b9eb0;"); descLabel.setWrapText(true); descLabel.setMaxWidth(200);

            infoBox.getChildren().addAll(nameLabel, priceLabel, descLabel);
            HBox.setHgrow(infoBox, Priority.ALWAYS);

            Button addToCartBtn = new Button(" + Keranjang ");
            addToCartBtn.setStyle("-fx-background-color: #1abc9c; -fx-text-fill: #07161E; -fx-font-weight: bold; -fx-background-radius: 4; -fx-cursor: hand;");
            addToCartBtn.setOnAction(e -> {
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Info");
                alert.setHeaderText(null);
                alert.setContentText("Silakan login terlebih dahulu untuk menambah produk ke keranjang.");
                alert.showAndWait();
            });

            productCard.getChildren().addAll(imageView, infoBox, addToCartBtn);
            cardsContainer.getChildren().add(productCard);
        }

        if (products.size() > displayed) {
            Text moreText = new Text("... dan " + (products.size() - displayed) + " produk lainnya.");
            moreText.setStyle("-fx-font-size: 14px; -fx-fill: #c2d6f6; -fx-font-style: italic;");
            cardsContainer.getChildren().add(moreText);
        }

        VBox bubbleContent = new VBox(cardsContainer);
        bubbleContent.getStyleClass().add("bubble-bot");
        bubbleContent.setPadding(new Insets(10, 14, 10, 14));

        Label timeLabel = new Label(time);
        timeLabel.getStyleClass().add("time-label");

        VBox content = new VBox(2, bubbleContent, timeLabel);
        StackPane avatar = new StackPane(); avatar.getStyleClass().add("bot-msg-avatar");
        Label avLbl = new Label("A"); avLbl.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #07161E;");
        avatar.getChildren().add(avLbl); avatar.setMinSize(30, 30); avatar.setMaxSize(30, 30);

        HBox row = new HBox(8, avatar, content);
        row.setPadding(new Insets(4, 20, 4, 20)); row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private void addUserMessage(String text) { messageContainer.getChildren().add(buildBubbleRow(text, true)); scrollToBottom(); }
    private void addBotMessage(String text) { messageContainer.getChildren().add(buildBubbleRow(text, false)); scrollToBottom(); }

    private HBox buildBubbleRow(String text, boolean isUser) {
        String time = LocalTime.now().format(TIME_FMT);
        Text msgText = new Text(text); msgText.setWrappingWidth(340);
        msgText.setFill(isUser ? javafx.scene.paint.Color.web("#07161E") : javafx.scene.paint.Color.web("#E8F0F3"));
        msgText.setStyle("-fx-font-size: 13px;");

        TextFlow flow = new TextFlow(msgText);
        flow.getStyleClass().add(isUser ? "bubble-user" : "bubble-bot");
        flow.setPadding(new Insets(10, 14, 10, 14));

        Label timeLabel = new Label(time); timeLabel.getStyleClass().add("time-label");
        VBox content = new VBox(2, flow, timeLabel);

        StackPane avatar = new StackPane(); avatar.getStyleClass().add(isUser ? "msg-avatar" : "bot-msg-avatar");
        Label avLbl = new Label(isUser ? "G" : "A");
        avLbl.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #07161E;");
        avatar.getChildren().add(avLbl); avatar.setMinSize(30, 30); avatar.setMaxSize(30, 30);

        HBox row = new HBox(8);
        row.setPadding(new Insets(4, 20, 4, 20));
        if (isUser) {
            row.setAlignment(Pos.CENTER_RIGHT); timeLabel.setAlignment(Pos.CENTER_RIGHT); row.getChildren().addAll(content, avatar);
        } else {
            row.setAlignment(Pos.CENTER_LEFT); row.getChildren().addAll(avatar, content);
        }
        return row;
    }

    private void scrollToBottom() {
        Platform.runLater(() -> {
            messageContainer.layout();
            scrollPane.layout();
            Platform.runLater(() -> scrollPane.setVvalue(1.0));
        });
    }

    private Product parseProduct(JsonElement element) {
        JsonElement normalized = normalizeJsonElement(element);
        if (normalized == null || normalized.isJsonNull()) return null;
        if (normalized.isJsonObject()) {
            JsonObject o = normalized.getAsJsonObject();
            return new Product(asString(o.get("id"), ""), asString(o.get("name"), "?"),
                    asString(o.get("gender"), ""), asLong(o.get("price"), 0L),
                    asInt(o.get("stock"), 0), asString(o.get("description"), "-"),
                    asNullableString(o.get("image")));
        }
        if (normalized.isJsonArray()) {
            JsonArray a = normalized.getAsJsonArray();
            return new Product(asString(getArrayValue(a, 0), ""), asString(getArrayValue(a, 1), "?"),
                    asString(getArrayValue(a, 6), ""), asLong(getArrayValue(a, 2), 0L),
                    asInt(getArrayValue(a, 3), 0), asString(getArrayValue(a, 5), "-"),
                    asNullableString(getArrayValue(a, 4)));
        }
        return null;
    }

    private JsonArray extractProducts(JsonElement source) {
        JsonElement normalized = normalizeJsonElement(source);
        if (normalized == null || normalized.isJsonNull()) return null;
        if (normalized.isJsonObject()) {
            JsonObject obj = normalized.getAsJsonObject();
            return obj.has("products") ? extractProducts(obj.get("products")) : null;
        }
        if (!normalized.isJsonArray()) return null;
        JsonArray array = normalized.getAsJsonArray();
        return (array.isEmpty() || !looksLikeProductPayload(array.get(0))) ? null : array;
    }

    private boolean looksLikeProductPayload(JsonElement element) {
        JsonElement n = normalizeJsonElement(element);
        if (n == null || n.isJsonNull()) return false;
        if (n.isJsonObject()) { JsonObject o = n.getAsJsonObject(); return o.has("name") && o.has("price"); }
        return n.isJsonArray() && n.getAsJsonArray().size() >= 4;
    }

    private JsonElement normalizeJsonElement(JsonElement element) {
        if (element == null || element.isJsonNull()) return null;
        if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isString()) {
            String raw = element.getAsString().trim();
            if ((raw.startsWith("{") && raw.endsWith("}")) || (raw.startsWith("[") && raw.endsWith("]"))) {
                try { return JsonParser.parseString(raw); } catch (Exception ignored) { }
            }
        }
        return element;
    }

    private JsonObject asJsonObject(JsonElement e) { JsonElement n = normalizeJsonElement(e); return (n != null && n.isJsonObject()) ? n.getAsJsonObject() : null; }
    private JsonArray asJsonArray(JsonElement e) { JsonElement n = normalizeJsonElement(e); return (n != null && n.isJsonArray()) ? n.getAsJsonArray() : null; }
    private String asString(JsonElement e, String fb) { JsonElement n = normalizeJsonElement(e); return (n == null || n.isJsonNull()) ? fb : n.getAsString(); }
    private long asLong(JsonElement e, long fb) { JsonElement n = normalizeJsonElement(e); return (n == null || n.isJsonNull()) ? fb : n.getAsLong(); }
    private int asInt(JsonElement e, int fb) { JsonElement n = normalizeJsonElement(e); return (n == null || n.isJsonNull()) ? fb : n.getAsInt(); }
    private String asNullableString(JsonElement e) { String s = asString(e, null); return (s == null || s.isBlank()) ? null : s; }
    private JsonElement getArrayValue(JsonArray a, int i) { return (a == null || i < 0 || i >= a.size()) ? null : a.get(i); }
}
