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
    private static boolean firstSceneLoaded = false;

    @Override
    public void start(Stage stage) throws IOException {
        primaryStage = stage;
        stage.setTitle("AILY E-commerce Chatbot");
        stage.setResizable(true);
        switchScene("landing");
        stage.sizeToScene();
        stage.show();
    }

    /** Keeps compatibility with older callers; the app now always opens maximized. */
    public static void switchScene(String fxmlName, double width, double height) throws IOException {
        switchScene(fxmlName);
    }

    public static void switchScene(String fxmlName) throws IOException {
        FXMLLoader loader = new FXMLLoader(App.class.getResource(fxmlName + ".fxml"));
        Parent root = loader.load();

        Scene scene;
        if (!firstSceneLoaded) {
            Rectangle2D bounds = Screen.getPrimary().getVisualBounds();
            scene = new Scene(root, bounds.getWidth(), bounds.getHeight());
        } else {
            scene = new Scene(root, primaryStage.getWidth(), primaryStage.getHeight());
        }
        scene.getStylesheets().add(App.class.getResource("style.css").toExternalForm());

        boolean wasMaximized = primaryStage.isMaximized();
        primaryStage.setScene(scene);

        if (!firstSceneLoaded) {
            Rectangle2D bounds = Screen.getPrimary().getVisualBounds();
            primaryStage.setX(bounds.getMinX());
            primaryStage.setY(bounds.getMinY());
            primaryStage.setWidth(bounds.getWidth());
            primaryStage.setHeight(bounds.getHeight());
            primaryStage.setMaximized(true);
            firstSceneLoaded = true;
        } else if (wasMaximized) {
            primaryStage.setMaximized(true);
        }
    }

    public static Stage getPrimaryStage() { return primaryStage; }

    public static void main(String[] args) { launch(); }
}
