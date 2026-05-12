package com.aily.controller;

import com.aily.App;
import com.aily.Session;
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
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class OrdersController implements Initializable {

    @FXML private VBox  ordersBox;
    @FXML private VBox  emptyState;
    @FXML private Label itemCountLabel;
    @FXML private Button tabSemua;
    @FXML private Button tabDiproses;
    @FXML private Button tabSelesai;

    private String currentFilter = "semua";

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        if (Session.currentUser != null) {
            new Thread(this::loadOrdersFromBackend).start();
        } else {
            refresh();
        }
    }

    private void loadOrdersFromBackend() {
        try {
            JsonObject response = ApiService.getUserOrders(Session.currentUser.getId());
            if (response.has("status") && response.get("status").getAsInt() == 200) {
                JsonObject data = response.getAsJsonObject("data");
                JsonArray orders = data.getAsJsonArray("orders");
                Session.orders.clear();
                for (JsonElement orderEl : orders) {
                    Session.orders.add(parseOrder(orderEl.getAsJsonObject()));
                }
            }
        } catch (Exception ignored) {
        }

        Platform.runLater(this::refresh);
    }

    private Order parseOrder(JsonObject orderObj) {
        JsonArray items = orderObj.has("items") && orderObj.get("items").isJsonArray()
                ? orderObj.getAsJsonArray("items")
                : new JsonArray();

        StringBuilder names = new StringBuilder();
        int quantity = 0;
        Product displayProduct = null;

        for (int i = 0; i < items.size(); i++) {
            JsonObject item = items.get(i).getAsJsonObject();
            if (i > 0) {
                names.append(", ");
            }
            names.append(item.get("product_name").getAsString());
            quantity += item.get("quantity").getAsInt();

            if (displayProduct == null) {
                JsonObject product = item.getAsJsonObject("product");
                displayProduct = new Product(
                        product.get("id").getAsString(),
                        item.get("product_name").getAsString(),
                        orderObj.get("order_code").getAsString(),
                        item.get("price").getAsLong(),
                        product.has("stock") ? product.get("stock").getAsInt() : 0,
                        product.has("description") ? product.get("description").getAsString() : "",
                        product.has("image") && !product.get("image").isJsonNull()
                                ? product.get("image").getAsString()
                                : null
                );
            }
        }

        if (displayProduct == null) {
            displayProduct = new Product("0", "Pesanan", orderObj.get("order_code").getAsString(), 0, 0, "", null);
        } else if (names.length() > 0) {
            displayProduct.setName(names.toString());
        }

        return new Order(
                orderObj.get("order_code").getAsString(),
                displayProduct,
                quantity,
                orderObj.get("total").getAsLong(),
                parseStatus(orderObj.get("status").getAsString())
        );
    }

    private Order.Status parseStatus(String status) {
        return switch (status.toLowerCase()) {
            case "dalam pengiriman" -> Order.Status.DIKIRIM;
            case "selesai" -> Order.Status.SELESAI;
            case "dibatalkan" -> Order.Status.DIBATALKAN;
            default -> Order.Status.DIPROSES;
        };
    }

    private void refresh() {
        List<Order> filtered = switch (currentFilter) {
            case "diproses" -> Session.orders.stream()
                    .filter(o -> o.getStatus() == Order.Status.DIPROSES || o.getStatus() == Order.Status.DIKIRIM)
                    .collect(Collectors.toList());
            case "selesai"  -> Session.orders.stream()
                    .filter(o -> o.getStatus() == Order.Status.SELESAI)
                    .collect(Collectors.toList());
            default         -> Session.orders;
        };

        ordersBox.getChildren().clear();
        boolean empty = filtered.isEmpty();
        emptyState.setVisible(empty);
        emptyState.setManaged(empty);
        ordersBox.setVisible(!empty);
        ordersBox.setManaged(!empty);

        itemCountLabel.setText(Session.orders.size() + " item");

        for (Order order : filtered) {
            ordersBox.getChildren().add(buildOrderRow(order));
        }

        // Update tab styles
        tabSemua   .getStyleClass().setAll(currentFilter.equals("semua")     ? "tab-btn-active" : "tab-btn");
        tabDiproses.getStyleClass().setAll(currentFilter.equals("diproses")  ? "tab-btn-active" : "tab-btn");
        tabSelesai .getStyleClass().setAll(currentFilter.equals("selesai")   ? "tab-btn-active" : "tab-btn");
    }

    private HBox buildOrderRow(Order order) {
        // Product image placeholder
        StackPane imgBox = new StackPane();
        imgBox.getStyleClass().add("product-image-box");
        imgBox.setMinSize(72, 72); imgBox.setMaxSize(72, 72);
        Label imgLbl = new Label("📦");
        imgLbl.setStyle("-fx-font-size: 24px;");
        imgBox.getChildren().add(imgLbl);

        VBox info = new VBox(4);
        Label name = new Label(order.getProduct().getName());
        name.getStyleClass().add("table-cell-bold");
        Label code = new Label(order.getProduct().getCode());
        code.getStyleClass().add("table-cell-muted");
        Label qty = new Label("x" + order.getQuantity());
        qty.getStyleClass().add("text-gray");
        info.getChildren().addAll(name, code, qty);
        HBox.setHgrow(info, Priority.ALWAYS);

        VBox right = new VBox(6);
        right.setAlignment(Pos.CENTER_RIGHT);
        Label total = new Label("Total pesanan  " + order.formattedTotal());
        total.getStyleClass().add("table-cell-teal");
        Label statusBadge = new Label(order.statusLabel());
        statusBadge.getStyleClass().add(order.statusCssClass());
        right.getChildren().addAll(total, statusBadge);

        if (order.canCancel()) {
            Button cancelBtn = new Button("Batalkan");
            cancelBtn.getStyleClass().add("btn-danger");
            cancelBtn.setOnAction(e -> cancelOrder(order));
            right.getChildren().add(cancelBtn);
        }

        HBox row = new HBox(14, imgBox, info, right);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("order-card");
        return row;
    }

    @FXML private void filterSemua()    { currentFilter = "semua";    refresh(); }
    @FXML private void filterDiproses() { currentFilter = "diproses"; refresh(); }
    @FXML private void filterSelesai()  { currentFilter = "selesai";  refresh(); }

    @FXML
    private void goBack() {
        try { App.switchScene("chat"); } catch (Exception ignored) {}
    }

    private void cancelOrder(Order order) {
        if (Session.currentUser == null) {
            return;
        }

        int orderId = parseOrderId(order.getId());
        new Thread(() -> {
            try {
                JsonObject response = ApiService.cancelOrder(Session.currentUser.getId(), orderId);
                Platform.runLater(() -> {
                    if (response.has("status") && response.get("status").getAsInt() == 200) {
                        new Thread(this::loadOrdersFromBackend).start();
                    } else {
                        String message = response.has("error")
                                ? response.get("error").getAsString()
                                : "Pesanan gagal dibatalkan.";
                        showAlert(Alert.AlertType.ERROR, "Gagal", message);
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "Gagal", "Tidak dapat terhubung ke server."));
            }
        }).start();
    }

    private int parseOrderId(String orderCode) {
        return Integer.parseInt(orderCode.replaceAll("[^0-9]", ""));
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
