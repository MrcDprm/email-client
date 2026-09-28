package com.mrcdprm.emailclient.ui;

import atlantafx.base.controls.CustomTextField;
import atlantafx.base.theme.Styles;
import atlantafx.base.theme.Tweaks;
import com.mrcdprm.emailclient.mail.MailAccount;
import com.mrcdprm.emailclient.mail.MailReader;
import com.mrcdprm.emailclient.mail.MessageSummary;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Locale;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.Separator;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.ToolBar;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import org.kordamp.ikonli.Ikon;
import org.kordamp.ikonli.feather.Feather;
import org.kordamp.ikonli.javafx.FontIcon;

/**
 * Ana ekran: solda hesap ve klasörler, ortada mesaj listesi, sağda okuma bölmesi.
 * Bütün IMAP işlemleri tek bir arka plan iş parçacığında sırayla çalışır; arayüz donmaz.
 */
public final class MainView extends BorderPane {

    private static final int PAGE_SIZE = 30;
    private static final Locale TURKISH = Locale.forLanguageTag("tr");

    private final MailAccount account;
    private final ExecutorService mailThread = Executors.newSingleThreadExecutor(runnable -> {
        final Thread thread = new Thread(runnable, "imap");
        thread.setDaemon(true);
        return thread;
    });
    private MailReader reader; // sadece mailThread içinde kullanılır

    private final ListView<String> folders = new ListView<>();
    private final ObservableList<MessageSummary> messages = FXCollections.observableArrayList();
    private final FilteredList<MessageSummary> visibleMessages = new FilteredList<>(messages);
    private final ListView<MessageSummary> messageList = new ListView<>(visibleMessages);
    private final Button loadMore = new Button("Daha fazla yükle");
    private final Label subject = new Label();
    private final Avatar senderAvatar = new Avatar(40);
    private final Label senderName = new Label();
    private final Label senderMeta = new Label();
    private final TextArea body = new TextArea();
    private final VBox readerContent;
    private final VBox readerEmpty;
    private final Button replyButton = iconButton(Feather.CORNER_UP_LEFT, "Yanıtla (Ctrl+R)");
    private final Button forwardButton = iconButton(Feather.CORNER_UP_RIGHT, "İlet");
    private final Button deleteButton = iconButton(Feather.TRASH_2, "Sil (Delete)");
    private final Button unreadButton = iconButton(Feather.MAIL, "Okunmadı yap");
    private final FontIcon unreadIcon = (FontIcon) unreadButton.getGraphic();    
    private final Label status = new Label();
    private final ProgressIndicator progress = new ProgressIndicator();
    private long shownUid = -1; // okuma bölmesinde gösterilmesi istenen son mesaj
    private String shownBody; // okuma bölmesindeki mesajın gövdesi (yüklenince dolar; yanıt / iletme için)
    private boolean replacing; // listedeki satır güncellenirken seçim olayları yok sayılır

    public MainView(MailAccount account, Consumer<String> openUrl, Runnable onLogout) {
        this.account = account;
        getStyleClass().add("main-view");

        // Üst araç çubuğu
        final Button compose = new Button("Yeni mesaj", Icons.of(Feather.EDIT));
        compose.getStyleClass().add(Styles.ACCENT);
        compose.setTooltip(new Tooltip("Yeni mesaj (Ctrl+N)"));
        compose.setOnAction(e -> openCompose(null, false));
        final Button refresh = iconButton(Feather.REFRESH_CW, "Yenile (F5)");
        refresh.setOnAction(e -> reloadMessages());
        replyButton.setOnAction(e -> openCompose(messageList.getSelectionModel().getSelectedItem(), false));
        forwardButton.setOnAction(e -> openCompose(messageList.getSelectionModel().getSelectedItem(), true));
        deleteButton.setOnAction(e -> deleteSelected());
        unreadButton.setOnAction(e -> toggleSeen());
        final CustomTextField search = new CustomTextField();
        search.setPromptText("Yüklenen mesajlarda ara");
        search.setLeft(Icons.of(Feather.SEARCH));
        search.setPrefWidth(260);
        search.textProperty().addListener((obs, old, text) -> filter(text));
        final MenuItem about = new MenuItem("Hakkında", Icons.of(Feather.INFO));
        about.setOnAction(e -> new AboutDialog(getScene().getWindow(), openUrl).showAndWait());
        final MenuItem logout = new MenuItem("Hesaptan çık", Icons.of(Feather.LOG_OUT));
        logout.setOnAction(e -> confirmLogout(onLogout));
        final MenuButton menu = new MenuButton(null, Icons.of(Feather.MORE_VERTICAL), about, new SeparatorMenuItem(), logout);
        menu.getStyleClass().addAll(Styles.BUTTON_ICON, Styles.FLAT, Tweaks.NO_ARROW);
        final Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        setTop(new ToolBar(compose, new Separator(Orientation.VERTICAL), refresh, replyButton, forwardButton,
                deleteButton, unreadButton, spacer, search, menu));

        // Sol: hesap ve klasörler
        final Avatar accountAvatar = new Avatar(36);
        accountAvatar.set(account.email(), account.email());
        final Label accountLabel = new Label(account.email());
        accountLabel.getStyleClass().add("account-label");
        final HBox accountBox = new HBox(10, accountAvatar, accountLabel);
        accountBox.setAlignment(Pos.CENTER_LEFT);
        accountBox.setPadding(new Insets(14, 12, 10, 12));
        folders.setCellFactory(list -> new FolderCell());
        folders.getSelectionModel().selectedItemProperty().addListener((obs, old, folder) -> reloadMessages());
        folders.getStyleClass().add("folder-list");
        final VBox sidebar = new VBox(accountBox, folders);
        sidebar.getStyleClass().add("sidebar");
        VBox.setVgrow(folders, Priority.ALWAYS);

        // Orta: mesaj listesi
        messageList.setCellFactory(list -> new MessageCell());
        messageList.setPlaceholder(new Label("Bu klasörde mesaj yok."));
        messageList.getSelectionModel().selectedItemProperty().addListener((obs, old, message) -> {
            if (!replacing)
                showMessage(message);
        });
        loadMore.setMaxWidth(Double.MAX_VALUE);
        loadMore.getStyleClass().add(Styles.FLAT);
        loadMore.setOnAction(e -> loadPage(messages.size()));
        loadMore.setVisible(false);
        loadMore.managedProperty().bind(loadMore.visibleProperty());
        final VBox listPane = new VBox(messageList, loadMore);
        VBox.setVgrow(messageList, Priority.ALWAYS);

        // Sağ: okuma bölmesi (seçim yoksa boş durum)
        subject.getStyleClass().add("reader-subject");
        subject.setWrapText(true);
        senderName.getStyleClass().add("sender-name");
        senderMeta.getStyleClass().add("hint");
        final HBox senderBox = new HBox(10, senderAvatar, new VBox(2, senderName, senderMeta));
        senderBox.setAlignment(Pos.CENTER_LEFT);
        body.setEditable(false); // metin seçilip kopyalanabilir ama değiştirilemez
        body.setWrapText(true);
        body.getStyleClass().add("reader-body");
        readerContent = new VBox(14, subject, senderBox, body);
        readerContent.setPadding(new Insets(20, 24, 20, 24));
        VBox.setVgrow(body, Priority.ALWAYS);
        final FontIcon emptyIcon = Icons.of(Feather.MAIL);
        emptyIcon.getStyleClass().add("empty-icon");
        readerEmpty = new VBox(10, emptyIcon, new Label("Okumak için bir mesaj seç"));
        readerEmpty.setAlignment(Pos.CENTER);
        readerEmpty.getStyleClass().add("hint");
        final StackPane readerPane = new StackPane(readerEmpty, readerContent);
        readerPane.getStyleClass().add("reader");

        final SplitPane split = new SplitPane(sidebar, listPane, readerPane);
        split.setDividerPositions(0.2, 0.52);
        SplitPane.setResizableWithParent(sidebar, false);
        setCenter(split);

        // Alt: durum çubuğu
        progress.setMaxSize(14, 14);
        progress.setVisible(false);
        final HBox statusBar = new HBox(8, progress, status);
        statusBar.setAlignment(Pos.CENTER_LEFT);
        statusBar.getStyleClass().add("status-bar");
        setBottom(statusBar);

        setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.F5)
                reloadMessages();
            else if (event.getCode() == KeyCode.DELETE && messageList.isFocused())
                deleteSelected();
            else if (event.isShortcutDown() && event.getCode() == KeyCode.N)
                openCompose(null, false);
            else if (event.isShortcutDown() && event.getCode() == KeyCode.R && !replyButton.isDisabled())
                openCompose(messageList.getSelectionModel().getSelectedItem(), false);
        });

        showMessage(null);
        loadFolders();
    }

    /** Pencere kapanırken bağlantıyı kapatır. */
    public void shutdown() {
        mailThread.submit(() -> {
            try {
                if (reader != null)
                    reader.close();
            } catch (Exception ignored) {
                // kapanışta hata önemli değil
            }
        });
        mailThread.shutdown();
    }

    /** original null ise yeni mesaj, değilse yanıt ya da iletme penceresi açar. */
    private void openCompose(MessageSummary original, boolean forward) {
        final Consumer<String> onSent = message -> {
            status.getStyleClass().remove("error");
            status.setText(message);
        };
        final ComposeWindow window;
        if (original == null)
            window = ComposeWindow.newMessage(getScene().getWindow(), account, onSent);
        else if (forward)
            window = ComposeWindow.forward(getScene().getWindow(), account, original, shownBody, onSent);
        else
            window = ComposeWindow.reply(getScene().getWindow(), account, original, shownBody, onSent);
        window.show();
    }

    private void confirmLogout(Runnable onLogout) {
        final Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Hesaptan çıkılsın mı? Kayıtlı şifre bu bilgisayardan silinecek.", ButtonType.YES, ButtonType.NO);
        confirm.initOwner(getScene().getWindow());
        if (confirm.showAndWait().orElse(ButtonType.NO) == ButtonType.YES)
            onLogout.run();
    }


    private static Button iconButton(Ikon icon, String tooltip) {
        final Button button = new Button(null, Icons.of(icon));
        button.getStyleClass().addAll(Styles.BUTTON_ICON, Styles.FLAT);
        button.setTooltip(new Tooltip(tooltip));
        return button;
    }

    private void loadFolders() {
        run("Klasörler yükleniyor…", () -> connected().folderNames(), names -> {
            folders.getItems().setAll(names);
            folders.getSelectionModel().select(0); // INBOX
        });
    }

    private void reloadMessages() {
        messages.clear();
        loadPage(0);
    }

    private void loadPage(int skip) {
        final String folder = folders.getSelectionModel().getSelectedItem();
        if (folder == null)
            return;
        run("Mesajlar yükleniyor…", () -> connected().list(folder, skip, PAGE_SIZE), page -> {
            if (!folder.equals(folders.getSelectionModel().getSelectedItem()))
                return; // bu arada başka klasöre geçildi
            messages.addAll(page);
            loadMore.setVisible(page.size() == PAGE_SIZE);
            status.setText(Formats.folderName(folder) + " · " + messages.size() + " mesaj");
        });
    }

    private void showMessage(MessageSummary message) {
        shownBody = null;
        updateButtons(message);
        readerContent.setVisible(message != null);
        readerEmpty.setVisible(message == null);
        if (message == null)
            return;

        shownUid = message.uid();
        subject.setText(message.subject());
        senderAvatar.set(message.from(), message.fromAddress());
        senderName.setText(message.from());
        senderMeta.setText((message.fromAddress() == null ? "" : message.fromAddress() + " · ")
                + Formats.longDate(message.date()));
        body.setText("Yükleniyor…");
        final String folder = folders.getSelectionModel().getSelectedItem();
        run(null, () -> connected().readBody(folder, message.uid()), text -> {
            if (shownUid != message.uid())
                return; // kullanıcı bu arada başka mesaja tıkladı
            shownBody = text;
            body.setText(text);
            body.positionCaret(0);
            updateButtons(message);
            replace(message, true);
        });
    }

    private void deleteSelected() {
        final MessageSummary message = messageList.getSelectionModel().getSelectedItem();
        final String folder = folders.getSelectionModel().getSelectedItem();
        if (message == null || folder == null)
            return;
        run("Siliniyor…", () -> {
            connected().delete(folder, message.uid());
            return null;
        }, ignored -> {
            messages.remove(message);
            status.setText("Mesaj silindi.");
        });
    }

    private void toggleSeen() {
        final MessageSummary message = messageList.getSelectionModel().getSelectedItem();
        final String folder = folders.getSelectionModel().getSelectedItem();
        if (message == null || folder == null)
            return;
        final boolean seen = !message.seen();
        run(null, () -> {
            connected().setSeen(folder, message.uid(), seen);
            return null;
        }, ignored -> replace(message, seen));
    }

    /** Listedeki mesajı yeni "okundu" durumuyla değiştirir (record değiştirilemediği için yenisi oluşturulur). */
    private void replace(MessageSummary message, boolean seen) {
        final int index = messages.indexOf(message);
        if (index < 0 || message.seen() == seen)
            return;
        final MessageSummary updated = new MessageSummary(message.uid(), message.from(), message.fromAddress(),
                message.subject(), message.date(), seen, message.messageId());
        replacing = true;
        messages.set(index, updated);
        messageList.getSelectionModel().select(updated);
        replacing = false;
        updateButtons(updated);
    }

    private void filter(String text) {
        final String query = text == null ? "" : text.strip().toLowerCase(TURKISH);
        visibleMessages.setPredicate(query.isEmpty() ? null : message ->
                message.subject().toLowerCase(TURKISH).contains(query)
                || message.from().toLowerCase(TURKISH).contains(query));
    }

    private void updateButtons(MessageSummary message) {
        // Yanıt ve iletme, gövde yüklenince açılır (alıntı için gövde gerekli)
        replyButton.setDisable(message == null || shownBody == null);
        forwardButton.setDisable(message == null || shownBody == null);
        deleteButton.setDisable(message == null);
        unreadButton.setDisable(message == null);
        final boolean unread = message != null && !message.seen();
        unreadIcon.setIconCode(unread ? Feather.CHECK_CIRCLE : Feather.MAIL); // yeni nesne yok, sadece şekil değişir
        unreadButton.getTooltip().setText(unread ? "Okundu yap" : "Okunmadı yap");
    }

    /** Bağlantı yoksa ya da sunucu kapattıysa yeniden bağlanır. Sadece mailThread içinden çağrılır. */
    private MailReader connected() throws Exception {
        if (reader == null || !reader.isConnected())
            reader = MailReader.connect(account);
        return reader;
    }

    /** İşi arka planda çalıştırır; sonucu arayüz iş parçacığında onSuccess'e verir, hatayı durum çubuğunda gösterir. */
    private <T> void run(String busyText, Callable<T> work, Consumer<T> onSuccess) {
        final Task<T> task = new Task<>() {
            @Override
            protected T call() throws Exception {
                return work.call();
            }
        };
        task.setOnSucceeded(e -> {
            progress.setVisible(false);
            status.getStyleClass().remove("error");
            onSuccess.accept(task.getValue());
        });
        task.setOnFailed(e -> {
            progress.setVisible(false);
            if (!status.getStyleClass().contains("error"))
                status.getStyleClass().add("error");
            status.setText(ErrorMessages.of(task.getException()));
        });
        if (busyText != null) {
            status.getStyleClass().remove("error");
            status.setText(busyText);
        }
        progress.setVisible(true);
        mailThread.submit(task);
    }

    /** Klasör satırı: simge ve okunabilir ad. */
    private static final class FolderCell extends ListCell<String> {
        @Override
        protected void updateItem(String name, boolean empty) {
            super.updateItem(name, empty);
            if (empty || name == null) {
                setText(null);
                setGraphic(null);
                return;
            }
            setText(Formats.folderName(name));
            setGraphic(Icons.of(Icons.forFolder(name)));
        }
    }

    /** Mesaj satırı: avatar, gönderen, tarih, konu; okunmamışlar kalın ve mavi noktalı. */
    private static final class MessageCell extends ListCell<MessageSummary> {
        private final Avatar avatar = new Avatar(34);
        private final Label from = new Label();
        private final Label date = new Label();
        private final Label subjectLabel = new Label();
        private final Circle unreadDot = new Circle(4);
        private final HBox content;

        MessageCell() {
            final Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            date.getStyleClass().add("hint");
            subjectLabel.getStyleClass().add("subject-line");
            unreadDot.getStyleClass().add("unread-dot");
            final VBox text = new VBox(3, new HBox(6, from, spacer, date), subjectLabel);
            HBox.setHgrow(text, Priority.ALWAYS);
            content = new HBox(10, unreadDot, avatar, text);
            content.setAlignment(Pos.CENTER_LEFT);
            content.getStyleClass().add("message-cell");
        }

        @Override
        protected void updateItem(MessageSummary message, boolean empty) {
            super.updateItem(message, empty);
            if (empty || message == null) {
                setGraphic(null);
                return;
            }
            avatar.set(message.from(), message.fromAddress());
            from.setText(message.from());
            subjectLabel.setText(message.subject());
            date.setText(Formats.shortDate(message.date(), ZoneId.systemDefault(), LocalDate.now()));
            unreadDot.setVisible(!message.seen());
            content.getStyleClass().remove("unread");
            if (!message.seen())
                content.getStyleClass().add("unread");
            setGraphic(content);
        }
    }
}