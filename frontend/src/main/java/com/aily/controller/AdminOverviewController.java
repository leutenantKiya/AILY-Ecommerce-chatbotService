package com.aily.controller;

import com.aily.App;
import com.aily.Session;
import com.aily.model.Order;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.net.URL;
import java.util.ResourceBundle;

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
        int txCount  = Session.orders.size();
        long revenue = Session.orders.stream().mapToLong(Order::getTotal).sum();

        totalProdukLabel.setText("0");
        totalTxLabel    .setText(String.valueOf(txCount));
        totalRevLabel   .setText(fmt(revenue));
        chatActiveLabel .setText("0");
        chatActiveSub   .setText("0 Belum Terjawab");

        // Show up to 5 recent transactions
        txRowsBox.getChildren().clear();
        int limit = Math.min(5, Session.orders.size());
        for (int i = 0; i < limit; i++) {
            Order o = Session.orders.get(Session.orders.size() - 1 - i);
            txRowsBox.getChildren().add(buildTxRow(o));
        }
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
    @FXML private void goProducts()     { try { App.switchScene("admin_products",     1280, 880); } catch (Exception ignored) {} }
    @FXML private void goTransactions() { try { App.switchScene("admin_transactions", 1280, 880); } catch (Exception ignored) {} }
    @FXML private void goChatHistory()  { try { App.switchScene("admin_chat",         1280, 880); } catch (Exception ignored) {} }

    @FXML
    private void handleLogout() {
        Session.clear();
        try { App.switchScene("landing", 1000, 700); } catch (Exception ignored) {}
    }

    private String fmt(long amount) {
        return String.format("Rp %,d", amount).replace(',', '.');
    }
}
