package com.mrcdprm.emailclient.mail;

import java.time.Instant;

/**
 * Mesaj listesinde bir satır. uid, IMAP sunucusunun klasör içinde verdiği kalıcı numaradır;
 * mesajın gövdesini sonradan okumak ya da silmek için kullanılır.
 */
public record MessageSummary(long uid, String from, String subject, Instant date, boolean seen) {
}
