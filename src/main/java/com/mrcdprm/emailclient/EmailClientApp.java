package com.mrcdprm.emailclient;

import atlantafx.base.theme.PrimerLight;
import com.mrcdprm.emailclient.mail.MailAccount;
import com.mrcdprm.emailclient.settings.AccountStore;
import com.mrcdprm.emailclient.ui.AccountDialog;
import com.mrcdprm.emailclient.ui.MainView;
import java.io.IOException;
import java.util.Optional;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Stage;

public class EmailClientApp extends Application {

    public static final String VERSION = "1.0.0";
    public static final String APP_PASSWORD_HELP = "https://support.google.com/accounts/answer/185833";

    @Override
    public void start(Stage stage) {
        Application.setUserAgentStylesheet(new PrimerLight().getUserAgentStylesheet()); // modern tema        
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

        final MainView mainView = new MainView(account.get());
        final Scene scene = new Scene(mainView, 1100, 720);
        scene.getStylesheets().add(getClass().getResource("/com/mrcdprm/emailclient/app.css").toExternalForm());
        stage.setTitle("Email Client - " + account.get().email());
        stage.setScene(scene);
        stage.setOnHidden(e -> mainView.shutdown());
        stage.show();

    }

    public static void main(String[] args) {
        launch(args);
    }
}