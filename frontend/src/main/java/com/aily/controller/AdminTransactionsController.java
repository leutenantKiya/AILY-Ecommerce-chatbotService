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

public class AdminTransactionsController implements Initializable {

    @FXML private VBox txRowsBox;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        txRowsBox.getChildren().clear();
        for (Order o : Session.orders) {
            txRowsBox.getChildren().add(buildRow(o));
        }
    }

    private HBox buildRow(Order o) {
        Label statusLbl = new Label(o.statusLabel());
        statusLbl.getStyleClass().add(o.statusCssClass());

        Label id      = new Label(o.getId());                  id.getStyleClass().add("table-cell-text");  id.setPrefWidth(140);
        Label buyer   = new Label("User");                     buyer.getStyleClass().add("table-cell-bold"); buyer.setPrefWidth(160);
        Label product = new Label(o.getProduct().getName());   product.getStyleClass().add("table-cell-text"); product.setPrefWidth(200);
        Label qty     = new Label(String.valueOf(o.getQuantity())); qty.getStyleClass().add("table-cell-text"); qty.setPrefWidth(80);
        Label total   = new Label(o.formattedTotal());         total.getStyleClass().add("table-cell-teal"); total.setPrefWidth(160);
        StackPane statusBox = new StackPane(statusLbl);        statusBox.setPrefWidth(120);

        HBox row = new HBox(id, buyer, product, qty, total, statusBox);
        row.getStyleClass().add("table-row");
        row.setPadding(new Insets(10, 0, 10, 0));
        return row;
    }

    @FXML private void goOverview()     { try { App.switchScene("admin_overview"); } catch (Exception ignored) {} }
    @FXML private void goProducts()     { try { App.switchScene("admin_products"); } catch (Exception ignored) {} }
    @FXML private void goTransactions() { /* already here */ }
    @FXML private void goChatHistory()  { try { App.switchScene("admin_chat"); } catch (Exception ignored) {} }

    @FXML
    private void handleLogout() {
        Session.clear();
        try { App.switchScene("landing"); } catch (Exception ignored) {}
    }
}
