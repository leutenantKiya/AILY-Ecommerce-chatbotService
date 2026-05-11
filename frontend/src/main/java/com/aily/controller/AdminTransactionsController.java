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
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

public class AdminTransactionsController implements Initializable {

    @FXML private VBox txRowsBox;
    @FXML private TextField searchField;

    private final List<Order> allOrders = new ArrayList<>();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        new Thread(this::loadOrdersFromBackend).start();

        if (searchField != null) {
            searchField.textProperty().addListener((obs, oldVal, newVal) -> filterOrders(newVal));
        }
    }

    private void loadOrdersFromBackend() {
        try {
            JsonObject response = ApiService.getAdminOrders();
            if (response.has("status") && response.get("status").getAsInt() == 200) {
                JsonObject data = response.getAsJsonObject("data");
                JsonArray orders = data.getAsJsonArray("orders");
                Session.orders.clear();
                for (JsonElement orderEl : orders) {
                    Session.orders.add(parseOrder(orderEl.getAsJsonObject()));
                }
                allOrders.clear();
                allOrders.addAll(Session.orders);
            }
        } catch (Exception ignored) {
        }

        Platform.runLater(this::refresh);
    }

    private void refresh() {
        String query = searchField != null ? searchField.getText() : "";
        filterOrders(query);
    }

    private void filterOrders(String query) {
        txRowsBox.getChildren().clear();
        String keyword = (query == null) ? "" : query.trim().toLowerCase();

        for (Order o : allOrders) {
            if (keyword.isEmpty()
                    || o.getId().toLowerCase().contains(keyword)
                    || o.getProduct().getName().toLowerCase().contains(keyword)
                    || o.statusLabel().toLowerCase().contains(keyword)) {
                txRowsBox.getChildren().add(buildRow(o));
            }
        }
    }

    private Order parseOrder(JsonObject orderObj) {
        JsonArray items = orderObj.has("items") && orderObj.get("items").isJsonArray()
                ? orderObj.getAsJsonArray("items")
                : new JsonArray();

        StringBuilder details = new StringBuilder();
        int quantity = 0;
        Product product = null;

        for (int i = 0; i < items.size(); i++) {
            JsonObject item = items.get(i).getAsJsonObject();
            String itemName = item.get("product_name").getAsString();
            int itemQty = item.get("quantity").getAsInt();
            quantity += itemQty;

            if (i > 0) {
                details.append("\n");
            }
            details.append(itemName).append(" ~ ").append(itemQty);

            if (product == null) {
                product = new Product(
                        item.get("product_id").getAsString(),
                        itemName,
                        orderObj.get("order_code").getAsString(),
                        item.get("price").getAsLong(),
                        0,
                        "",
                        null
                );
            }
        }

        if (product == null) {
            product = new Product("0", "Pesanan", orderObj.get("order_code").getAsString(), 0, 0, "", null);
        } else {
            product.setName(details.toString());
        }

        return new Order(
                orderObj.get("order_code").getAsString(),
                product,
                quantity,
                orderObj.get("total").getAsLong(),
                parseStatus(orderObj.get("status").getAsString())
        );
    }

    private Order.Status parseStatus(String status) {
    return switch (status.toLowerCase()) {
        case "dalam pengiriman":
            yield Order.Status.DIKIRIM;

        case "selesai":
            yield Order.Status.SELESAI;

        case "dibatalkan":
            yield Order.Status.DIBATALKAN;

        default:
            yield Order.Status.DIPROSES;
    };
}

    private HBox buildRow(Order o) {
        Label statusLbl = new Label(o.statusLabel());
        statusLbl.getStyleClass().add(o.statusCssClass());

        Label id      = new Label(o.getId());                  id.getStyleClass().add("table-cell-text");  id.setPrefWidth(140);
        Label buyer   = new Label("User");                     buyer.getStyleClass().add("table-cell-bold"); buyer.setPrefWidth(160);

        // Detail produk multi-line: "Nama ~ qty" per baris
        Label product = new Label(o.getProduct().getName());
        product.getStyleClass().add("table-cell-text");
        product.setPrefWidth(280);
        product.setWrapText(true);

        Label total   = new Label(o.formattedTotal());         total.getStyleClass().add("table-cell-teal"); total.setPrefWidth(160);
        StackPane statusBox = new StackPane(statusLbl);        statusBox.setPrefWidth(120);

        HBox actions = new HBox(6);
        actions.setPrefWidth(140);
        if (o.getStatus() == Order.Status.DIPROSES) {
            Button shipBtn = new Button("Kirim");
            shipBtn.getStyleClass().add("btn-gray");
            shipBtn.setOnAction(e -> updateStatus(o, "Dalam Pengiriman"));
            actions.getChildren().add(shipBtn);
        } else if (o.getStatus() == Order.Status.DIKIRIM) {
            Button doneBtn = new Button("Selesai");
            doneBtn.getStyleClass().add("btn-gray");
            doneBtn.setOnAction(e -> updateStatus(o, "Selesai"));
            actions.getChildren().add(doneBtn);
        }

        HBox row = new HBox(id, buyer, product, total, statusBox, actions);
        row.getStyleClass().add("table-row");
        row.setPadding(new Insets(10, 0, 10, 0));
        return row;
    }

    private void updateStatus(Order order, String status) {
        int orderId = Integer.parseInt(order.getId().replaceAll("[^0-9]", ""));
        new Thread(() -> {
            try {
                ApiService.updateOrderStatus(orderId, status);
                loadOrdersFromBackend();
            } catch (Exception ignored) {
            }
        }).start();
    }

    @FXML private void goOverview()     { try { App.switchScene("admin_overview"); } catch (Exception ignored) {} }
    @FXML private void goProducts()     { try { App.switchScene("admin_products"); } catch (Exception ignored) {} }
    @FXML private void goStoreInfo()    { try { App.switchScene("admin_store"); } catch (Exception ignored) {} }
    @FXML private void goTransactions() { /* already here */ }
    @FXML private void goChatHistory()  { try { App.switchScene("admin_chat"); } catch (Exception ignored) {} }

    @FXML
    private void handleLogout() {
        Session.clear();
        try { App.switchScene("landing"); } catch (Exception ignored) {}
    }
}
