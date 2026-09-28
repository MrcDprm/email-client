package com.mrcdprm.emailclient;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.mrcdprm.emailclient.ui.Formats;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;

class FormatsTest {

    private static final ZoneId ISTANBUL = ZoneId.of("Europe/Istanbul");
    private static final LocalDate TODAY = LocalDate.of(2026, 9, 28);

    @Test
    void shortDateDependsOnAge() {
        assertEquals("14:05", Formats.shortDate(Instant.parse("2026-09-28T11:05:00Z"), ISTANBUL, TODAY));
        assertEquals("3 Ağu", Formats.shortDate(Instant.parse("2026-08-03T09:00:00Z"), ISTANBUL, TODAY));
        assertEquals("15 Oca 2025", Formats.shortDate(Instant.parse("2025-01-15T09:00:00Z"), ISTANBUL, TODAY));
        assertEquals("", Formats.shortDate(null, ISTANBUL, TODAY));
    }

    @Test
    void folderNamesAreReadable() {
        assertEquals("Gelen Kutusu", Formats.folderName("INBOX"));
        assertEquals("Gönderilmiş Postalar", Formats.folderName("[Gmail]/Gönderilmiş Postalar"));
        assertEquals("Faturalar", Formats.folderName("Faturalar"));
    }
}