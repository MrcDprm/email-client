package com.mrcdprm.emailclient.ui;

import jakarta.mail.internet.AddressException;
import atlantafx.base.theme.Styles;
import com.mrcdprm.emailclient.mail.MailAccount;
import com.mrcdprm.emailclient.mail.MailSender;
import com.mrcdprm.emailclient.mail.MessageSummary;
import java.util.function.Consumer;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.stage.Stage;
import javafx.stage.Window;
import org.kordamp.ikonli.feather.Feather;

/** Yeni mesaj, yanıt ve iletme penceresi. Gönderim arka planda yapılır; pencere ancak başarılı olunca kapanır. */
public final class ComposeWindow extends Stage {

    private final MailAccount account;
    private final String inReplyTo;
    private final Consumer<String> onSent;
    private final TextField to = new TextField();
    private final TextField cc = new TextField();
    private final TextField subject = new TextField();
    private final TextArea body = new TextArea();
    private final Label error = new Label();
    private final Button send = new Button("Gönder", Icons.of(Feather.SEND));
    private final ProgressIndicator progress = new ProgressIndicator();
    private boolean sent;
    private String initialState = "";

    private ComposeWindow(Window owner, MailAccount account, String title, String inReplyTo, Consumer<String> onSent) {
        this.account = account;
        this.inReplyTo = inReplyTo;
        this.onSent = onSent;
        initOwner(owner);
        setTitle(title);

        to.setPromptText("ornek@gmail.com, ikinci@ornek.com");
        cc.setPromptText("İsteğe bağlı");
        subject.setPromptText("Konu");
        body.setWrapText(true);
        body.getStyleClass().add("compose-body");

        final GridPane header = new GridPane();
        header.setHgap(10);
        header.setVgap(8);
        header.addRow(0, new Label("Kime"), to);
        header.addRow(1, new Label("Bilgi (Cc)"), cc);
        header.addRow(2, new Label("Konu"), subject);
        GridPane.setHgrow(to, Priority.ALWAYS);

        error.getStyleClass().add("error");
        error.setWrapText(true);
        error.setVisible(false);
        error.managedProperty().bind(error.visibleProperty());

        send.getStyleClass().add(Styles.ACCENT);
        send.setOnAction(e -> sendInBackground());
        final Button cancel = new Button("Vazgeç");
        cancel.setOnAction(e -> {
            if (confirmDiscard())
                close();
        });
        progress.setMaxSize(18, 18);
        progress.setVisible(false);
        final Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        final HBox actions = new HBox(10, error, spacer, progress, cancel, send);
        actions.setAlignment(Pos.CENTER_RIGHT);

        final BorderPane root = new BorderPane(body, header, null, actions, null);
        root.setPadding(new Insets(14));
        BorderPane.setMargin(body, new Insets(12, 0, 12, 0));
        final Scene scene = new Scene(root, 680, 520);
        scene.getStylesheets().add(getClass().getResource("/com/mrcdprm/emailclient/app.css").toExternalForm());
        // Ctrl+Enter ile gönder (Gmail ve Outlook'taki gibi)
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.ENTER, KeyCombination.SHORTCUT_DOWN), this::sendInBackground);
        setScene(scene);

        // Yazılmış bir mesaj yanlışlıkla kaybolmasın
        setOnCloseRequest(event -> {
            if (!confirmDiscard())
                event.consume();
        });
    }

    public static ComposeWindow newMessage(Window owner, MailAccount account, Consumer<String> onSent) {
        final ComposeWindow window = new ComposeWindow(owner, account, "Yeni mesaj", null, onSent);
        window.to.requestFocus();
        window.markClean();
        return window;
    }

    public static ComposeWindow reply(Window owner, MailAccount account, MessageSummary original, String originalBody,
                                      Consumer<String> onSent) {
        final ComposeWindow window = new ComposeWindow(owner, account, "Yanıtla", original.messageId(), onSent);
        window.to.setText(original.fromAddress() == null ? "" : original.fromAddress());
        window.subject.setText(MailSender.replySubject(original.subject()));
        window.body.setText(MailSender.quote(original, originalBody));
        window.body.positionCaret(0); // yanıt alıntının üstüne yazılır
        window.body.requestFocus();
        window.markClean();
        return window;
    }

    public static ComposeWindow forward(Window owner, MailAccount account, MessageSummary original, String originalBody,
                                        Consumer<String> onSent) {
        final ComposeWindow window = new ComposeWindow(owner, account, "İlet", null, onSent);
        window.subject.setText(MailSender.forwardSubject(original.subject()));
        window.body.setText("\n\n---------- İletilen mesaj ----------\n"
                + "Kimden: " + original.from() + (original.fromAddress() == null ? "" : " <" + original.fromAddress() + ">") + "\n"
                + "Tarih: " + Formats.longDate(original.date()) + "\n"
                + "Konu: " + original.subject() + "\n\n"
                + (originalBody == null ? "" : originalBody));
        window.body.positionCaret(0);
        window.to.requestFocus();
        window.markClean();
        return window;
    }

    private void sendInBackground() {
        if (send.isDisabled())
            return;
        final MailSender.Draft draft = new MailSender.Draft(to.getText(), cc.getText(), subject.getText(), body.getText(), inReplyTo);
        try {
            // Önce alıcılar: adres yanlışsa konu hakkında boşuna soru sorulmasın
            MailSender.parseRecipients(draft.to(), true);
            MailSender.parseRecipients(draft.cc(), false);
        } catch (AddressException e) {
            error.setText(e.getMessage());
            error.setVisible(true);
            return;
        }        
        if (draft.subject() == null || draft.subject().isBlank()) {
            final Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Konu boş. Yine de gönderilsin mi?", ButtonType.YES, ButtonType.NO);
            confirm.initOwner(this);
            if (confirm.showAndWait().orElse(ButtonType.NO) != ButtonType.YES)
                return;
        }

        final Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                MailSender.send(account, draft);
                return null;
            }
        };
        task.setOnSucceeded(e -> {
            sent = true;
            close();
            onSent.accept("Mesaj gönderildi.");
        });
        task.setOnFailed(e -> {
            setBusy(false);
            error.setText(ErrorMessages.of(task.getException()));
            error.setVisible(true);
        });
        setBusy(true);
        final Thread thread = new Thread(task, "smtp-send");
        thread.setDaemon(true);
        thread.start();
    }

    private void setBusy(boolean busy) {
        send.setDisable(busy);
        progress.setVisible(busy);
        if (busy)
            error.setVisible(false);
    }

    /** Pencere açıldığındaki hâl; kullanıcı bir şey değiştirmediyse kapatırken sormaya gerek yok. */
    private String snapshot() {
        return to.getText() + '\u0000' + cc.getText() + '\u0000' + subject.getText() + '\u0000' + body.getText();
    }

    private void markClean() {
        initialState = snapshot();
    }

    /** Gönderilmemiş bir değişiklik varsa kapatmadan önce sorar. */
    private boolean confirmDiscard() {
        if (sent || snapshot().equals(initialState))
            return true;
        final Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Mesaj gönderilmedi. Kapatılsın mı?", ButtonType.YES, ButtonType.NO);
        confirm.initOwner(this);
        return confirm.showAndWait().orElse(ButtonType.NO) == ButtonType.YES;
    }
}