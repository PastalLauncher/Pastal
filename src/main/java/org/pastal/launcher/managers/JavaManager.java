package org.pastal.launcher.managers;

import com.google.gson.*;
import org.pastal.launcher.Launcher;
import org.pastal.launcher.api.ValueManager;
import org.pastal.launcher.api.objects.JavaInstallation;
import org.pastal.launcher.util.Multithreading;
import org.tinylog.Logger;

import java.io.*;
import java.util.Comparator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JavaManager extends ValueManager<JavaInstallation> {
    private static final Pattern VERSION_PATTERN = Pattern.compile("version \"(.+?)\"");
    private static final String[] COMMON_PATHS = {
            System.getProperty("java.home"),
            "C:\\Program Files",
            "C:\\Program Files (x86)",
            "/usr/lib/jvm",
            "/Library/Java/JavaVirtualMachines"
    };

    private final File configFile;
    private final Gson gson;

    public JavaManager(Launcher launcher) {
        final File pastalDir = new File(launcher.getRunningDirectory(), "pastal");
        if (!pastalDir.exists()) {
            pastalDir.mkdirs();
        }
        this.configFile = new File(pastalDir, "java_installations.json");
        this.gson = new GsonBuilder().setPrettyPrinting().create();
    }

    public void scan() {


        if (loadFromFile()) {
            Logger.info("Loaded {} Java installation(s).", values.size());
            return;
        }

        Logger.info("Scanning for Java installations...");
        values.clear();

        scanPath();

        for (final String path : COMMON_PATHS) {
            final File dir = new File(path);
            if (dir.exists() && dir.isDirectory()) {
                scanDirectory(dir);
            }
        }

        Logger.info("Found {} Java installation(s)", values.size());
        saveToFile();
    }

    private boolean loadFromFile() {
        if (!configFile.exists()) {
            return false;
        }

        try (final Reader reader = new FileReader(configFile)) {
            final JsonArray array = JsonParser.parseReader(reader).getAsJsonArray();
            for (final JsonElement element : array) {
                final JsonObject obj = element.getAsJsonObject();
                final File executable = new File(obj.get("executable").getAsString());
                final String version = obj.get("version").getAsString();
                final int majorVersion = obj.get("majorVersion").getAsInt();

                final JavaInstallation installation = new JavaInstallation(executable, version, majorVersion);

                if (executable.exists() && verifyJavaInstallation(installation)) {
                    registerValue(installation);
                }
            }
            return !values.isEmpty();
        } catch (IOException e) {
            Logger.warn("Failed to load Java installations from cache", e);
        }
        return false;
    }

    private void saveToFile() {
        try (final Writer writer = new FileWriter(configFile)) {
            gson.toJson(values, writer);
        } catch (IOException e) {
            Logger.warn("Failed to save Java installations to cache", e);
        }
    }

    private boolean verifyJavaInstallation(JavaInstallation installation) {
        try {
            final ProcessBuilder pb = new ProcessBuilder(installation.getPath(), "-version");
            pb.redirectErrorStream(true);
            final Process process = pb.start();

            try (final BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                final String line = reader.readLine();
                if (line != null) {
                    final Matcher matcher = VERSION_PATTERN.matcher(line);
                    if (matcher.find()) {
                        final String version = matcher.group(1);
                        return version.equals(installation.getVersion());
                    }
                }
            }

            process.waitFor();
        } catch (IOException | InterruptedException e) {
            return false;
        }
        return false;
    }

    private void scanPath() {
        final String pathVar = System.getenv("PATH");
        if (pathVar == null) return;

        for (final String path : pathVar.split(File.pathSeparator)) {
            final File java = new File(path, "java" + (System.getProperty("os.name").toLowerCase().contains("win") ? ".exe" : ""));
            if (java.exists() && java.isFile()) {
                checkAndAddJava(java);
            }
        }
    }

    private void scanDirectory(final File directory) {
        final File[] files = directory.listFiles();
        if (files == null) return;

        for (final File file : files) {
            if (file.isDirectory()) {
                scanDirectory(file);
                final File binDir = new File(file, "bin");
                if (binDir.exists() && binDir.isDirectory()) {
                    final File java = new File(binDir, "java" + (System.getProperty("os.name").toLowerCase().contains("win") ? ".exe" : ""));
                    if (java.exists() && java.isFile()) {
                        checkAndAddJava(java);
                    }
                }
            }
        }
    }

    private void checkAndAddJava(final File javaExecutable) {
        try {
            final ProcessBuilder pb = new ProcessBuilder(javaExecutable.getAbsolutePath(), "-version");
            pb.redirectErrorStream(true);
            final Process process = pb.start();

            try (final BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                final String line = reader.readLine();
                if (line != null) {
                    final Matcher matcher = VERSION_PATTERN.matcher(line);
                    if (matcher.find()) {
                        final String version = matcher.group(1);
                        final JavaInstallation installation = new JavaInstallation(
                                javaExecutable,
                                version,
                                parseMajorVersion(version)
                        );
                        if (values.stream().noneMatch(existing ->
                                existing.getExecutable().equals(
                                        installation.getExecutable()))) {
                            registerValue(installation);
                            Logger.info("Found Java {} at {}", installation.getVersion(),
                                    installation.getExecutable());
                        }
                    }
                }
            }

            process.waitFor();
        } catch (IOException | InterruptedException e) {
            Logger.warn("Failed to check Java at: " + javaExecutable.getAbsolutePath());
        }
    }

    private int parseMajorVersion(final String version) {
        if (version.startsWith("1.")) {
            return Integer.parseInt(version.substring(2, 3));
        }
        final int dot = version.indexOf('.');
        if (dot > 0) {
            try {
                return Integer.parseInt(version.substring(0, dot));
            } catch (NumberFormatException e) {
                return -1;
            }
        }
        return -1;
    }

    public JavaInstallation getBestJavaFor(final int requiredVersion) {
        return values.stream()
                .filter(java -> java.getMajorVersion() >= requiredVersion)
                .min(Comparator.comparingInt(JavaInstallation::getMajorVersion))
                .orElse(null);
    }

    public void rescan() {
        if (configFile.exists()) {
            configFile.delete();
        }
        scan();
    }
}