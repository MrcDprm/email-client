package com.mrcdprm.emailclient.ui;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Arayüzde gösterilen tarih ve klasör adlarının biçimi. */
public final class Formats {

    private static final Locale TURKISH = Locale.forLanguageTag("tr");
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm", TURKISH);
    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("d MMM", TURKISH);
    private static final DateTimeFormatter FULL = DateTimeFormatter.ofPattern("d MMM yyyy", TURKISH);
    private static final DateTimeFormatter LONG = DateTimeFormatter.ofPattern("d MMMM yyyy HH:mm", TURKISH);

    private Formats() {
    }

    /** Listede kısa tarih: bugünse saat, bu yılsa gün ve ay, daha eskiyse tam tarih. */
    public static String shortDate(Instant instant, ZoneId zone, LocalDate today) {
        if (instant == null)
            return "";
        final ZonedDateTime time = instant.atZone(zone);
        if (time.toLocalDate().equals(today))
            return TIME.format(time);
        if (time.getYear() == today.getYear())
            return DAY.format(time);
        return FULL.format(time);
    }

    public static String longDate(Instant instant) {
        return instant == null ? "" : LONG.format(instant.atZone(ZoneId.systemDefault()));
    }

    /** "INBOX" -> "Gelen Kutusu", "[Gmail]/Gönderilmiş Postalar" -> "Gönderilmiş Postalar". */
    public static String folderName(String fullName) {
        if (fullName.equalsIgnoreCase("INBOX"))
            return "Gelen Kutusu";
        final int slash = fullName.lastIndexOf('/');
        return slash >= 0 ? fullName.substring(slash + 1) : fullName;
    }
}