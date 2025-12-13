package com.example.cyberguard.secure;

import android.util.Base64;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

public class NotesCrypto {

    private static final int PBKDF2_ITERS = 150_000;
    private static final int KEY_BITS = 256;
    private static final int GCM_TAG_BITS = 128;

    public static class EncResult {
        public final String cipherTextB64;
        public final String ivB64;

        public EncResult(String cipherTextB64, String ivB64) {
            this.cipherTextB64 = cipherTextB64;
            this.ivB64 = ivB64;
        }
    }

    public static String generateSaltB64() {
        byte[] salt = new byte[16];
        new SecureRandom().nextBytes(salt);
        return Base64.encodeToString(salt, Base64.NO_WRAP);
    }

    public static SecretKey deriveKey(String masterPassword, String saltB64) throws Exception {
        byte[] salt = Base64.decode(saltB64, Base64.NO_WRAP);

        PBEKeySpec spec = new PBEKeySpec(
                masterPassword.toCharArray(),
                salt,
                PBKDF2_ITERS,
                KEY_BITS
        );

        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        byte[] keyBytes = factory.generateSecret(spec).getEncoded();
        return new SecretKeySpec(keyBytes, "AES");
    }

    public static EncResult encrypt(String plainText, SecretKey key) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, key);

        byte[] iv = cipher.getIV();
        byte[] cipherBytes = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

        return new EncResult(
                Base64.encodeToString(cipherBytes, Base64.NO_WRAP),
                Base64.encodeToString(iv, Base64.NO_WRAP)
        );
    }

    public static String decrypt(String cipherTextB64, String ivB64, SecretKey key) throws Exception {
        byte[] cipherBytes = Base64.decode(cipherTextB64, Base64.NO_WRAP);
        byte[] iv = Base64.decode(ivB64, Base64.NO_WRAP);

        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_BITS, iv));

        byte[] plainBytes = cipher.doFinal(cipherBytes);
        return new String(plainBytes, StandardCharsets.UTF_8);
    }
}
