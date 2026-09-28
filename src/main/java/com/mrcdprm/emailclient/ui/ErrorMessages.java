package com.mrcdprm.emailclient.ui;

import jakarta.mail.AuthenticationFailedException;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.AddressException;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import javax.net.ssl.SSLException;
import org.eclipse.angus.mail.util.MailConnectException;

/**
 * Teknik hataları kullanıcıya gösterilecek Türkçe mesajlara çevirir.
 * Ayrıntılı hata (stack trace) kullanıcıya gösterilmez, sadece konsola yazılır.
 */
public final class ErrorMessages {

    private ErrorMessages() {
    }

    public static String of(Throwable error) {
        for (Throwable cause = error; cause != null; cause = cause.getCause()) {
            if (cause instanceof AuthenticationFailedException)
                return "Giriş başarısız: e-posta adresi ya da şifre hatalı.\n"
                        + "Gmail için normal şifren değil, 16 haneli \"uygulama şifresi\" gerekir.";
            if (cause instanceof AddressException)
                return cause.getMessage();
            if (cause instanceof UnknownHostException)
                return "Sunucu bulunamadı. İnternet bağlantını ve sunucu adresini kontrol et.";
            if (cause instanceof SocketTimeoutException)
                return "Sunucu cevap vermedi. Biraz sonra tekrar dene.";
            if (cause instanceof SSLException)
                return "Güvenli bağlantı kurulamadı. Sunucu adresi ve port ayarlarını kontrol et.";
            if (cause instanceof MailConnectException)
                return "Sunucuya bağlanılamadı. İnternet bağlantını ve port ayarlarını kontrol et.";
        }
        System.err.println("Beklenmeyen hata: " + error);
        if (error instanceof MessagingException || error instanceof IOException)
            return "E-posta sunucusuyla iletişimde bir sorun oluştu. Tekrar dene.";
        return "Beklenmeyen bir hata oluştu.";
    }
}
