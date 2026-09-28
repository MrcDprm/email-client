package com.mrcdprm.emailclient.mail;

import java.util.Locale;

/** Yaygın e-posta sağlayıcılarının sunucu ayarları. */
public enum ServerPreset {
    GMAIL("Gmail", "imap.gmail.com", 993, "smtp.gmail.com", 465, Security.SSL),
    YANDEX("Yandex", "imap.yandex.com", 993, "smtp.yandex.com", 465, Security.SSL),
    ICLOUD("iCloud", "imap.mail.me.com", 993, "smtp.mail.me.com", 587, Security.STARTTLS),
    CUSTOM("Diğer (elle gir)", "", 993, "", 587, Security.STARTTLS);

    private final String displayName;
    private final String imapHost;
    private final int imapPort;
    private final String smtpHost;
    private final int smtpPort;
    private final Security smtpSecurity;

    ServerPreset(String displayName, String imapHost, int imapPort,
                 String smtpHost, int smtpPort, Security smtpSecurity) {
        this.displayName = displayName;
        this.imapHost = imapHost;
        this.imapPort = imapPort;
        this.smtpHost = smtpHost;
        this.smtpPort = smtpPort;
        this.smtpSecurity = smtpSecurity;
    }

    public String imapHost() { return imapHost; }
    public int imapPort() { return imapPort; }
    public String smtpHost() { return smtpHost; }
    public int smtpPort() { return smtpPort; }
    public Security smtpSecurity() { return smtpSecurity; }

    /** E-posta adresinin alan adından sağlayıcıyı tahmin eder; bilinmiyorsa CUSTOM döner. */
    public static ServerPreset detect(String email) {
        final int at = email == null ? -1 : email.lastIndexOf('@');
        if (at < 0)
            return CUSTOM;
        return switch (email.substring(at + 1).toLowerCase(Locale.ROOT)) {
            case "gmail.com", "googlemail.com" -> GMAIL;
            case "yandex.com", "yandex.com.tr", "yandex.ru" -> YANDEX;
            case "icloud.com", "me.com", "mac.com" -> ICLOUD;
            default -> CUSTOM;
        };
    }

    /** Açılır listede görünecek ad. */
    @Override
    public String toString() {
        return displayName;
    }
}
