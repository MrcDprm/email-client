package com.mrcdprm.emailclient;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.stage.Stage;

public class EmailClientApp extends Application {

    public static final String VERSION = "1.0.0";

    @Override
    public void start(Stage stage) {
        stage.setTitle("Email Client");
        stage.setScene(new Scene(new Label("Merhaba JavaFX! Sürüm " + VERSION), 1000, 700));
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
