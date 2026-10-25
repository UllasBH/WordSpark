package com.wordspark.app.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

/**
 * Lightweight salted SHA-256 password hashing.
 *
 * This is intentionally simple (no external crypto libraries, no
 * enterprise-grade key derivation like bcrypt/Argon2) since WordSpark is a
 * college MAD mini project, not a production authentication system.
 * Passwords are still never stored in plain text.
 */
public class PasswordUtil {

    private static final String DELIMITER = ":";

    public static String hash(String plainPassword) {
        try {
            byte[] salt = new byte[16];
            new SecureRandom().nextBytes(salt);
            byte[] hashed = digest(plainPassword, salt);
            return toHex(salt) + DELIMITER + toHex(hashed);
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 is always available on Android; this should never happen.
            throw new RuntimeException(e);
        }
    }

    public static boolean matches(String plainPassword, String storedHash) {
        if (storedHash == null || !storedHash.contains(DELIMITER)) return false;
        try {
            String[] parts = storedHash.split(DELIMITER);
            byte[] salt = fromHex(parts[0]);
            byte[] expected = fromHex(parts[1]);
            byte[] actual = digest(plainPassword, salt);
            return MessageDigest.isEqual(expected, actual);
        } catch (Exception e) {
            return false;
        }
    }

    private static byte[] digest(String password, byte[] salt) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        md.update(salt);
        return md.digest(password.getBytes());
    }

    private static String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) sb.append(String.format("%02x", b));
        return sb.toString();
    }

    private static byte[] fromHex(String hex) {
        int len = hex.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                    + Character.digit(hex.charAt(i + 1), 16));
        }
        return data;
    }
}
