package com.aily;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class App extends Application {

    private static Stage primaryStage;

    @Override
    public void start(Stage stage) throws IOException {
        primaryStage = stage;
        stage.setTitle("AILY E-commerce Chatbot");
        stage.setResizable(true);
        switchScene("landing", 1000, 700);
        stage.show();
    }

    /** Switch to a scene and resize the window. */
    public static void switchScene(String fxmlName, double width, double height) throws IOException {
        FXMLLoader loader = new FXMLLoader(App.class.getResource(fxmlName + ".fxml"));
        Scene scene = new Scene(loader.load(), width, height);
        scene.getStylesheets().add(App.class.getResource("style.css").toExternalForm());
        primaryStage.setScene(scene);
        primaryStage.setWidth(width);
        primaryStage.setHeight(height);
        primaryStage.centerOnScreen();
    }

    /** Convenience overload — keeps current window size. */
    public static void switchScene(String fxmlName) throws IOException {
        switchScene(fxmlName, primaryStage.getWidth(), primaryStage.getHeight());
    }

    public static Stage getPrimaryStage() { return primaryStage; }

    public static void main(String[] args) { launch(); }
}
