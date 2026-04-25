package com.aily.service;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class ApiService {

    private static final String BASE_URL = "http://localhost:8000";
    private static final HttpClient client = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .build();
    private static final Gson gson = new Gson();

    public static JsonObject login(String username, String password) throws Exception {
        String url = BASE_URL + "/aily/login?uname=" + encode(username) + "&pword=" + encode(password);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .POST(HttpRequest.BodyPublishers.noBody())
                .header("Content-Type", "application/json")
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        return gson.fromJson(response.body(), JsonObject.class);
    }

    public static JsonObject deleteChat(String userId) throws Exception {
        String url = BASE_URL + "/aily/user/conversation/chat/delete?user_id=" + encode(userId);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .DELETE()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        return gson.fromJson(response.body(), JsonObject.class);
    }

    public static JsonObject register(String username, String password,
                                      String email, String phone,
                                      String address, String role) throws Exception {
        return register(username, password, email, phone, address, role, "L");
    }

    public static JsonObject register(String username, String password,
                                      String email, String phone,
                                      String address, String role,
                                      String gender) throws Exception {
        String url = BASE_URL + "/aily/registration"
                + "?uname=" + encode(username)
                + "&pword=" + encode(password)
                + "&email=" + encode(email)
                + "&phone=" + encode(phone)
                + "&add=" + encode(address)
                + "&role=" + encode(role)
                + "&gender=" + encode(gender);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .POST(HttpRequest.BodyPublishers.noBody())
                .header("Content-Type", "application/json")
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        return gson.fromJson(response.body(), JsonObject.class);
    }

    public static JsonObject sendMessage(String hashedPassword, String message) throws Exception {
        String url = BASE_URL + "/aily/conversation";

        JsonObject body = new JsonObject();
        body.addProperty("id", hashedPassword);
        body.addProperty("message", message);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                .header("Content-Type", "application/json")
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        return gson.fromJson(response.body(), JsonObject.class);
    }

    public static JsonObject getChatHistory(String userId) throws Exception {
        String url = BASE_URL + "/aily/user/conversation/chat/load?user_id=" + encode(userId);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        return gson.fromJson(response.body(), JsonObject.class);
    }

    public static JsonObject saveChatMessage(String userId, String username,
                                             String role, String message) throws Exception {
        String url = BASE_URL + "/aily/user/conversation/chat/save";

        JsonObject body = new JsonObject();
        body.addProperty("user_id", userId);
        body.addProperty("username", username);
        body.addProperty("role", role);
        body.addProperty("message", message);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                .header("Content-Type", "application/json")
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        return gson.fromJson(response.body(), JsonObject.class);
    }

    public static JsonObject getTentangToko() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/aily/tentangToko"))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        return gson.fromJson(response.body(), JsonObject.class);
    }

    public static JsonObject getProduk() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/aily/admin/product/list"))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        return gson.fromJson(response.body(), JsonObject.class);
    }

    public static JsonObject addProduct(String name, int price, int stock,
                                        String description, String category,
                                        String gender, String warna, String image) throws Exception {
        JsonObject body = new JsonObject();
        body.addProperty("name", name);
        body.addProperty("price", price);
        body.addProperty("stock", stock);
        body.addProperty("image", image);
        body.addProperty("description", description);
        body.addProperty("category", category);
        body.addProperty("gender", gender != null ? gender : "U");
        body.addProperty("warna", warna);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/aily/admin/product/add"))
                .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                .header("Content-Type", "application/json")
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        return gson.fromJson(response.body(), JsonObject.class);
    }

    public static JsonObject updateProduct(int productId, String name, int price, int stock,
                                           String description, String category,
                                           String gender, String warna, String image) throws Exception {
        JsonObject body = new JsonObject();
        body.addProperty("name", name);
        body.addProperty("price", price);
        body.addProperty("stock", stock);
        body.addProperty("image", image);
        body.addProperty("description", description);
        body.addProperty("category", category);
        body.addProperty("gender", gender != null ? gender : "U");
        body.addProperty("warna", warna);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/aily/admin/product/update/" + productId))
                .PUT(HttpRequest.BodyPublishers.ofString(body.toString()))
                .header("Content-Type", "application/json")
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        return gson.fromJson(response.body(), JsonObject.class);
    }

    public static JsonObject deleteProduct(int productId) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/aily/admin/product/delete/" + productId))
                .DELETE()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        return gson.fromJson(response.body(), JsonObject.class);
    }

    public static JsonObject getUserCart(String hashedPassword) throws Exception {
        String url = BASE_URL + "/aily/user/cart?user_id=" + encode(hashedPassword);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        return gson.fromJson(response.body(), JsonObject.class);
    }

    public static JsonObject addToCart(String hashedPassword, int productId, int quantity) throws Exception {
        JsonObject body = new JsonObject();
        body.addProperty("user_id", hashedPassword);
        body.addProperty("product_id", productId);
        body.addProperty("quantity", quantity);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/aily/user/cart/add"))
                .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                .header("Content-Type", "application/json")
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        return gson.fromJson(response.body(), JsonObject.class);
    }

    public static JsonObject updateCartItem(String hashedPassword, int productId, int quantity) throws Exception {
        JsonObject body = new JsonObject();
        body.addProperty("user_id", hashedPassword);
        body.addProperty("product_id", productId);
        body.addProperty("quantity", quantity);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/aily/user/cart/item"))
                .PUT(HttpRequest.BodyPublishers.ofString(body.toString()))
                .header("Content-Type", "application/json")
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        return gson.fromJson(response.body(), JsonObject.class);
    }

    public static JsonObject removeFromCart(String hashedPassword, int productId) throws Exception {
        String url = BASE_URL + "/aily/user/cart/item?user_id=" + encode(hashedPassword) + "&product_id=" + productId;
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .DELETE()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        return gson.fromJson(response.body(), JsonObject.class);
    }

    public static JsonObject clearUserCart(String hashedPassword) throws Exception {
        String url = BASE_URL + "/aily/user/cart/clear?user_id=" + encode(hashedPassword);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .DELETE()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        return gson.fromJson(response.body(), JsonObject.class);
    }

    public static JsonObject checkout(String hashedPassword) throws Exception {
        JsonObject body = new JsonObject();
        body.addProperty("user_id", hashedPassword);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/aily/user/checkout"))
                .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                .header("Content-Type", "application/json")
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        return gson.fromJson(response.body(), JsonObject.class);
    }

    public static JsonObject getUserOrders(String hashedPassword) throws Exception {
        String url = BASE_URL + "/aily/user/orders?user_id=" + encode(hashedPassword);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        return gson.fromJson(response.body(), JsonObject.class);
    }

    public static JsonObject cancelOrder(String hashedPassword, int orderId) throws Exception {
        String url = BASE_URL + "/aily/user/orders/" + orderId + "/cancel?user_id=" + encode(hashedPassword);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        return gson.fromJson(response.body(), JsonObject.class);
    }

    public static JsonObject getAdminOrders() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/aily/admin/orders"))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        return gson.fromJson(response.body(), JsonObject.class);
    }

    public static JsonObject updateOrderStatus(int orderId, String status) throws Exception {
        JsonObject body = new JsonObject();
        body.addProperty("status", status);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/aily/admin/orders/" + orderId + "/status"))
                .PUT(HttpRequest.BodyPublishers.ofString(body.toString()))
                .header("Content-Type", "application/json")
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        return gson.fromJson(response.body(), JsonObject.class);
    }

    public static JsonObject updateUserProfile(String hashedPassword, String[][] updates) throws Exception {
        String url = BASE_URL + "/aily/user/updateUser?id=" + encode(hashedPassword);

        JsonArray dataList = new JsonArray();
        for (String[] pair : updates) {
            JsonArray item = new JsonArray();
            item.add(pair[0]);
            item.add(pair[1]);
            dataList.add(item);
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .POST(HttpRequest.BodyPublishers.ofString(dataList.toString()))
                .header("Content-Type", "application/json")
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        return gson.fromJson(response.body(), JsonObject.class);
    }

    public static JsonObject getUserProfile(String hashedPassword) throws Exception {
        String url = BASE_URL + "/aily/user/profile?id=" + encode(hashedPassword);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        return gson.fromJson(response.body(), JsonObject.class);
    }

    public static JsonObject getStoreInfoAdmin() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/aily/admin/store-info/list"))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        return gson.fromJson(response.body(), JsonObject.class);
    }

    public static JsonObject addStoreInfo(String question, String answer) throws Exception {
        JsonObject body = new JsonObject();
        body.addProperty("question", question);
        body.addProperty("answer", answer);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/aily/admin/store-info/add"))
                .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                .header("Content-Type", "application/json")
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        return gson.fromJson(response.body(), JsonObject.class);
    }

    public static JsonObject updateStoreInfo(int id, String question, String answer) throws Exception {
        JsonObject body = new JsonObject();
        body.addProperty("question", question);
        body.addProperty("answer", answer);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/aily/admin/store-info/update/" + id))
                .PUT(HttpRequest.BodyPublishers.ofString(body.toString()))
                .header("Content-Type", "application/json")
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        return gson.fromJson(response.body(), JsonObject.class);
    }

    public static JsonObject deleteStoreInfo(int id) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/aily/admin/store-info/delete/" + id))
                .DELETE()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        return gson.fromJson(response.body(), JsonObject.class);
    }

    private static String encode(String value) {
        return java.net.URLEncoder.encode(value, java.nio.charset.StandardCharsets.UTF_8);
    }
}
