package com.mrcdprm.emailclient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mrcdprm.emailclient.mail.MailAccount;
import com.mrcdprm.emailclient.mail.Security;
import com.mrcdprm.emailclient.mail.ServerPreset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class MailAccountTest {

    @Test
    void detectsProviderFromAddress() {
        assertEquals(ServerPreset.GMAIL, ServerPreset.detect("ali@gmail.com"));
        assertEquals(ServerPreset.GMAIL, ServerPreset.detect("Ali@GMAIL.COM"));
        assertEquals(ServerPreset.YANDEX, ServerPreset.detect("ali@yandex.com.tr"));
        assertEquals(ServerPreset.ICLOUD, ServerPreset.detect("ali@icloud.com"));
        assertEquals(ServerPreset.CUSTOM, ServerPreset.detect("ali@sirket.com"));
        assertEquals(ServerPreset.CUSTOM, ServerPreset.detect("adres-degil"));
        assertEquals(ServerPreset.CUSTOM, ServerPreset.detect(null));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "ali", "ali@", "@gmail.com", "ali veli@gmail.com", "a@b.com, c@d.com",
                            "Ali <ali@gmail.com>"})
    void rejectsInvalidAddresses(String address) {
        assertFalse(MailAccount.isValidAddress(address));
    }

    @Test
    void acceptsValidAddress() {
        assertTrue(MailAccount.isValidAddress("mirac.deprem@gmail.com"));
        assertTrue(MailAccount.isValidAddress(" mirac@gmail.com "));
    }

    @Test
    void presetAccountUsesProviderServers() {
        final MailAccount account = MailAccount.fromPreset(ServerPreset.GMAIL, " mirac@gmail.com ", "uygulama-sifresi");
        assertEquals("mirac@gmail.com", account.email());
        assertEquals("imap.gmail.com", account.imapHost());
        assertEquals(465, account.smtpPort());
        assertEquals(Security.SSL, account.smtpSecurity());
    }

    @Test
    void rejectsMissingFields() {
        assertThrows(IllegalArgumentException.class,
                () -> MailAccount.fromPreset(ServerPreset.GMAIL, "mirac@gmail.com", " "));
        assertThrows(IllegalArgumentException.class,
                () -> MailAccount.fromPreset(ServerPreset.CUSTOM, "mirac@sirket.com", "sifre"));
        assertThrows(IllegalArgumentException.class,
                () -> new MailAccount("mirac@sirket.com", "sifre", "imap.sirket.com", 0, Security.SSL,
                        "smtp.sirket.com", 587, Security.STARTTLS));
    }

    @Test
    void toStringHidesPassword() {
        final MailAccount account = MailAccount.fromPreset(ServerPreset.GMAIL, "mirac@gmail.com", "gizli123");
        assertFalse(account.toString().contains("gizli123"));
    }
}
