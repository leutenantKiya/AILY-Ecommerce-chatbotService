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

    private void loadProductsFromBackend() {
        new Thread(() -> {
            try {
                JsonObject response = ApiService.getProduk();
                Platform.runLater(() -> {
                    products.clear();
                    if (response.has("status") && response.get("status").getAsInt() == 200) {
                        JsonObject data = response.getAsJsonObject("data");
                        if (data.has("products")) {
                            JsonArray arr = data.getAsJsonArray("products");
                            for (int i = 0; i < arr.size(); i++) {
                                JsonObject p = arr.get(i).getAsJsonObject();
                                String id = String.valueOf(p.get("id").getAsInt());
                                String name = p.get("name").getAsString();
                                String code = p.has("category") ? p.get("category").getAsString() : "";
                                long price = p.get("price").getAsLong();
                                int stock = p.get("stock").getAsInt();
                                String description = p.has("description") && !p.get("description").isJsonNull()
                                        ? p.get("description").getAsString() : "";
                                String imageStr = p.has("image") && !p.get("image").isJsonNull()
                                        ? p.get("image").getAsString() : null;
                                products.add(new Product(id, name, code, price, stock, description, imageStr));
                            }
                        }
                    }
                    refreshTable();
                });
            } catch (Exception e) {
                Platform.runLater(this::refreshTable);
            }
        }).start();
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

        saveButton.setDisable(true);

        if (editingProduct != null) {
            int productId = Integer.parseInt(editingProduct.getId());
            new Thread(() -> {
                try {
                    ApiService.updateProduct(productId, name, (int) price, stock, desc, code, "U", null, currentImageBase64);
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
                    ApiService.addProduct(name, (int) price, stock, desc, code, "U", null, currentImageBase64);
                    Platform.runLater(() -> {
                        saveButton.setDisable(false);
                        clearForm();
                        loadProductsFromBackend();
                    });
                } catch (Exception e) {
                    Platform.runLater(() -> saveButton.setDisable(false));
                }
            }).start();
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

    @FXML private void goOverview()     { try { App.switchScene("admin_overview"); } catch (Exception ignored) {} }
    @FXML private void goProducts()     { /* already here */ }
    @FXML private void goStoreInfo()    { try { App.switchScene("admin_store"); } catch (Exception ignored) {} }
    @FXML private void goTransactions() { try { App.switchScene("admin_transactions"); } catch (Exception ignored) {} }
    @FXML private void goChatHistory()  { try { App.switchScene("admin_chat"); } catch (Exception ignored) {} }

    @FXML
    private void handleLogout() {
        Session.clear();
        try { App.switchScene("landing"); } catch (Exception ignored) {}
    }
}
