package org.pastal.launcher.util;

import lombok.experimental.UtilityClass;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

@UtilityClass
public final class HashUtils {
    public String md5(String input) {
        return hashString(input, "MD5");
    }

    public String sha1(String input) {
        return hashString(input, "SHA-1");
    }

    public String sha256(String input) {
        return hashString(input, "SHA-256");
    }

    private String hashString(String input, String algorithm) {
        try {
            MessageDigest digest = MessageDigest.getInstance(algorithm);
            byte[] hashBytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));

            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("搞毛？算法不可用！" + algorithm, e);
        }
    }
}
