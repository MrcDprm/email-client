package com.mrcdprm.emailclient.mail;

import jakarta.mail.Address;
import jakarta.mail.FetchProfile;
import jakarta.mail.Flags;
import jakarta.mail.Folder;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Store;
import jakarta.mail.UIDFolder;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeUtility;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.eclipse.angus.mail.imap.IMAPFolder;

/**
 * IMAP ile gelen kutusunu okur. Bir bağlantı açık tutulur; her işlem ağa gittiği için
 * arayüzden değil, arka plan iş parçacığından çağrılmalıdır.
 */
public final class MailReader implements AutoCloseable {

    private final Store store;

    private MailReader(Store store) {
        this.store = store;
    }

    /** Sunucuya bağlanır; kullanıcı adı / şifre yanlışsa AuthenticationFailedException fırlatır. */
    public static MailReader connect(MailAccount account) throws MessagingException {
        final Store store = MailSessions.imapSession(account).getStore(MailSessions.imapProtocol(account));
        store.connect(account.imapHost(), account.imapPort(), account.email(), account.password());
        return new MailReader(store);
    }

    /** Mesaj içerebilen bütün klasörlerin tam adları (INBOX her zaman ilk sırada). */
    public List<String> folderNames() throws MessagingException {
        final List<String> names = new ArrayList<>();
        for (Folder folder : store.getDefaultFolder().list("*")) {
            if ((folder.getType() & Folder.HOLDS_MESSAGES) != 0)
                names.add(folder.getFullName());
        }
        names.sort((a, b) -> a.equalsIgnoreCase("INBOX") ? -1 : b.equalsIgnoreCase("INBOX") ? 1 : a.compareToIgnoreCase(b));
        return names;
    }

    /**
     * En yeniden eskiye mesaj özetleri. skip kadar yeni mesajı atlar ("daha fazla yükle" için),
     * en fazla limit kadar döndürür.
     */
    public List<MessageSummary> list(String folderName, int skip, int limit) throws MessagingException {
        final Folder folder = open(folderName);
        final int total = folder.getMessageCount();
        final int end = total - Math.max(0, skip);
        final int start = Math.max(1, end - limit + 1);
        final List<MessageSummary> result = new ArrayList<>();
        if (end < 1)
            return result;

        final Message[] messages = folder.getMessages(start, end);
        final FetchProfile profile = new FetchProfile(); // başlıkları tek istekte toplu indir
        profile.add(FetchProfile.Item.ENVELOPE);
        profile.add(FetchProfile.Item.FLAGS);
        profile.add(UIDFolder.FetchProfileItem.UID);
        profile.add("Message-ID");
        folder.fetch(messages, profile);

        final UIDFolder uids = (UIDFolder) folder;
        for (int i = messages.length - 1; i >= 0; i--) {
            final Message message = messages[i];
            result.add(new MessageSummary(
                    uids.getUID(message),
                    sender(message),
                    replyAddress(message),
                    message.getSubject() == null || message.getSubject().isBlank() ? "(Konu yok)" : message.getSubject(),
                    message.getSentDate() == null ? null : message.getSentDate().toInstant(),
                    message.isSet(Flags.Flag.SEEN),
                    message instanceof MimeMessage mime ? mime.getMessageID() : null));
        }
        return result;
    }

    /** Mesajın düz metin gövdesi. Okunan mesaj sunucuda "okundu" olarak işaretlenir. */
    public String readBody(String folderName, long uid) throws MessagingException, IOException {
        final Message message = find(folderName, uid);
        final String text = MessageText.of(message);
        message.setFlag(Flags.Flag.SEEN, true);
        return text;
    }

    public void setSeen(String folderName, long uid, boolean seen) throws MessagingException {
        find(folderName, uid).setFlag(Flags.Flag.SEEN, seen);
    }

    /** Mesajı çöp kutusuna taşır; zaten çöpteyse ya da çöp klasörü yoksa kalıcı olarak siler. */
    public void delete(String folderName, long uid) throws MessagingException {
        final Folder folder = open(folderName);
        final Message message = find(folderName, uid);
        final String trash = trashFolderName();
        if (trash != null && !trash.equals(folder.getFullName()))
            folder.copyMessages(new Message[] {message}, store.getFolder(trash));
        message.setFlag(Flags.Flag.DELETED, true);
        folder.expunge();
    }

    /** Bağlantı hâlâ açık mı (sunucu zaman aşımıyla kapatmış olabilir). */
    public boolean isConnected() {
        return store.isConnected();
    }

    @Override
    public void close() throws MessagingException {
        store.close(); // açık klasörler de kapanır
    }

    private Folder open(String folderName) throws MessagingException {
        final Folder folder = store.getFolder(folderName);
        if (!folder.isOpen())
            folder.open(Folder.READ_WRITE);
        return folder;
    }

    private Message find(String folderName, long uid) throws MessagingException {
        final Message message = ((UIDFolder) open(folderName)).getMessageByUID(uid);
        if (message == null)
            throw new MessagingException("Mesaj bulunamadı; silinmiş ya da taşınmış olabilir.");
        return message;
    }

    /** Sunucunun "\Trash" olarak işaretlediği klasör; yoksa adı "Trash" ya da "Çöp" içeren klasör. */
    private String trashFolderName() throws MessagingException {
        String byName = null;
        for (Folder folder : store.getDefaultFolder().list("*")) {
            if (folder instanceof IMAPFolder imap) {
                for (String attribute : imap.getAttributes())
                    if (attribute.equalsIgnoreCase("\\Trash"))
                        return folder.getFullName();
            }
            final String name = folder.getName().toLowerCase(Locale.ROOT);
            if (byName == null && (name.contains("trash") || name.contains("çöp") || name.contains("deleted")))
                byName = folder.getFullName();
        }
        return byName;
    }

    private static String sender(Message message) throws MessagingException {
        if (message.getFrom() == null || message.getFrom().length == 0)
            return "(Gönderen yok)";
        if (message.getFrom()[0] instanceof InternetAddress address) {
            final String personal = address.getPersonal();
            return personal == null || personal.isBlank() ? address.getAddress() : personal;
        }
        return decode(message.getFrom()[0].toString());
    }

    /** Yanıtın gideceği adres: varsa Reply-To, yoksa gönderen. */
    private static String replyAddress(Message message) throws MessagingException {
        final Address[] replyTo = message.getReplyTo();
        if (replyTo != null && replyTo.length > 0 && replyTo[0] instanceof InternetAddress address)
            return address.getAddress();
        return null;
    }


    private static String decode(String text) {
        try {
            return MimeUtility.decodeText(text);
        } catch (UnsupportedEncodingException e) {
            return text;
        }
    }
}
