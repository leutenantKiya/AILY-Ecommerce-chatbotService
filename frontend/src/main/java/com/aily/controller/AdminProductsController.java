package com.aily.controller;

import com.aily.App;
import com.aily.Session;
import com.aily.model.Product;
import com.aily.service.ApiService;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;

import java.io.File;
import java.nio.file.Files;
import java.util.Base64;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

public class AdminProductsController implements Initializable {

    @FXML private TextField namaProdukField;
    @FXML private TextField kodeProdukField;
    @FXML private TextField hargaField;
    @FXML private TextField stokField;
    @FXML private TextArea  deskripsiField;
    @FXML private Button    saveButton;
    @FXML private Label     produkCountLabel;
    @FXML private VBox      productRowsBox;
    @FXML private Label     imageNameLabel;

    private String currentImageBase64 = null;

    // Product list loaded from backend
    private final List<Product> products = new ArrayList<>();
    private Product editingProduct = null;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        loadProductsFromBackend();
    }

<<<<<<< Updated upstream
    private void loadProductsFromBackend() {
        new Thread(() -> {
            try {
                JsonObject response = ApiService.listProducts();
                Platform.runLater(() -> {
                    products.clear();
                    if (response.has("status") && response.get("status").getAsInt() == 200) {
                        JsonObject data = response.getAsJsonObject("data");
                        if (data.has("products")) {
                            JsonArray arr = data.getAsJsonArray("products");
                            for (int i = 0; i < arr.size(); i++) {
                                JsonObject p = arr.get(i).getAsJsonObject();
                                products.add(new Product(
                                        String.valueOf(p.get("id").getAsInt()),
                                        p.get("name").getAsString(),
                                        p.has("category") ? p.get("category").getAsString() : "",
                                        p.get("price").getAsLong(),
                                        p.get("stock").getAsInt(),
                                        p.has("description") && !p.get("description").isJsonNull()
                                                ? p.get("description").getAsString() : ""
                                ));
                            }
                        }
                    }
                    refreshTable();
                });
            } catch (Exception e) {
                Platform.runLater(this::refreshTable);
            }
        }).start();
=======
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
            String imageStr = prod.has("image") && !prod.get("image").isJsonNull() ? prod.get("image").getAsString() : null;

            System.out.println(id);
            products.add(new Product(id, name, category, price, stock, warna, imageStr));
        }

        refreshTable();
>>>>>>> Stashed changes
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

<<<<<<< Updated upstream
        saveButton.setDisable(true);

        if (editingProduct != null) {
            int productId = Integer.parseInt(editingProduct.getId());
            new Thread(() -> {
                try {
                    ApiService.updateProduct(productId, name, (int) price, stock, desc, code, "U", null);
                    Platform.runLater(() -> {
                        saveButton.setDisable(false);
                        editingProduct = null;
                        saveButton.setText("Simpan Produk");
                        clearForm();
                        loadProductsFromBackend();
                    });
                } catch (Exception e) {
                    Platform.runLater(() -> saveButton.setDisable(false));
                }
            }).start();
        } else {
            new Thread(() -> {
                try {
                    ApiService.addProduct(name, (int) price, stock, desc, code, "U", null);
                    Platform.runLater(() -> {
                        saveButton.setDisable(false);
                        clearForm();
                        loadProductsFromBackend();
                    });
                } catch (Exception e) {
                    Platform.runLater(() -> saveButton.setDisable(false));
                }
            }).start();
=======

        Product product = new Product(UUID.randomUUID().toString().substring(0, 8).toUpperCase(),
                name, code, price, stock, desc, currentImageBase64);
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
            editingProduct.setImage(currentImageBase64);
            editingProduct = null;
            saveButton.setText("Simpan Produk");
        } else {
            products.add(new Product(UUID.randomUUID().toString().substring(0, 8).toUpperCase(),
                    name, code, price, stock, desc, currentImageBase64));
>>>>>>> Stashed changes
        }
    }

    private void clearForm() {
        namaProdukField.clear();
        kodeProdukField.clear();
        hargaField.clear();
        stokField.clear();
        deskripsiField.clear();
        currentImageBase64 = null;
        if (imageNameLabel != null) {
            imageNameLabel.setText("Belum ada gambar");
        }
    }

    private void refreshTable() {
        productRowsBox.getChildren().clear();
        produkCountLabel.setText(products.size() + " produk");
        for (Product p : products) {
            productRowsBox.getChildren().add(buildProductRow(p));
        }
    }

    private HBox buildProductRow(Product p) {
        Label id    = new Label(p.getId());         id.getStyleClass().add("table-cell-text");  id.setPrefWidth(60);
        Label name  = new Label(p.getName());       name.getStyleClass().add("table-cell-bold"); name.setPrefWidth(200);
        Label code  = new Label(p.getCode());       code.getStyleClass().add("table-cell-text"); code.setPrefWidth(120);
        Label price = new Label(p.formattedPrice()); price.getStyleClass().add("table-cell-teal"); price.setPrefWidth(160);

        Label stokLbl = new Label(p.getStock() + " UNIT");
        stokLbl.getStyleClass().add("status-diproses");
        StackPane stokBox = new StackPane(stokLbl); stokBox.setPrefWidth(100);

        Button editBtn = new Button("Edit");
        editBtn.getStyleClass().add("btn-gray");
        editBtn.setOnAction(e -> startEdit(p));

        Button delBtn = new Button("Hapus");
        delBtn.getStyleClass().add("btn-danger");
        delBtn.setOnAction(e -> handleDelete(p));

        HBox aksi = new HBox(6, editBtn, delBtn); aksi.setPrefWidth(120);

        HBox row = new HBox(id, name, code, price, stokBox, aksi);
        row.getStyleClass().add("table-row");
        row.setPadding(new Insets(10, 0, 10, 0));
        return row;
    }

    private void handleDelete(Product p) {
        new Thread(() -> {
            try {
                int productId = Integer.parseInt(p.getId());
                ApiService.deleteProduct(productId);
                Platform.runLater(this::loadProductsFromBackend);
            } catch (Exception e) {
                // silently fail
            }
        }).start();
    }

    private void startEdit(Product p) {
        editingProduct = p;
        namaProdukField.setText(p.getName());
        kodeProdukField.setText(p.getCode());
        hargaField.setText(String.valueOf(p.getPrice()));
        stokField.setText(String.valueOf(p.getStock()));
        deskripsiField.setText(p.getDescription());
        currentImageBase64 = p.getImage();
        if (imageNameLabel != null) {
            imageNameLabel.setText(currentImageBase64 != null ? "Gambar tersimpan" : "Belum ada gambar");
        }
        saveButton.setText("Update Produk");
    }

    @FXML
    private void handleChooseImage() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Pilih Gambar Produk");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg")
        );
        File file = fileChooser.showOpenDialog(null);
        if (file != null) {
            try {
                byte[] fileContent = Files.readAllBytes(file.toPath());
                currentImageBase64 = Base64.getEncoder().encodeToString(fileContent);
                if (imageNameLabel != null) {
                    imageNameLabel.setText(file.getName());
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
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
