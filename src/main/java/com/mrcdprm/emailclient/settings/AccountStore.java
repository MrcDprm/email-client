package com.mrcdprm.emailclient.settings;

import com.mrcdprm.emailclient.mail.MailAccount;
import com.mrcdprm.emailclient.mail.Security;
import com.mrcdprm.emailclient.mail.ServerPreset;
import com.mrcdprm.emailclient.security.SecretStore;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;

/**
 * Hesap bilgisini kullanıcı klasörüne kaydeder (%APPDATA%\EmailClient\account.properties).
 * Şifre dosyaya düz metin değil, DPAPI ile şifrelenmiş olarak yazılır.
 */
public final class AccountStore {

    /** Geliştirme ve testlerde hesap bilgisini dosya yerine ortam değişkenlerinden almak için. */
    public static final String ENV_EMAIL = "EMAIL_ADDRESS";
    public static final String ENV_PASSWORD = "EMAIL_APP_PASSWORD";

    private final Path file;

    public AccountStore(Path directory) {
        this.file = directory.resolve("account.properties");
    }

    /** Uygulamanın veri klasörü: Windows'ta %APPDATA%\EmailClient. */
    public static Path defaultDirectory() {
        final String appData = System.getenv("APPDATA");
        final Path base = appData != null ? Path.of(appData) : Path.of(System.getProperty("user.home"));
        return base.resolve("EmailClient");
    }

    /**
     * Ortam değişkenlerinde Gmail / Yandex / iCloud hesabı tanımlıysa onu döndürür.
     * Değişkenler yoksa ya da sağlayıcı tanınmıyorsa boş döner.
     */
    public static Optional<MailAccount> fromEnvironment(Map<String, String> env) {
        final String email = env.get(ENV_EMAIL);
        final String password = env.get(ENV_PASSWORD);
        final ServerPreset preset = ServerPreset.detect(email);
        if (email == null || password == null || preset == ServerPreset.CUSTOM)
            return Optional.empty();
        try {
            return Optional.of(MailAccount.fromPreset(preset, email, password));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    public Optional<MailAccount> load() {
        if (!Files.isRegularFile(file))
            return Optional.empty();
        final Properties props = new Properties();
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            props.load(reader);
            final Optional<String> password = SecretStore.decrypt(
                    Base64.getDecoder().decode(props.getProperty("password", "")));
            if (password.isEmpty())
                return Optional.empty();
            return Optional.of(new MailAccount(
                    props.getProperty("email"),
                    password.get(),
                    props.getProperty("imapHost"),
                    Integer.parseInt(props.getProperty("imapPort")),
                    Security.valueOf(props.getProperty("imapSecurity")),
                    props.getProperty("smtpHost"),
                    Integer.parseInt(props.getProperty("smtpPort")),
                    Security.valueOf(props.getProperty("smtpSecurity"))));
        } catch (IOException | RuntimeException e) {
            // Bozuk ya da elle değiştirilmiş dosya: uygulama çökmez, hesap kurulumu tekrar istenir
            return Optional.empty();
        }
    }

    public void save(MailAccount account) throws IOException {
        final Properties props = new Properties();
        props.setProperty("email", account.email());
        props.setProperty("password", Base64.getEncoder().encodeToString(SecretStore.encrypt(account.password())));
        props.setProperty("imapHost", account.imapHost());
        props.setProperty("imapPort", String.valueOf(account.imapPort()));
        props.setProperty("imapSecurity", account.imapSecurity().name());
        props.setProperty("smtpHost", account.smtpHost());
        props.setProperty("smtpPort", String.valueOf(account.smtpPort()));
        props.setProperty("smtpSecurity", account.smtpSecurity().name());

        // Önce geçici dosyaya yaz, sonra tek adımda yer değiştir: yazarken çökse bile eski dosya bozulmaz
        Files.createDirectories(file.getParent());
        final Path temp = file.resolveSibling(file.getFileName() + ".tmp");
        try (Writer writer = Files.newBufferedWriter(temp, StandardCharsets.UTF_8)) {
            props.store(writer, "Email Client hesabı (şifre DPAPI ile şifreli)");
        }
        Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
    }

    /** Hesaptan çıkış: kayıtlı bilgiler silinir. */
    public void delete() throws IOException {
        Files.deleteIfExists(file);
    }
}
