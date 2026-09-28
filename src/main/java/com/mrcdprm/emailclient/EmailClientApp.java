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
import javafx.scene.image.Image;
import javafx.stage.Stage;

public class EmailClientApp extends Application {

    public static final String VERSION = "1.0.0";
    public static final String APP_PASSWORD_HELP = "https://support.google.com/accounts/answer/185833";

    private final AccountStore store = new AccountStore(AccountStore.defaultDirectory());
    private Stage stage;
    private MainView mainView;

    @Override
    public void start(Stage primaryStage) {
        Application.setUserAgentStylesheet(new PrimerLight().getUserAgentStylesheet()); // modern tema
        stage = primaryStage;
        stage.getIcons().add(new Image(getClass().getResourceAsStream("/com/mrcdprm/emailclient/icon.png")));
        stage.setOnHidden(e -> {
            if (mainView != null)
                mainView.shutdown();
        });

        // Önce ortam değişkenleri (geliştirme), sonra kayıtlı hesap, ikisi de yoksa kurulum penceresi
        final Optional<MailAccount> account = AccountStore.fromEnvironment(System.getenv()).or(store::load);
        if (account.isPresent())
            showMain(account.get());
        else
            showAccountSetup();
    }

    private void showAccountSetup() {
        final Optional<MailAccount> account =
                new AccountDialog(() -> getHostServices().showDocument(APP_PASSWORD_HELP)).showAndWait();
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
        showMain(account.get());
    }

    private void showMain(MailAccount account) {
        mainView = new MainView(account, url -> getHostServices().showDocument(url), this::logout);
        final Scene scene = new Scene(mainView, 1200, 760);
        scene.getStylesheets().add(getClass().getResource("/com/mrcdprm/emailclient/app.css").toExternalForm());
        stage.setTitle("Email Client - " + account.email());
        stage.setScene(scene);
        stage.show();
    }

    /** Hesaptan çıkış: bağlantı kapanır, kayıtlı bilgiler silinir, kurulum penceresi tekrar açılır. */
    private void logout() {
        mainView.shutdown();
        mainView = null;
        try {
            store.delete();
        } catch (IOException e) {
            new Alert(Alert.AlertType.WARNING, "Kayıtlı hesap bilgisi silinemedi.").showAndWait();
        }
        showAccountSetup(); // ana pencere açık kalır; gizlenirse JavaFX son pencere kapandı sanıp uygulamayı kapatır
    }

    public static void main(String[] args) {
        launch(args);
    }
}