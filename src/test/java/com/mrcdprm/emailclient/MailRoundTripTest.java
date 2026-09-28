package com.mrcdprm.emailclient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.icegreen.greenmail.junit5.GreenMailExtension;
import com.icegreen.greenmail.user.GreenMailUser;
import com.icegreen.greenmail.util.GreenMailUtil;
import com.icegreen.greenmail.util.ServerSetupTest;
import com.mrcdprm.emailclient.mail.MailAccount;
import com.mrcdprm.emailclient.mail.MailReader;
import com.mrcdprm.emailclient.mail.MessageSummary;
import com.mrcdprm.emailclient.mail.Security;
import jakarta.mail.AuthenticationFailedException;
import jakarta.mail.internet.MimeMessage;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

/** Bellekte çalışan GreenMail sunucusuyla gerçek IMAP akışı; gerçek hesaplara dokunulmaz. */
class MailRoundTripTest {

    @RegisterExtension
    static GreenMailExtension greenMail = new GreenMailExtension(ServerSetupTest.SMTP_IMAP);

    private static final String EMAIL = "mirac@test.local";
    private static final String PASSWORD = "test-sifre";

    private GreenMailUser user;

    @BeforeEach
    void createUser() {
        user = greenMail.setUser(EMAIL, EMAIL, PASSWORD);
    }

    private static MailAccount account(String password) {
        return new MailAccount(EMAIL, password,
                "127.0.0.1", ServerSetupTest.IMAP.getPort(), Security.NONE,
                "127.0.0.1", ServerSetupTest.SMTP.getPort(), Security.NONE);
    }

    private void deliver(int count) {
        for (int i = 1; i <= count; i++) {
            final MimeMessage message = GreenMailUtil.createTextEmail(
                    EMAIL, "ayse@test.local", "Mesaj " + i, "Gövde " + i, ServerSetupTest.SMTP);
            user.deliver(message);
        }
    }

    @Test
    void wrongPasswordIsRejected() {
        assertThrows(AuthenticationFailedException.class, () -> MailReader.connect(account("yanlis")));
    }

    @Test
    void listsNewestFirstWithPaging() throws Exception {
        deliver(25);
        try (MailReader reader = MailReader.connect(account(PASSWORD))) {
            assertEquals("INBOX", reader.folderNames().get(0));

            final List<MessageSummary> first = reader.list("INBOX", 0, 20);
            assertEquals(20, first.size());
            assertEquals("Mesaj 25", first.get(0).subject());
            assertEquals("Mesaj 6", first.get(19).subject());

            final List<MessageSummary> more = reader.list("INBOX", 20, 20);
            assertEquals(5, more.size());
            assertEquals("Mesaj 1", more.get(4).subject());
            assertTrue(reader.list("INBOX", 25, 20).isEmpty());
        }
    }

    @Test
    void readingMarksAsSeen() throws Exception {
        deliver(1);
        try (MailReader reader = MailReader.connect(account(PASSWORD))) {
            final MessageSummary summary = reader.list("INBOX", 0, 10).get(0);
            assertFalse(summary.seen());
            assertEquals("Gövde 1", reader.readBody("INBOX", summary.uid()));
            assertTrue(reader.list("INBOX", 0, 10).get(0).seen());

            reader.setSeen("INBOX", summary.uid(), false);
            assertFalse(reader.list("INBOX", 0, 10).get(0).seen());
        }
    }

    @Test
    void deleteMovesToTrash() throws Exception {
        greenMail.getManagers().getImapHostManager().createMailbox(user, "Trash");
        deliver(2);
        try (MailReader reader = MailReader.connect(account(PASSWORD))) {
            final MessageSummary newest = reader.list("INBOX", 0, 10).get(0);
            reader.delete("INBOX", newest.uid());

            final List<MessageSummary> inbox = reader.list("INBOX", 0, 10);
            assertEquals(1, inbox.size());
            assertEquals("Mesaj 1", inbox.get(0).subject());
            assertEquals("Mesaj 2", reader.list("Trash", 0, 10).get(0).subject());
        }
    }
}
