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
import java.util.ResourceBundle;

import static com.aily.service.ApiService.getProduk;

public class AdminOverviewController implements Initializable {

    @FXML private Label totalProdukLabel;
    @FXML private Label totalProdukSub;
    @FXML private Label totalTxLabel;
    @FXML private Label totalTxSub;
    @FXML private Label totalRevLabel;
    @FXML private Label totalRevSub;
    @FXML private Label chatActiveLabel;
    @FXML private Label chatActiveSub;
    @FXML private VBox  txRowsBox;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        totalProdukLabel.setText(String.valueOf(countProduct()));
        chatActiveLabel .setText("0");
        chatActiveSub   .setText("0 Belum Terjawab");
        new Thread(this::loadOrdersFromBackend).start();
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
            }
        } catch (Exception ignored) {
        }

        Platform.runLater(this::refreshTransactions);
    }

    private void refreshTransactions() {
        // Tampilkan transaksi yang selesai saja
        java.util.List<Order> completed = new java.util.ArrayList<>();
        for (Order o : Session.orders) {
            if (o.getStatus() == Order.Status.SELESAI) {
                completed.add(o);
            }
        }

        int txCount = completed.size();
        long revenue = 0;
        for (Order o : completed) {
            revenue += o.getTotal();
        }

        totalTxLabel.setText(String.valueOf(txCount));
        totalRevLabel.setText(fmt(revenue));
        txRowsBox.getChildren().clear();
        int limit = Math.min(5, completed.size());
        for (int i = 0; i < limit; i++) {
            Order o = completed.get(i);
            txRowsBox.getChildren().add(buildTxRow(o));
        }
    }

    private Order parseOrder(JsonObject orderObj) {
        JsonArray items = orderObj.has("items") && orderObj.get("items").isJsonArray()
                ? orderObj.getAsJsonArray("items")
                : new JsonArray();

        StringBuilder names = new StringBuilder();
        int quantity = 0;
        Product product = null;

        for (int i = 0; i < items.size(); i++) {
            JsonObject item = items.get(i).getAsJsonObject();
            if (i > 0) {
                names.append(", ");
            }
            names.append(item.get("product_name").getAsString());
            quantity += item.get("quantity").getAsInt();
            if (product == null) {
                product = new Product(
                        item.get("product_id").getAsString(),
                        item.get("product_name").getAsString(),
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
        } else if (names.length() > 0) {
            product.setName(names.toString());
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
            case "dalam pengiriman" -> Order.Status.DIKIRIM;
            case "selesai" -> Order.Status.SELESAI;
            case "dibatalkan" -> Order.Status.DIBATALKAN;
            default -> Order.Status.DIPROSES;
        };
    }

    private int countProduct(){
        String productCount = "0";

        try{
            JsonObject obj = getProduk().getAsJsonObject("data");
            JsonArray array = obj.getAsJsonArray("products");
            productCount = String.valueOf(array.size());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return Integer.parseInt(productCount);
    }

    private HBox buildTxRow(Order o) {
        Label statusLbl = new Label(o.statusLabel());
        statusLbl.getStyleClass().add(o.statusCssClass());

        Label id      = new Label(o.getId());       id.getStyleClass().add("table-cell-text"); id.setPrefWidth(100);
        Label buyer   = new Label("User");           buyer.getStyleClass().add("table-cell-bold"); buyer.setPrefWidth(160);
        Label product = new Label(o.getProduct().getName()); product.getStyleClass().add("table-cell-text"); product.setPrefWidth(200);
        Label total   = new Label(o.formattedTotal()); total.getStyleClass().add("table-cell-teal"); total.setPrefWidth(160);
        StackPane statusBox = new StackPane(statusLbl);
        statusBox.setPrefWidth(120);

        HBox row = new HBox(id, buyer, product, total, statusBox);
        row.getStyleClass().add("table-row");
        row.setPadding(new Insets(10, 0, 10, 0));
        return row;
    }

    // ── Nav ──────────────────────────────────────────────────────
    @FXML private void goOverview()     { /* already here */ }
    @FXML private void goProducts()     { try { App.switchScene("admin_products"); } catch (Exception ignored) {} }
    @FXML private void goStoreInfo()    { try { App.switchScene("admin_store"); } catch (Exception ignored) {} }
    @FXML private void goTransactions() { try { App.switchScene("admin_transactions"); } catch (Exception ignored) {} }
    @FXML private void goChatHistory()  { try { App.switchScene("admin_chat"); } catch (Exception ignored) {} }

    @FXML
    private void handleLogout() {
        Session.clear();
        try { App.switchScene("landing"); } catch (Exception ignored) {}
    }

    private String fmt(long amount) {
        return String.format("Rp %,d", amount).replace(',', '.');
    }
}
