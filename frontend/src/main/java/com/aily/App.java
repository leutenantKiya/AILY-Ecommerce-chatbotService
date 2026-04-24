package com.aily;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Screen;
import javafx.stage.Stage;

import java.io.IOException;

public class App extends Application {

    private static Stage primaryStage;

    @Override
    public void start(Stage stage) throws IOException {
        primaryStage = stage;
        stage.setTitle("AILY E-commerce Chatbot");
        stage.setResizable(true);
        switchScene("landing");
        stage.show();
    }

    /** Keeps compatibility with older callers; the app now always opens maximized. */
    public static void switchScene(String fxmlName, double width, double height) throws IOException {
        switchScene(fxmlName);
    }

    public static void switchScene(String fxmlName) throws IOException {
        FXMLLoader loader = new FXMLLoader(App.class.getResource(fxmlName + ".fxml"));
        Parent root = loader.load();

        Rectangle2D bounds = Screen.getPrimary().getVisualBounds();
        Scene scene = new Scene(root, bounds.getWidth(), bounds.getHeight());
        scene.getStylesheets().add(App.class.getResource("style.css").toExternalForm());

        primaryStage.setScene(scene);
        primaryStage.setX(bounds.getMinX());
        primaryStage.setY(bounds.getMinY());
        primaryStage.setWidth(bounds.getWidth());
        primaryStage.setHeight(bounds.getHeight());
        primaryStage.setMaximized(true);
    }

    public static Stage getPrimaryStage() { return primaryStage; }

    public static void main(String[] args) { launch(); }
}
