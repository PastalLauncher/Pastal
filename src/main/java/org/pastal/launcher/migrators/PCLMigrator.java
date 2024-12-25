package org.pastal.launcher.migrators;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.pastal.launcher.Launcher;
import org.pastal.launcher.api.interfaces.Migrator;
import org.pastal.launcher.api.interfaces.Importable;
import org.pastal.launcher.api.objects.account.RefreshTokenAccount;
import org.pastal.launcher.enums.ClientIdentification;
import org.pastal.launcher.natives.NativeUtil;

import javax.crypto.Cipher;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESKeySpec;
import javax.crypto.spec.IvParameterSpec;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * credit:
 * <a href="https://github.com/Hex-Dragon/PCL2/">PCL2 source code</a>
 */
public class PCLMigrator implements Migrator {
    @Override
    public String getName() {
        return "PCL";
    }

    @Override
    public List<Importable> findData() {
        List<Importable> importables = new ArrayList<>();
        JsonObject object = JsonParser.parseString(getLoginData()).getAsJsonObject();
        for (Map.Entry<String, JsonElement> element : object.entrySet()) {
            String token = element.getValue().getAsString();
            Importable importable = new Importable() {
                @Override
                public String getDisplayString() {
                    return "Account " + element.getKey();
                }

                @Override
                public void importData() {
                    Launcher.getInstance().getAccountManager()
                            .forceRegisterValue(new RefreshTokenAccount(token, ClientIdentification.PCL, true));
                }
            };
            importables.add(importable);
        }
        return importables;
    }

    @Override
    public boolean hasData() {
        if (!System.getProperty("os.name").toLowerCase().contains("win")) {
            return false;
        }
        try {
            NativeUtil.readRegistryValue("Software\\PCL", "Identify");
            return !getLoginData().equals("{}"); // has premium account
        } catch (Throwable t) {
            return false;
        }
    }

    private static String getLoginData() {
        String key = "PCL" + NativeUtil.getHardwareIdentify();
        String loginData = NativeUtil.readRegistryValue("Software\\PCL", "LoginMsJson");
        return decryptString(loginData, key);
    }

    public static String decryptString(String sourceString, String key) {
        try {
            String secretKey = getSecretKey(key);
            byte[] keyBytes = secretKey.getBytes(StandardCharsets.UTF_8);
            byte[] ivBytes = "95168702".getBytes(StandardCharsets.UTF_8);
            if (keyBytes.length > 8) {
                keyBytes = Arrays.copyOf(keyBytes, 8);
            } else if (keyBytes.length < 8) {
                keyBytes = Arrays.copyOf(keyBytes, 8);
            }
            DESKeySpec desKeySpec = new DESKeySpec(keyBytes);
            SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("DES");
            javax.crypto.SecretKey secretKeyObj = keyFactory.generateSecret(desKeySpec);
            Cipher cipher = Cipher.getInstance("DES/CBC/PKCS5Padding");
            IvParameterSpec ivSpec = new IvParameterSpec(ivBytes);
            cipher.init(Cipher.DECRYPT_MODE, secretKeyObj, ivSpec);
            byte[] decodedBytes = Base64.getDecoder().decode(sourceString);
            return new String(cipher.doFinal(decodedBytes));
        } catch (Exception e) {
            throw new RuntimeException("Decryption failed", e);
        }
    }

    public static String getSecretKey(String key) {
        if (key == null || key.isEmpty()) {
            return "@;$ Abv2";
        } else {
            String hashString = String.valueOf(NativeUtil.getHash(key));
            String filledString = strFill(hashString, "X", (byte) 8);
            return filledString.substring(0, 8);
        }
    }

    public static String strFill(String str, String code, byte length) {
        if (str.length() > length) {
            return str.substring(0, length);
        } else {
            StringBuilder result = new StringBuilder();
            for (int i = 0; i < length - str.length(); i++) {
                result.append(code.charAt(0));
            }
            return result + str;
        }
    }
}
