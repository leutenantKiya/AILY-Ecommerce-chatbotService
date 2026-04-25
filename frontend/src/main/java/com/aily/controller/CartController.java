package com.aily.controller;

import com.aily.App;
import com.aily.Session;
import com.aily.model.CartItem;
import com.aily.model.Order;
import com.aily.model.Product;
import com.aily.service.ApiService;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.util.ResourceBundle;

public class CartController implements Initializable {

    @FXML private VBox cartItemsBox;
    @FXML private VBox emptyState;
    @FXML private Label itemCountLabel;
    @FXML private Label subtotalLabel;
    @FXML private Label ongkirLabel;
    @FXML private Label diskonLabel;
    @FXML private Label totalLabel;

    private static final long ONGKIR = 15_000L;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        if (Session.currentUser != null) {
            new Thread(this::loadCartFromBackend).start();
        } else {
            refresh();
        }
    }

    private void loadCartFromBackend() {
        try {
            JsonObject response = ApiService.getUserCart(Session.currentUser.getId());
            if (response.has("status") && response.get("status").getAsInt() == 200) {
                JsonObject data = response.getAsJsonObject("data");
                JsonArray items = data.getAsJsonArray("items");
                Session.cart.clear();

                for (JsonElement itemEl : items) {
                    JsonObject itemObj = itemEl.getAsJsonObject();
                    Product product = new Product(
                            itemObj.get("product_id").getAsString(),
                            itemObj.get("name").getAsString(),
                            itemObj.has("gender") && !itemObj.get("gender").isJsonNull()
                                    ? itemObj.get("gender").getAsString()
                                    : "U",
                            itemObj.get("price").getAsLong(),
                            itemObj.get("stock").getAsInt(),
                            itemObj.has("description") && !itemObj.get("description").isJsonNull()
                                    ? itemObj.get("description").getAsString()
                                    : "-",
                            itemObj.has("image") && !itemObj.get("image").isJsonNull()
                                    ? itemObj.get("image").getAsString()
                                    : null
                    );
                    Session.cart.add(new CartItem(product, itemObj.get("quantity").getAsInt()));
                }
            }
        } catch (Exception ignored) {
        }

        Platform.runLater(this::refresh);
    }

    private void refresh() {
        cartItemsBox.getChildren().clear();
        boolean empty = Session.cart.isEmpty();
        emptyState.setVisible(empty);
        emptyState.setManaged(empty);
        cartItemsBox.setVisible(!empty);
        cartItemsBox.setManaged(!empty);

        int count = Session.cartCount();
        itemCountLabel.setText(count + " item");

        long subtotal = 0;
        for (CartItem item : Session.cart) {
            subtotal += item.subtotal();
            cartItemsBox.getChildren().add(buildCartItemRow(item));
        }

        long ongkir = empty ? 0 : ONGKIR;
        long total = subtotal + ongkir;

        subtotalLabel.setText(fmt(subtotal));
        ongkirLabel.setText(fmt(ongkir));
        diskonLabel.setText("- Rp 0");
        totalLabel.setText(fmt(total));
    }

    private HBox buildCartItemRow(CartItem item) {
        Product product = item.getProduct();

        StackPane imgBox = new StackPane();
        imgBox.getStyleClass().add("product-image-box");
        imgBox.setMinSize(80, 80);
        imgBox.setMaxSize(80, 80);
        Label imgLbl = new Label("\uD83D\uDCE6");
        imgLbl.setStyle("-fx-font-size: 28px;");
        imgBox.getChildren().add(imgLbl);

        VBox info = new VBox(4);
        Label name = new Label(product.getName());
        name.getStyleClass().add("table-cell-bold");
        Label code = new Label(product.getCode());
        code.getStyleClass().add("table-cell-muted");
        Label stock = new Label("Stok tersedia: " + product.getStock());
        stock.getStyleClass().add("text-muted");
        info.getChildren().addAll(name, code, stock);
        HBox.setHgrow(info, Priority.ALWAYS);

        Button minus = new Button("-");
        minus.getStyleClass().add("qty-btn");
        Label qtyLabel = new Label(String.valueOf(item.getQuantity()));
        qtyLabel.getStyleClass().add("qty-label");
        Button plus = new Button("+");
        plus.getStyleClass().add("qty-btn");

        minus.setOnAction(e -> {
            int newQuantity = item.getQuantity() - 1;
            if (newQuantity > 0) {
                item.setQuantity(newQuantity);
                syncCartQuantityAsync(product.getId(), newQuantity);
            } else {
                Session.cart.remove(item);
                removeCartItemAsync(product.getId());
            }
            refresh();
        });

        plus.setOnAction(e -> {
            if (item.getQuantity() < product.getStock()) {
                item.setQuantity(item.getQuantity() + 1);
                syncCartQuantityAsync(product.getId(), item.getQuantity());
                refresh();
            }
        });

        HBox qtyBox = new HBox(8, minus, qtyLabel, plus);
        qtyBox.setAlignment(Pos.CENTER);

        Label price = new Label(product.formattedPrice());
        price.getStyleClass().add("text-teal");
        price.setStyle("-fx-font-weight: bold; -fx-font-size: 13px;");

        HBox row = new HBox(14, imgBox, info, qtyBox, price);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("cart-item-card");
        row.setPadding(new Insets(14, 16, 14, 16));
        return row;
    }

    @FXML
    private void handleCheckout() {
        if (Session.cart.isEmpty()) {
            return;
        }

        if (Session.currentUser == null) {
            showAlert(Alert.AlertType.ERROR, "Checkout Gagal", "Sesi habis. Silakan login ulang.");
            return;
        }

        new Thread(() -> {
            try {
                JsonObject response = ApiService.checkout(Session.currentUser.getId());
                Platform.runLater(() -> {
                    if (response.has("status") && response.get("status").getAsInt() == 200) {
                        JsonObject data = response.getAsJsonObject("data");
                        JsonObject order = data.has("order") ? data.getAsJsonObject("order") : null;
                        String orderCode = order != null && order.has("order_code")
                                ? order.get("order_code").getAsString()
                                : "-";
                        Session.cart.clear();
                        refresh();
                        showAlert(Alert.AlertType.INFORMATION, "Pesanan Berhasil",
                                "Pesanan berhasil dibuat! No. pesanan " + orderCode);
                    } else {
                        String message = response.has("error")
                                ? response.get("error").getAsString()
                                : "Checkout gagal diproses.";
                        showAlert(Alert.AlertType.ERROR, "Checkout Gagal", message);
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "Checkout Gagal",
                        "Tidak dapat terhubung ke server."));
            }
        }).start();
    }

    @FXML
    private void goBack() {
        try {
            App.switchScene("chat");
        } catch (Exception ignored) {
        }
    }

    private void syncCartQuantityAsync(String productId, int quantity) {
        if (Session.currentUser == null) {
            return;
        }

        new Thread(() -> {
            try {
                ApiService.updateCartItem(Session.currentUser.getId(), Integer.parseInt(productId), quantity);
            } catch (Exception ignored) {
            }
        }).start();
    }

    private void removeCartItemAsync(String productId) {
        if (Session.currentUser == null) {
            return;
        }

        new Thread(() -> {
            try {
                ApiService.removeFromCart(Session.currentUser.getId(), Integer.parseInt(productId));
            } catch (Exception ignored) {
            }
        }).start();
    }

    private void clearBackendCartAsync() {
        if (Session.currentUser == null) {
            return;
        }

        new Thread(() -> {
            try {
                ApiService.clearUserCart(Session.currentUser.getId());
            } catch (Exception ignored) {
            }
        }).start();
    }

    private String fmt(long amount) {
        return String.format("Rp %,d", amount).replace(',', '.');
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
