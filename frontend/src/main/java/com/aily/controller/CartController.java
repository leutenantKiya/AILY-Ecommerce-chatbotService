package com.aily.controller;

import com.aily.App;
import com.aily.Session;
import com.aily.model.CartItem;
import com.aily.model.Order;
import com.aily.model.Product;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.net.URL;
import java.util.ResourceBundle;
import java.util.UUID;

public class CartController implements Initializable {

    @FXML private VBox  cartItemsBox;
    @FXML private VBox  emptyState;
    @FXML private Label itemCountLabel;
    @FXML private Label subtotalLabel;
    @FXML private Label ongkirLabel;
    @FXML private Label diskonLabel;
    @FXML private Label totalLabel;

    private static final long ONGKIR = 15_000L;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        refresh();
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

        long ongkir  = empty ? 0 : ONGKIR;
        long total   = subtotal + ongkir;

        subtotalLabel.setText(fmt(subtotal));
        ongkirLabel  .setText(fmt(ongkir));
        diskonLabel  .setText("- Rp 0");
        totalLabel   .setText(fmt(total));
    }

    private HBox buildCartItemRow(CartItem item) {
        Product p = item.getProduct();

        // Product image placeholder
        StackPane imgBox = new StackPane();
        imgBox.getStyleClass().add("product-image-box");
        imgBox.setMinSize(80, 80); imgBox.setMaxSize(80, 80);
        Label imgLbl = new Label("📦");
        imgLbl.setStyle("-fx-font-size: 28px;");
        imgBox.getChildren().add(imgLbl);

        // Product info
        VBox info = new VBox(4);
        Label name = new Label(p.getName());
        name.getStyleClass().add("table-cell-bold");
        Label code = new Label(p.getCode());
        code.getStyleClass().add("table-cell-muted");
        Label stock = new Label("Stok tersedia: " + p.getStock());
        stock.getStyleClass().add("text-muted");
        info.getChildren().addAll(name, code, stock);
        HBox.setHgrow(info, Priority.ALWAYS);

        // Qty controls
        Button minus = new Button("-");
        minus.getStyleClass().add("qty-btn");
        Label qtyLabel = new Label(String.valueOf(item.getQuantity()));
        qtyLabel.getStyleClass().add("qty-label");
        Button plus = new Button("+");
        plus.getStyleClass().add("qty-btn");

        minus.setOnAction(e -> {
            if (item.getQuantity() > 1) {
                item.setQuantity(item.getQuantity() - 1);
            } else {
                Session.cart.remove(item);
            }
            refresh();
        });
        plus.setOnAction(e -> {
            if (item.getQuantity() < p.getStock()) {
                item.setQuantity(item.getQuantity() + 1);
                refresh();
            }
        });

        HBox qtyBox = new HBox(8, minus, qtyLabel, plus);
        qtyBox.setAlignment(Pos.CENTER);

        // Price
        Label price = new Label(p.formattedPrice());
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
        if (Session.cart.isEmpty()) return;

        long subtotal = Session.cart.stream().mapToLong(CartItem::subtotal).sum();
        long total = subtotal + ONGKIR;
        String orderId = "#TRX-" + (int)(Math.random() * 900 + 100);

        // Create one order per cart item (simplified)
        for (CartItem item : Session.cart) {
            Order order = new Order(orderId, item.getProduct(),
                    item.getQuantity(), total, Order.Status.DIPROSES);
            Session.orders.add(order);
        }
        Session.cart.clear();
        refresh();

        // Show a simple alert
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Pesanan Berhasil");
        alert.setHeaderText(null);
        alert.setContentText("Pesanan berhasil dibuat! No. pesanan " + orderId);
        alert.showAndWait();
    }

    @FXML
    private void goBack() {
        try { App.switchScene("chat", 1280, 880); } catch (Exception ignored) {}
    }

    private String fmt(long amount) {
        return String.format("Rp %,d", amount).replace(',', '.');
    }
}
