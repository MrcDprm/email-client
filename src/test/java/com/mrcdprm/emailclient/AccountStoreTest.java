package com.mrcdprm.emailclient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mrcdprm.emailclient.mail.MailAccount;
import com.mrcdprm.emailclient.mail.ServerPreset;
import com.mrcdprm.emailclient.security.SecretStore;
import com.mrcdprm.emailclient.settings.AccountStore;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class AccountStoreTest {

    @TempDir
    Path directory;

    private final MailAccount account =
            MailAccount.fromPreset(ServerPreset.GMAIL, "mirac@gmail.com", "abcd efgh ijkl mnop");

    @Test
    void savesAndLoadsAccount() throws Exception {
        final AccountStore store = new AccountStore(directory);
        assertTrue(store.load().isEmpty());

        store.save(account);
        assertEquals(account, store.load().orElseThrow());
    }

    @Test
    void passwordIsNotStoredAsPlainText() throws Exception {
        new AccountStore(directory).save(account);
        final String content = Files.readString(directory.resolve("account.properties"), StandardCharsets.UTF_8);
        assertTrue(content.contains("mirac@gmail.com"));
        assertFalse(content.contains("abcd efgh"));
    }

    @Test
    void corruptFileIsIgnored() throws Exception {
        Files.writeString(directory.resolve("account.properties"), "email=mirac@gmail.com\npassword=bozuk!!\nimapPort=abc");
        assertTrue(new AccountStore(directory).load().isEmpty());
    }

    @Test
    void deleteRemovesAccount() throws Exception {
        final AccountStore store = new AccountStore(directory);
        store.save(account);
        store.delete();
        assertTrue(store.load().isEmpty());
    }

    @Test
    void readsAccountFromEnvironment() {
        final MailAccount fromEnv = AccountStore.fromEnvironment(Map.of(
                AccountStore.ENV_EMAIL, "mirac@gmail.com",
                AccountStore.ENV_PASSWORD, "uygulama-sifresi")).orElseThrow();
        assertEquals("imap.gmail.com", fromEnv.imapHost());

        assertTrue(AccountStore.fromEnvironment(Map.of()).isEmpty());
        assertTrue(AccountStore.fromEnvironment(Map.of(
                AccountStore.ENV_EMAIL, "mirac@sirket.com", AccountStore.ENV_PASSWORD, "x")).isEmpty());
    }

    @Test
    void secretStoreRoundTrip() {
        final byte[] encrypted = SecretStore.encrypt("gizli şifre");
        assertEquals("gizli şifre", SecretStore.decrypt(encrypted).orElseThrow());
        assertTrue(SecretStore.decrypt(new byte[] {1, 2, 3}).isEmpty());
    }
}
