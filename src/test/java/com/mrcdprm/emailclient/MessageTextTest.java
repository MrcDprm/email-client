package com.mrcdprm.emailclient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mrcdprm.emailclient.mail.MessageText;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import java.util.Properties;
import org.junit.jupiter.api.Test;

class MessageTextTest {

    private final Session session = Session.getInstance(new Properties());

    @Test
    void htmlBecomesReadableText() {
        final String text = MessageText.htmlToText(
                "<p>Merhaba <b>Miraç</b>,</p><p>Toplantı &amp; sunum yarın.<br>Görüşürüz</p>"
                + "<script>alert('x')</script><style>p{color:red}</style>");
        assertTrue(text.contains("Merhaba Miraç,"));
        assertTrue(text.contains("Toplantı & sunum yarın.\nGörüşürüz"));
        assertFalse(text.contains("<"));
        assertFalse(text.contains("alert"));
        assertFalse(text.contains("color"));
    }

    @Test
    void prefersPlainTextPart() throws Exception {
        final MimeMultipart alternative = new MimeMultipart("alternative");
        final MimeBodyPart plain = new MimeBodyPart();
        plain.setText("Düz metin sürümü", "UTF-8");
        final MimeBodyPart html = new MimeBodyPart();
        html.setContent("<p>HTML sürümü</p>", "text/html; charset=UTF-8");
        alternative.addBodyPart(plain);
        alternative.addBodyPart(html);

        final MimeMessage message = new MimeMessage(session);
        message.setContent(alternative);
        message.saveChanges();
        assertEquals("Düz metin sürümü", MessageText.of(message));
    }

    @Test
    void usesHtmlWhenNoPlainTextAndSkipsAttachments() throws Exception {
        final MimeMultipart mixed = new MimeMultipart("mixed");
        final MimeBodyPart attachment = new MimeBodyPart();
        attachment.setText("ekteki metin, gövde değil", "UTF-8");
        attachment.setDisposition(MimeBodyPart.ATTACHMENT);
        attachment.setFileName("not.txt");
        final MimeBodyPart html = new MimeBodyPart();
        html.setContent("<div>Sadece <i>HTML</i> gövde</div>", "text/html; charset=UTF-8");
        mixed.addBodyPart(attachment);
        mixed.addBodyPart(html);

        final MimeMessage message = new MimeMessage(session);
        message.setContent(mixed);
        message.saveChanges();
        assertEquals("Sadece HTML gövde", MessageText.of(message));
    }

    @Test
    void emptyMessageShowsNotice() throws Exception {
        final MimeMessage message = new MimeMessage(session);
        message.setText("   ", "UTF-8");
        message.saveChanges();
        assertEquals("(Bu mesajda gösterilebilecek metin yok.)", MessageText.of(message));
    }
}
