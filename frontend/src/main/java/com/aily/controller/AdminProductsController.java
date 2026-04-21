package com.aily.controller;

import com.aily.App;
import com.aily.Session;
import com.aily.model.Product;
import com.aily.service.ApiService;
import com.aily.service.ApiService.*;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.UUID;


public class AdminProductsController implements Initializable {
    private List<Product> productList;
    @FXML private TextField namaProdukField;
    @FXML private TextField kodeProdukField;
    @FXML private TextField hargaField;
    @FXML private TextField stokField;
    @FXML private TextArea  deskripsiField;
    @FXML private Button    saveButton;
    @FXML private Label     produkCountLabel;
    @FXML private VBox      productRowsBox;

    // In-memory product list (would be fetched from backend in production)
    private final List<Product> products = new ArrayList<>();
    private Product editingProduct = null;

//    public AdminProductsController() throws Exception {
//        initTable();
//    }



    @Override
    public void initialize(URL url, ResourceBundle rb) {
        try {
            initTable();
        } catch (Exception e) {
            e.printStackTrace();
        }
        refreshTable();
    }

    private void initTable() throws Exception {
        JsonObject obj = ApiService.getProduk().getAsJsonObject("data");
        JsonArray array = obj.getAsJsonArray("products");

        products.clear();

        for (int i = 0; i < array.size(); i++) {
            JsonObject prod = array.get(i).getAsJsonObject();

            String id = prod.get("id").getAsString();
            String name = prod.get("name").getAsString();
            long price = prod.get("price").getAsLong();
            int stock = prod.get("stock").getAsInt();
            String category = prod.get("category").getAsString();
            String warna = prod.get("warna").getAsString();

            System.out.println(id);
            products.add(new Product(id, name, category, price, stock, warna));
        }

        refreshTable();
    }

    @FXML
    private void handleSave() {
        String name  = namaProdukField.getText().trim();
        String code  = kodeProdukField.getText().trim();
        String harga = hargaField.getText().trim();
        String stok  = stokField.getText().trim();
        String desc  = deskripsiField.getText().trim();

        if (name.isEmpty() || code.isEmpty()) return;

        long price  = harga.isEmpty() ? 0 : Long.parseLong(harga.replaceAll("[^0-9]", ""));
        int  stock  = stok.isEmpty()  ? 0 : Integer.parseInt(stok.replaceAll("[^0-9]", ""));


        Product product = new Product(UUID.randomUUID().toString().substring(0, 8).toUpperCase(),
                name, code, price, stock, desc);
        clearForm();
        appendProduct(product);

    }

    private void appendProduct(Product product){
        String name = product.getName();
        String code = product.getCode();
        Long price = product.getPrice();
        int stock = product.getStock();
        String desc = product.getDescription();

        if (editingProduct != null) {
            editingProduct.setName(name);
            editingProduct.setCode(code);
            editingProduct.setPrice(price);
            editingProduct.setStock(stock);
            editingProduct.setDescription(desc);
            editingProduct = null;
            saveButton.setText("Simpan Produk");
        } else {
            products.add(new Product(UUID.randomUUID().toString().substring(0, 8).toUpperCase(),
                    name, code, price, stock, desc));
        }

        refreshTable();
    }



    private void clearForm() {
        namaProdukField.clear();
        kodeProdukField.clear();
        hargaField.clear();
        stokField.clear();
        deskripsiField.clear();
    }

    private void refreshTable() {
        productRowsBox.getChildren().clear();
        produkCountLabel.setText(products.size() + "");
        for (Product p : products) {
            productRowsBox.getChildren().add(buildProductRow(p));
        }
    }

    private HBox buildProductRow(Product p) {
        Label id    = new Label(p.getCode());      id.getStyleClass().add("table-cell-text");  id.setPrefWidth(120);
        Label name  = new Label(p.getName());      name.getStyleClass().add("table-cell-bold"); name.setPrefWidth(200);
        Label price = new Label(p.formattedPrice()); price.getStyleClass().add("table-cell-teal"); price.setPrefWidth(160);

        Label stokLbl = new Label(p.getStock() + " UNIT");
        stokLbl.getStyleClass().add("status-diproses");
        StackPane stokBox = new StackPane(stokLbl); stokBox.setPrefWidth(100);

        Button editBtn = new Button("Edit");
        editBtn.getStyleClass().add("btn-gray");
        editBtn.setOnAction(e -> startEdit(p));

        Button delBtn = new Button("Hapus");
        delBtn.getStyleClass().add("btn-danger");
        delBtn.setOnAction(e -> { products.remove(p); refreshTable(); });

        HBox aksi = new HBox(6, editBtn, delBtn); aksi.setPrefWidth(120);

        HBox row = new HBox(id, name, price, stokBox, aksi);
        row.getStyleClass().add("table-row");
        row.setPadding(new Insets(10, 0, 10, 0));
        return row;
    }

    private void startEdit(Product p) {
        editingProduct = p;
        namaProdukField.setText(p.getName());
        kodeProdukField.setText(p.getCode());
        hargaField.setText(String.valueOf(p.getPrice()));
        stokField.setText(String.valueOf(p.getStock()));
        deskripsiField.setText(p.getDescription());
        saveButton.setText("Update Produk");
    }

    @FXML private void goOverview()     { try { App.switchScene("admin_overview",     1280, 880); } catch (Exception ignored) {} }
    @FXML private void goProducts()     { /* already here */ }
    @FXML private void goTransactions() { try { App.switchScene("admin_transactions", 1280, 880); } catch (Exception ignored) {} }
    @FXML private void goChatHistory()  { try { App.switchScene("admin_chat",         1280, 880); } catch (Exception ignored) {} }

    @FXML
    private void handleLogout() {
        Session.clear();
        try { App.switchScene("landing", 1000, 700); } catch (Exception ignored) {}
    }
}
