package com.mrcdprm.emailclient.ui;

import java.util.Locale;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

/** Gönderenin baş harfiyle renkli yuvarlak avatar; aynı adres her zaman aynı rengi alır. */
public final class Avatar extends StackPane {

    private static final Color[] COLORS = {
        Color.web("#2563eb"), Color.web("#7c3aed"), Color.web("#db2777"), Color.web("#ea580c"),
        Color.web("#16a34a"), Color.web("#0891b2"), Color.web("#ca8a04"), Color.web("#4f46e5"),
    };

    private final Circle circle;
    private final Label initial = new Label();

    public Avatar(double size) {
        circle = new Circle(size / 2);
        initial.getStyleClass().add("avatar-initial");
        initial.setStyle("-fx-font-size: " + Math.round(size * 0.42) + "px;");
        getChildren().addAll(circle, initial);
        setMinSize(size, size);
        setMaxSize(size, size);
    }

    /** name ekranda görünen ad, key rengi belirleyen değer (e-posta adresi). */
    public void set(String name, String key) {
        final String text = name == null || name.isBlank() ? "?" : name.strip();
        initial.setText(text.substring(0, Character.charCount(text.codePointAt(0))).toUpperCase(Locale.forLanguageTag("tr")));
        final String colorKey = key == null ? text : key.toLowerCase(Locale.ROOT);
        circle.setFill(COLORS[Math.floorMod(colorKey.hashCode(), COLORS.length)]);
    }
}