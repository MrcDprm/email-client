package com.mrcdprm.emailclient.mail;

/** Sunucu bağlantısının şifreleme türü. */
public enum Security {
    /** Bağlantı baştan şifreli (IMAP 993, SMTP 465). */
    SSL,
    /** Önce düz bağlanıp sonra şifreliye geçer (SMTP 587). */
    STARTTLS,
    /** Şifresiz; sadece testlerdeki yerel sunucu için. */
    NONE
}
