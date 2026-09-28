package com.mrcdprm.emailclient;

import com.mrcdprm.emailclient.mail.MailAccount;
import com.mrcdprm.emailclient.settings.AccountStore;
import com.mrcdprm.emailclient.ui.AccountDialog;
import java.io.IOException;
import java.util.Optional;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.stage.Stage;

public class EmailClientApp extends Application {

    public static final String VERSION = "1.0.0";
    public static final String APP_PASSWORD_HELP = "https://support.google.com/accounts/answer/185833";

    @Override
    public void start(Stage stage) {
        final AccountStore store = new AccountStore(AccountStore.defaultDirectory());

        // Önce ortam değişkenleri (geliştirme), sonra kayıtlı hesap, ikisi de yoksa kurulum penceresi
        Optional<MailAccount> account = AccountStore.fromEnvironment(System.getenv()).or(store::load);
        if (account.isEmpty()) {
            account = new AccountDialog(() -> getHostServices().showDocument(APP_PASSWORD_HELP)).showAndWait();
            if (account.isEmpty()) {
                Platform.exit();
                return;
            }
            try {
                store.save(account.get());
            } catch (IOException e) {
                new Alert(Alert.AlertType.WARNING, "Hesap bilgisi kaydedilemedi; bir sonraki açılışta tekrar sorulacak.")
                        .showAndWait();
            }
        }

        stage.setTitle("Email Client - " + account.get().email());
        stage.setScene(new Scene(new Label("Bağlandı: " + account.get().email()), 1000, 700));
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}