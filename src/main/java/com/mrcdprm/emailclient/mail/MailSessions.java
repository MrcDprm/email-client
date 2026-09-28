package com.mrcdprm.emailclient.mail;

import jakarta.mail.Session;
import java.util.Properties;

/** IMAP ve SMTP bağlantıları için Jakarta Mail oturum ayarları. */
final class MailSessions {

    private static final String TIMEOUT_MS = "20000";

    private MailSessions() {
    }

    /** IMAP için protokol adı: şifreli bağlantıda "imaps", testteki şifresiz sunucuda "imap". */
    static String imapProtocol(MailAccount account) {
        return account.imapSecurity() == Security.SSL ? "imaps" : "imap";
    }

    static Session imapSession(MailAccount account) {
        final String protocol = imapProtocol(account);
        final Properties props = new Properties();
        props.put("mail." + protocol + ".connectiontimeout", TIMEOUT_MS);
        props.put("mail." + protocol + ".timeout", TIMEOUT_MS);
        if (account.imapSecurity() == Security.SSL)
            props.put("mail.imaps.ssl.checkserveridentity", "true"); // sahte sunucu sertifikasını reddet
        return Session.getInstance(props);
    }

    static Session smtpSession(MailAccount account) {
        final Properties props = new Properties();
        props.put("mail.smtp.host", account.smtpHost());
        props.put("mail.smtp.port", String.valueOf(account.smtpPort()));
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.connectiontimeout", TIMEOUT_MS);
        props.put("mail.smtp.timeout", TIMEOUT_MS);
        props.put("mail.smtp.writetimeout", TIMEOUT_MS);
        switch (account.smtpSecurity()) {
            case SSL -> {
                props.put("mail.smtp.ssl.enable", "true");
                props.put("mail.smtp.ssl.checkserveridentity", "true");
            }
            case STARTTLS -> {
                // Şifreliye geçilemezse şifre düz metin gönderilmesin diye "required"
                props.put("mail.smtp.starttls.enable", "true");
                props.put("mail.smtp.starttls.required", "true");
                props.put("mail.smtp.ssl.checkserveridentity", "true");
            }
            case NONE -> {
                // sadece testlerdeki yerel sunucu
            }
        }
        return Session.getInstance(props);
    }
}
