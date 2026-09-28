package com.mrcdprm.emailclient.security;

import com.sun.jna.platform.win32.Crypt32Util;
import com.sun.jna.platform.win32.Win32Exception;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Optional;

/**
 * Şifreleri Windows DPAPI ile şifreler. Şifreli veri sadece aynı Windows kullanıcısı
 * tarafından, aynı bilgisayarda çözülebilir; dosya başka yere kopyalansa bile işe yaramaz.
 */
public final class SecretStore {

    private SecretStore() {
    }

    public static byte[] encrypt(String secret) {
        final byte[] plain = secret.getBytes(StandardCharsets.UTF_8);
        try {
            return Crypt32Util.cryptProtectData(plain);
        } finally {
            Arrays.fill(plain, (byte) 0); // düz metni bellekte bırakma
        }
    }

    /** Çözülemezse (bozuk veri, başka kullanıcı ya da bilgisayar) boş döner. */
    public static Optional<String> decrypt(byte[] encrypted) {
        try {
            final byte[] plain = Crypt32Util.cryptUnprotectData(encrypted);
            final String secret = new String(plain, StandardCharsets.UTF_8);
            Arrays.fill(plain, (byte) 0);
            return Optional.of(secret);
        } catch (Win32Exception | IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
