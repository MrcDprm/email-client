package com.mrcdprm.emailclient.ui;

import com.mrcdprm.emailclient.EmailClientApp;
import java.util.function.Consumer;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

/** Hakkında penceresi: sürüm, açıklama, lisans ve kullanılan kütüphaneler. */
public final class AboutDialog extends Dialog<Void> {

    private static final String REPO_URL = "https://github.com/MrcDprm/email-client";

    public AboutDialog(Window owner, Consumer<String> openUrl) {
        initOwner(owner);
        setTitle("Hakkında");
        getDialogPane().getButtonTypes().add(new ButtonType("Kapat", ButtonType.CLOSE.getButtonData()));
        getDialogPane().getStylesheets().add(getClass().getResource("/com/mrcdprm/emailclient/app.css").toExternalForm());

        final ImageView icon = new ImageView(new Image(getClass().getResourceAsStream("/com/mrcdprm/emailclient/icon.png")));
        icon.setFitWidth(64);
        icon.setFitHeight(64);

        final Label title = new Label("Email Client");
        title.getStyleClass().add("about-title");
        final Label version = new Label("Sürüm " + EmailClientApp.VERSION);
        version.getStyleClass().add("hint");
        final Label description = new Label("Java ve JavaFX ile yazılmış masaüstü e-posta istemcisi. "
                + "IMAP ile okur, SMTP ile gönderir. Şifre Windows DPAPI ile şifreli saklanır.");
        description.setWrapText(true);
        final Hyperlink repo = new Hyperlink("GitHub'da kaynak kodu");
        repo.setOnAction(e -> openUrl.accept(REPO_URL));
        final Label license = new Label("MIT Lisansı · © 2026 Miraç Deprem\n"
                + "JavaFX, Jakarta Mail (Eclipse Angus), AtlantaFX, Ikonli, jsoup, JNA");
        license.getStyleClass().add("hint");

        final VBox text = new VBox(6, title, version, description, repo, license);
        final HBox content = new HBox(16, icon, text);
        content.setAlignment(Pos.TOP_LEFT);
        content.setPadding(new Insets(10));
        content.setPrefWidth(440);
        getDialogPane().setContent(content);
    }
}