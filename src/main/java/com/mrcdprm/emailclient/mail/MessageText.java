package com.mrcdprm.emailclient.mail;

import jakarta.mail.BodyPart;
import jakarta.mail.MessagingException;
import jakarta.mail.Multipart;
import jakarta.mail.Part;
import java.io.IOException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.parser.Parser;
import org.jsoup.safety.Safelist;

/**
 * E-posta gövdesini ekranda gösterilecek düz metne çevirir.
 * HTML asla render edilmez: sadece içindeki metin alınır, kod ve bağlantılar çalışmaz.
 */
public final class MessageText {

    private static final int MAX_LENGTH = 200_000; // aşırı büyük gövdeler arayüzü kilitlemesin

    private MessageText() {
    }

    public static String of(Part part) throws MessagingException, IOException {
        final String plain = find(part, "text/plain");
        final String text = plain != null ? plain : htmlToText(find(part, "text/html"));
        final String result = text == null || text.isBlank() ? "(Bu mesajda gösterilebilecek metin yok.)" : text.strip();
        return result.length() > MAX_LENGTH ? result.substring(0, MAX_LENGTH) + "\n\n(Mesaj kısaltıldı.)" : result;
    }

    /** Parçalar içinde verilen türdeki ilk metni bulur (ekleri atlar). */
    private static String find(Part part, String mimeType) throws MessagingException, IOException {
        if (Part.ATTACHMENT.equalsIgnoreCase(part.getDisposition()))
            return null;
        if (part.isMimeType(mimeType) && part.getContent() instanceof String text)
            return text;
        if (part.isMimeType("multipart/*") && part.getContent() instanceof Multipart multipart) {
            for (int i = 0; i < multipart.getCount(); i++) {
                final BodyPart child = multipart.getBodyPart(i);
                final String found = find(child, mimeType);
                if (found != null)
                    return found;
            }
        }
        return null;
    }

    /** HTML'den okunabilir düz metin: satır sonları korunur, etiketler ve betikler atılır. */
    public static String htmlToText(String html) {
        if (html == null)
            return null;
        final Document document = Jsoup.parse(html);
        document.outputSettings().prettyPrint(false);
        document.select("script, style").remove();
        document.select("br").before("\\n");
        document.select("p, div, tr, li, h1, h2, h3, h4, h5, h6").before("\\n");
        final String withMarkers = document.body() == null ? "" : document.body().html().replace("\\n", "\n");
        final String cleaned = Jsoup.clean(withMarkers, "", Safelist.none(), new Document.OutputSettings().prettyPrint(false));
        return Parser.unescapeEntities(cleaned, false).replaceAll("[ \\t]+\n", "\n").replaceAll("\n{3,}", "\n\n");
    }
}
