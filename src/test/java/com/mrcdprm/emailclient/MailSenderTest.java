package com.mrcdprm.emailclient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.icegreen.greenmail.junit5.GreenMailExtension;
import com.icegreen.greenmail.util.ServerSetupTest;
import com.mrcdprm.emailclient.mail.MailAccount;
import com.mrcdprm.emailclient.mail.MailReader;
import com.mrcdprm.emailclient.mail.MailSender;
import com.mrcdprm.emailclient.mail.MailSender.Draft;
import com.mrcdprm.emailclient.mail.MessageSummary;
import com.mrcdprm.emailclient.mail.Security;
import jakarta.mail.Message;
import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.MimeMessage;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class MailSenderTest {

    @RegisterExtension
    static GreenMailExtension greenMail = new GreenMailExtension(ServerSetupTest.SMTP_IMAP);

    private static final String EMAIL = "mirac@test.local";
    private static final String PASSWORD = "test-sifre";

    @BeforeEach
    void createUsers() {
        greenMail.setUser(EMAIL, EMAIL, PASSWORD);
        greenMail.setUser("ayse@test.local", "ayse@test.local", "sifre2");
    }

    private static MailAccount account(String email, String password) {
        return new MailAccount(email, password,
                "127.0.0.1", ServerSetupTest.IMAP.getPort(), Security.NONE,
                "127.0.0.1", ServerSetupTest.SMTP.getPort(), Security.NONE);
    }

    @Test
    void sendsWithTurkishCharactersAndCc() throws Exception {
        MailSender.send(account(EMAIL, PASSWORD),
                new Draft("ayse@test.local", "ali@test.local; veli@test.local", "Toplantı çarşamba", "Görüşürüz 👋", null));

        final MimeMessage[] received = greenMail.getReceivedMessages();
        assertEquals(3, received.length); // to + 2 cc
        final MimeMessage message = received[0];
        assertEquals("Toplantı çarşamba", message.getSubject());
        assertEquals("Görüşürüz 👋", ((String) message.getContent()).strip());
        assertEquals(EMAIL, message.getFrom()[0].toString());
        assertEquals(2, message.getRecipients(Message.RecipientType.CC).length);
    }

    @Test
    void replyKeepsThreadAndAddressIsReadBack() throws Exception {
        MailSender.send(account("ayse@test.local", "sifre2"), new Draft(EMAIL, "", "Soru", "Nasılsın?", null));

        try (MailReader reader = MailReader.connect(account(EMAIL, PASSWORD))) {
            final MessageSummary original = reader.list("INBOX", 0, 10).get(0);
            assertEquals("ayse@test.local", original.fromAddress());
            assertTrue(original.messageId() != null && original.messageId().contains("@"));

            MailSender.send(account(EMAIL, PASSWORD), new Draft(original.fromAddress(), "",
                    MailSender.replySubject(original.subject()), "İyiyim", original.messageId()));
        }
        final MimeMessage reply = greenMail.getReceivedMessages()[1];
        assertEquals("Re: Soru", reply.getSubject());
        assertEquals(greenMail.getReceivedMessages()[0].getMessageID(), reply.getHeader("In-Reply-To")[0]);
    }

    @Test
    void rejectsBadRecipients() {
        final MailAccount account = account(EMAIL, PASSWORD);
        assertThrows(AddressException.class, () -> MailSender.send(account, new Draft(" ", "", "k", "g", null)));
        final AddressException bad = assertThrows(AddressException.class,
                () -> MailSender.send(account, new Draft("ayse@test.local, bozuk-adres", "", "k", "g", null)));
        assertTrue(bad.getMessage().contains("bozuk-adres"));

        final String many = String.join(",", java.util.Collections.nCopies(51, "a@test.local"));
        assertThrows(AddressException.class, () -> MailSender.send(account, new Draft(many, "", "k", "g", null)));
        assertEquals(0, greenMail.getReceivedMessages().length);
    }

    @Test
    void subjectsAreNotPrefixedTwice() {
        assertEquals("Re: Merhaba", MailSender.replySubject("Merhaba"));
        assertEquals("RE: Merhaba", MailSender.replySubject("RE: Merhaba"));
        assertEquals("Fwd: Rapor", MailSender.forwardSubject(" Rapor "));
        assertEquals("Fw: Rapor", MailSender.forwardSubject("Fw: Rapor"));
    }

    @Test
    void quotesOriginalLines() {
        final MessageSummary original = new MessageSummary(1, "Ayşe", "ayse@test.local", "Soru",
                Instant.parse("2026-09-28T09:30:00Z"), true, null);
        final String quote = MailSender.quote(original, "Satır 1\nSatır 2");
        assertTrue(quote.contains("Ayşe yazdı:"));
        assertTrue(quote.contains("> Satır 1\n> Satır 2"));
        assertTrue(quote.contains("2026"));
    }
}