package com.mrcdprm.emailclient.mail;

import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Transport;
import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Locale;

/** SMTP ile e-posta gönderir; yanıt ve iletme için konu ve alıntı metnini hazırlar. */
public final class MailSender {

    /** Tek seferde gönderilebilecek en fazla alıcı: uygulamanın toplu e-posta için kötüye kullanılmasını önler. */
    public static final int MAX_RECIPIENTS = 50;

    private static final DateTimeFormatter QUOTE_DATE =
            DateTimeFormatter.ofPattern("d MMMM yyyy HH:mm", Locale.forLanguageTag("tr")).withZone(ZoneId.systemDefault());

    private MailSender() {
    }

    /** Gönderilecek mesaj. inReplyTo, yanıtlanan mesajın kimliğidir (yeni mesajda null). */
    public record Draft(String to, String cc, String subject, String body, String inReplyTo) {
    }

    public static void send(MailAccount account, Draft draft) throws MessagingException {
        final InternetAddress[] to = parseRecipients(draft.to(), true);
        final InternetAddress[] cc = parseRecipients(draft.cc(), false);
        if (to.length + cc.length > MAX_RECIPIENTS)
            throw new AddressException("En fazla " + MAX_RECIPIENTS + " alıcıya gönderebilirsin.");

        final MimeMessage message = new MimeMessage(MailSessions.smtpSession(account));
        message.setFrom(new InternetAddress(account.email()));
        message.setRecipients(Message.RecipientType.TO, to);
        if (cc.length > 0)
            message.setRecipients(Message.RecipientType.CC, cc);
        message.setSubject(draft.subject() == null ? "" : draft.subject().strip(), "UTF-8");
        message.setText(draft.body() == null ? "" : draft.body(), "UTF-8");
        message.setSentDate(new Date());
        if (draft.inReplyTo() != null && !draft.inReplyTo().isBlank()) {
            // Kimlik dışarıdan gelen bir e-postadan okunur: satır sonu başka başlık eklemesin diye temizlenir
            final String inReplyTo = draft.inReplyTo().replaceAll("[\\r\\n]", "").strip();
            message.setHeader("In-Reply-To", inReplyTo);
            message.setHeader("References", inReplyTo);
        }
        Transport.send(message, account.email(), account.password());
    }

    /** SMTP sunucusuna giriş yapıp çıkar; hesap kurulumunda bilgilerin doğruluğunu kontrol etmek için. */
    public static void verify(MailAccount account) throws MessagingException {
        try (Transport transport = MailSessions.smtpSession(account).getTransport("smtp")) {
            transport.connect(account.email(), account.password());
        }
    }


    /**
     * Virgül ya da noktalı virgülle ayrılmış adresleri çözümler. Geçersiz bir adres varsa hangisi olduğunu
     * söyleyen bir hata fırlatır.
     */
    public static InternetAddress[] parseRecipients(String text, boolean required) throws AddressException {
        final String normalized = text == null ? "" : text.replace(';', ',').strip();
        if (normalized.isEmpty()) {
            if (required)
                throw new AddressException("En az bir alıcı gir.");
            return new InternetAddress[0];
        }
        final InternetAddress[] addresses = InternetAddress.parse(normalized, true);
        for (InternetAddress address : addresses) {
            if (!MailAccount.isValidAddress(address.getAddress()))
                throw new AddressException("Geçersiz adres: " + address.getAddress());
        }
        return addresses;
    }

    public static String replySubject(String subject) {
        return prefixed("Re: ", subject, "re:");
    }

    public static String forwardSubject(String subject) {
        return prefixed("Fwd: ", subject, "fwd:", "fw:");
    }

    /** Yanıt / iletme gövdesi: altta tarihli bir başlık ve "> " ile başlayan alıntı satırları. */
    public static String quote(MessageSummary original, String body) {
        final String date = original.date() == null ? "" : QUOTE_DATE.format(original.date()) + " tarihinde ";
        final StringBuilder quoted = new StringBuilder("\n\n").append(date).append(original.from()).append(" yazdı:\n");
        for (String line : (body == null ? "" : body).split("\\R", -1))
            quoted.append("> ").append(line).append('\n');
        return quoted.toString();
    }

    private static String prefixed(String prefix, String subject, String... existing) {
        final String clean = subject == null ? "" : subject.strip();
        final String lower = clean.toLowerCase(Locale.ROOT);
        for (String p : existing)
            if (lower.startsWith(p))
                return clean;
        return prefix + clean;
    }
}
