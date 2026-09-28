package com.mrcdprm.emailclient.ui;

import com.mrcdprm.emailclient.mail.MailAccount;
import com.mrcdprm.emailclient.mail.MailReader;
import com.mrcdprm.emailclient.mail.MailSender;
import com.mrcdprm.emailclient.mail.Security;
import com.mrcdprm.emailclient.mail.ServerPreset;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.Spinner;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/**
 * Hesap kurulum penceresi. "Bağlan"a basınca bilgiler IMAP ve SMTP sunucusunda denenir;
 * pencere ancak giriş başarılı olursa kapanır ve hesabı döndürür.
 */
public final class AccountDialog extends Dialog<MailAccount> {

    private final TextField email = new TextField();
    private final PasswordField password = new PasswordField();
    private final ComboBox<ServerPreset> provider = new ComboBox<>();
    private final TextField imapHost = new TextField();
    private final Spinner<Integer> imapPort = new Spinner<>(1, 65535, 993);
    private final TextField smtpHost = new TextField();
    private final Spinner<Integer> smtpPort = new Spinner<>(1, 65535, 587);
    private final ComboBox<Security> smtpSecurity = new ComboBox<>();
    private final Label error = new Label();
    private final ProgressIndicator progress = new ProgressIndicator();
    private MailAccount verified;

    public AccountDialog(Runnable openAppPasswordHelp) {
        setTitle("E-posta hesabı");
        setHeaderText("Hesabını ekle");

        final ButtonType connectType = new ButtonType("Bağlan", ButtonBar.ButtonData.OK_DONE);
        getDialogPane().getButtonTypes().addAll(connectType, ButtonType.CANCEL);
        getDialogPane().getStylesheets().add(getClass().getResource("/com/mrcdprm/emailclient/app.css").toExternalForm());

        email.setPromptText("ornek@gmail.com");
        password.setPromptText("Gmail için 16 haneli uygulama şifresi");
        provider.getItems().addAll(ServerPreset.values());
        provider.setValue(ServerPreset.GMAIL);
        smtpSecurity.getItems().addAll(Security.SSL, Security.STARTTLS);
        smtpSecurity.setValue(Security.STARTTLS);
        imapPort.setEditable(true);
        smtpPort.setEditable(true);

        // Adres yazıldıkça sağlayıcı otomatik seçilir
        email.textProperty().addListener((obs, old, text) -> {
            final ServerPreset detected = ServerPreset.detect(text);
            if (detected != ServerPreset.CUSTOM)
                provider.setValue(detected);
        });

        final GridPane custom = new GridPane();
        custom.setHgap(8);
        custom.setVgap(8);
        custom.addRow(0, new Label("IMAP sunucusu"), imapHost, new Label("Port"), imapPort);
        custom.addRow(1, new Label("SMTP sunucusu"), smtpHost, new Label("Port"), smtpPort);
        custom.addRow(2, new Label("SMTP güvenliği"), smtpSecurity);
        custom.visibleProperty().bind(provider.valueProperty().isEqualTo(ServerPreset.CUSTOM));
        custom.managedProperty().bind(custom.visibleProperty()); // gizliyken yer kaplamasın

        final GridPane form = new GridPane();
        form.setHgap(8);
        form.setVgap(8);
        form.addRow(0, new Label("E-posta"), email);
        form.addRow(1, new Label("Şifre"), password);
        form.addRow(2, new Label("Sağlayıcı"), provider);
        email.setPrefColumnCount(26);

        final Hyperlink help = new Hyperlink("Gmail uygulama şifresi nasıl alınır?");
        help.setOnAction(e -> openAppPasswordHelp.run());
        final Label note = new Label("Outlook / Hotmail hesapları Microsoft'un zorunlu kıldığı OAuth2 girişini "
                + "gerektirdiği için bu sürümde desteklenmiyor.");
        note.setWrapText(true);
        note.getStyleClass().add("hint");

        error.getStyleClass().add("error");
        error.setWrapText(true);
        error.setVisible(false);
        error.managedProperty().bind(error.visibleProperty());
        progress.setMaxSize(22, 22);
        progress.setVisible(false);
        final Label progressText = new Label("Bağlanılıyor…");
        progressText.visibleProperty().bind(progress.visibleProperty());

        final VBox content = new VBox(10, form, custom, help, note, error, new HBox(8, progress, progressText));
        content.setPadding(new Insets(10));
        content.setPrefWidth(460);
        getDialogPane().setContent(content);

        // "Bağlan" pencereyi hemen kapatmasın: önce bilgiler arka planda denenir
        final Node connectButton = getDialogPane().lookupButton(connectType);
        connectButton.addEventFilter(ActionEvent.ACTION, event -> {
            event.consume();
            verifyInBackground((Button) connectButton);
        });
        setResultConverter(button -> button == connectType ? verified : null);
    }

    private void verifyInBackground(Button connectButton) {
        final MailAccount account;
        try {
            account = buildAccount();
        } catch (IllegalArgumentException e) {
            showError(e.getMessage());
            return;
        }

        final Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                try (MailReader reader = MailReader.connect(account)) {
                    reader.folderNames();
                }
                MailSender.verify(account);
                return null;
            }
        };
        task.setOnSucceeded(e -> {
            verified = account;
            setResult(account);
            close();
        });
        task.setOnFailed(e -> {
            setBusy(false, connectButton);
            showError(ErrorMessages.of(task.getException()));
        });
        setBusy(true, connectButton);
        final Thread thread = new Thread(task, "account-verify");
        thread.setDaemon(true);
        thread.start();
    }

    private MailAccount buildAccount() {
        final ServerPreset preset = provider.getValue();
        if (preset != ServerPreset.CUSTOM)
            return MailAccount.fromPreset(preset, email.getText(), password.getText());
        return new MailAccount(email.getText(), password.getText(),
                imapHost.getText(), imapPort.getValue(), Security.SSL,
                smtpHost.getText(), smtpPort.getValue(), smtpSecurity.getValue());
    }

    private void setBusy(boolean busy, Button connectButton) {
        connectButton.setDisable(busy);
        progress.setVisible(busy);
        if (busy)
            error.setVisible(false);
    }

    private void showError(String message) {
        error.setText(message);
        error.setVisible(true);
    }
}
