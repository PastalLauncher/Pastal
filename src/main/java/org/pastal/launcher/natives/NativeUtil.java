package org.pastal.launcher.natives;

import lombok.SneakyThrows;
import org.tinylog.Logger;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

public final class NativeUtil {
    static {
        loadNative();
    }

    @SneakyThrows
    private static void loadNative() {
        if (!System.getProperty("os.name").toLowerCase().contains("win")) {
            Logger.info("Non-windows os detected, skip native.");
            return;
        }

        InputStream inputStream = NativeUtil.class.getResourceAsStream("/RegistryUtil.dll");
        if (inputStream == null) {
            return;
        }
        File tempFile = File.createTempFile("RegistryUtil", ".dll");
        tempFile.deleteOnExit();
        try (FileOutputStream outputStream = new FileOutputStream(tempFile)) {
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
        }
        System.load(tempFile.getAbsolutePath());
    }

    public static native byte[] unprotectData(byte[] data);

    public static native long getHash(String input);

    public static native String getHardwareIdentify();

    public static native String readRegistryValue(String path, String name);

    public static native long getPidFromHandle(long handle);

}
