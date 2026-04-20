package com.aily.controller;

import com.aily.App;
import com.aily.Session;
import com.aily.model.Order;
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
        refresh();
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
        try { App.switchScene("chat", 1280, 880); } catch (Exception ignored) {}
    }
}
