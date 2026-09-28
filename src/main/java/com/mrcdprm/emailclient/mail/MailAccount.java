package com.mrcdprm.emailclient.mail;

import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;
import java.util.Objects;

/**
 * Bir e-posta hesabının bağlantı bilgileri. Oluşturulurken doğrulanır; geçersiz bilgiyle nesne oluşmaz.
 */
public record MailAccount(
        String email,
        String password,
        String imapHost,
        int imapPort,
        Security imapSecurity,
        String smtpHost,
        int smtpPort,
        Security smtpSecurity) {

    public MailAccount {
        Objects.requireNonNull(imapSecurity);
        Objects.requireNonNull(smtpSecurity);
        if (!isValidAddress(email))
            throw new IllegalArgumentException("Geçerli bir e-posta adresi gir.");
        if (password == null || password.isBlank())
            throw new IllegalArgumentException("Şifre boş olamaz.");
        if (imapHost == null || imapHost.isBlank() || smtpHost == null || smtpHost.isBlank())
            throw new IllegalArgumentException("Sunucu adresleri boş olamaz.");
        if (!isValidPort(imapPort) || !isValidPort(smtpPort))
            throw new IllegalArgumentException("Port 1 ile 65535 arasında olmalı.");
        email = email.trim();
        imapHost = imapHost.trim();
        smtpHost = smtpHost.trim();
    }

    /** Hazır sağlayıcı ayarlarıyla hesap oluşturur (IMAP her zaman SSL). */
    public static MailAccount fromPreset(ServerPreset preset, String email, String password) {
        return new MailAccount(email, password, preset.imapHost(), preset.imapPort(), Security.SSL,
                preset.smtpHost(), preset.smtpPort(), preset.smtpSecurity());
    }

    /** "ad@alan.com" biçiminde tek ve geçerli bir adres mi. */
    public static boolean isValidAddress(String address) {
        if (address == null || address.isBlank())
            return false;
        try {
            final InternetAddress parsed = new InternetAddress(address.trim(), true);
            return parsed.getAddress().contains("@") && parsed.getPersonal() == null;
        } catch (AddressException e) {
            return false;
        }
    }

    private static boolean isValidPort(int port) {
        return port >= 1 && port <= 65535;
    }

    /** Şifre günlüklere ya da hata mesajlarına sızmasın diye gizlenir. */
    @Override
    public String toString() {
        return "MailAccount[" + email + ", imap=" + imapHost + ":" + imapPort
                + ", smtp=" + smtpHost + ":" + smtpPort + ", password=***]";
    }
}
