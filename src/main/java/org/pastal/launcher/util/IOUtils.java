package org.pastal.launcher.util;

import lombok.experimental.UtilityClass;
import org.pastal.launcher.natives.NativeUtil;
import org.tinylog.Logger;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Scanner;
import java.util.function.Consumer;


@UtilityClass
public class IOUtils {
    public void stripeHeadToFile(BufferedImage image, File outputFilePath) {
        try {
            BufferedImage subImage = image.getSubimage(8, 8, 8, 8);
            BufferedImage scaledImage = new BufferedImage(80, 80, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2d = scaledImage.createGraphics();
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            g2d.drawImage(subImage, 0, 0, 80, 80, null);
            g2d.dispose();
            ImageIO.write(scaledImage, "png", outputFilePath);
        } catch (IOException e) {
            Logger.error(e);
        }
    }

    public void readStream(final Consumer<String> log, final InputStream stream) {
        new Thread(() -> {
            try (final BufferedReader reader = new BufferedReader(new InputStreamReader(stream))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    log.accept(line);
                }
            } catch (Exception e) {
                Logger.error(e);
            }
        }).start();
    }


    public void deleteDirectory(final File dir) {
        final File[] files = dir.listFiles();
        if (files != null) {
            for (final File file : files) {
                if (file.isDirectory()) {
                    deleteDirectory(file);
                } else {
                    file.delete();
                }
            }
        }
        dir.delete();
    }

    public void writeFile(final File file, final InputStream inputStream) throws IOException {
        try (final FileOutputStream outputStream = new FileOutputStream(file)) {
            final byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
        }
    }

    public void writeFile(final File file, final byte[] bytes) throws IOException {
        final FileOutputStream fos = new FileOutputStream(file);
        fos.write(bytes);
        fos.close();
    }

    public byte[] toByteArray(final InputStream inputStream) {
        final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        int nRead;
        final byte[] data = new byte[16384];
        try {
            while ((nRead = inputStream.read(data, 0, data.length)) != -1) {
                buffer.write(data, 0, nRead);
            }
        } catch (Exception e) {
            Logger.error(e);
        }
        return buffer.toByteArray();
    }


    public String toString(final InputStream inputStream) {
        final Scanner scanner = new Scanner(inputStream, StandardCharsets.UTF_8.name());
        scanner.useDelimiter("\\A");
        final String result = scanner.hasNext() ? scanner.next() : "";
        scanner.close();
        return result;
    }

    public void copyDir(final File folder, final File targetFolder) throws IOException {
        if (!folder.exists()) {
            throw new FileNotFoundException("Source folder does not exist: " + folder.getAbsolutePath());
        }

        if (!targetFolder.exists()) {
            targetFolder.mkdirs();
        }

        final File[] files = folder.listFiles();
        if (files != null) {
            for (final File file : files) {
                final File targetFile = new File(targetFolder, file.getName());
                if (file.isDirectory()) {
                    copyDir(file, targetFile);
                } else {
                    copyFile(file, targetFile);
                }
            }
        }
    }

    private void copyFile(final File sourceFile, final File targetFile) throws IOException {
        try (final InputStream in = Files.newInputStream(sourceFile.toPath());
             final OutputStream out = Files.newOutputStream(targetFile.toPath())) {
            final byte[] buffer = new byte[1024];
            int length;
            while ((length = in.read(buffer)) > 0) {
                out.write(buffer, 0, length);
            }
        }
    }
}
