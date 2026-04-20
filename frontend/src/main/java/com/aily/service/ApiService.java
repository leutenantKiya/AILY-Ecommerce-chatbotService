package com.aily.service;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class ApiService {

    private static final String BASE_URL = "http://localhost:8000";
    private static final HttpClient client = HttpClient.newHttpClient();
    private static final Gson gson = new Gson();

    // ── Auth ──────────────────────────────────────────────────────────────────

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

    public static JsonObject register(String username, String password,
                                      String email, String phone,
                                      String address, String role) throws Exception {
        String url = BASE_URL + "/aily/registration"
                + "?uname=" + encode(username)
                + "&pword=" + encode(password)
                + "&email=" + encode(email)
                + "&phone=" + encode(phone)
                + "&add=" + encode(address)
                + "&role=" + encode(role);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .POST(HttpRequest.BodyPublishers.noBody())
                .header("Content-Type", "application/json")
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        return gson.fromJson(response.body(), JsonObject.class);
    }

    // ── Conversation ──────────────────────────────────────────────────────────

    public static JsonObject sendMessage(String hashedPassword, String message) throws Exception {
        String url = BASE_URL + "/aily/conversation/" + encode(hashedPassword);

        JsonObject body = new JsonObject();
        body.addProperty("message", message);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                .header("Content-Type", "application/json")
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        return gson.fromJson(response.body(), JsonObject.class);
    }

    // ── Toko ──────────────────────────────────────────────────────────────────

    public static JsonObject getTentangToko() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/aily/tentangToko"))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        return gson.fromJson(response.body(), JsonObject.class);
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private static String encode(String value) {
        return java.net.URLEncoder.encode(value, java.nio.charset.StandardCharsets.UTF_8);
    }
}
