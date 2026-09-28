package com.mrcdprm.emailclient.ui;

import java.util.Locale;
import org.kordamp.ikonli.Ikon;
import org.kordamp.ikonli.feather.Feather;
import org.kordamp.ikonli.javafx.FontIcon;

/** Arayüzdeki simgeler (Feather simge seti). */
public final class Icons {

    private Icons() {
    }

    public static FontIcon of(Ikon icon) {
        return new FontIcon(icon);
    }

    /** Klasörün adına göre uygun simge: Gelen, Gönderilmiş, Çöp vb. (Türkçe ve İngilizce adlar). */
    public static Ikon forFolder(String fullName) {
        if (fullName.equalsIgnoreCase("INBOX"))
            return Feather.INBOX;
        // Locale.ROOT: Türkçe kuralla "INBOX" -> "ınbox" olurdu (noktasız ı)
        final String name = fullName.toLowerCase(Locale.ROOT);
        if (name.contains("sent") || name.contains("gönderil"))
            return Feather.SEND;
        if (name.contains("trash") || name.contains("çöp") || name.contains("deleted"))
            return Feather.TRASH_2;
        if (name.contains("spam") || name.contains("junk"))
            return Feather.ALERT_OCTAGON;
        if (name.contains("draft") || name.contains("taslak"))
            return Feather.FILE_TEXT;
        if (name.contains("star") || name.contains("yıldız"))
            return Feather.STAR;
        if (name.contains("important") || name.contains("önemli"))
            return Feather.BOOKMARK;
        if (name.contains("all mail") || name.contains("tüm postalar"))
            return Feather.ARCHIVE;
        return Feather.FOLDER;
    }
}