package com.mrcdprm.emailclient.mail;

import java.time.Instant;

/**
 * Mesaj listesinde bir satır. uid, IMAP sunucusunun klasör içinde verdiği kalıcı numaradır;
 * mesajın gövdesini sonradan okumak ya da silmek için kullanılır.
 * fromAddress ve messageId yanıtlarken kullanılır (cevabın kime gideceği ve hangi mesaja ait olduğu).
 */
public record MessageSummary(long uid, String from, String fromAddress, String subject, Instant date,
                             boolean seen, String messageId) {
}
