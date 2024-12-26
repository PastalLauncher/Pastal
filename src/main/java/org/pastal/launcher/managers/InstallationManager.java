package org.pastal.launcher.managers;

import com.google.gson.*;
import org.pastal.launcher.Launcher;
import org.pastal.launcher.api.ValueManager;
import org.pastal.launcher.api.objects.Installation;
import org.pastal.launcher.api.objects.Version;
import org.pastal.launcher.api.objects.download.DownloadTask;
import org.pastal.launcher.api.objects.download.ProgressData;
import org.pastal.launcher.components.network.DownloadEngine;
import org.pastal.launcher.components.network.RequestComponent;
import org.pastal.launcher.util.IOUtils;
import org.pastal.launcher.util.JsonUtils;
import org.tinylog.Logger;

import java.io.*;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class InstallationManager extends ValueManager<Installation> {
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private File versionsDirectory;

    public void scan(final Launcher launcher) {
        this.versionsDirectory = new File(launcher.getRunningDirectory(), "versions");
        if (!versionsDirectory.exists()) {
            if (!versionsDirectory.mkdirs()) {
                throw new IllegalStateException();
            }
        }

        Logger.info("Scanning installed launch profiles...");
        values.clear();

        final File[] versionDirs = versionsDirectory.listFiles();
        if (versionDirs == null) {
            Logger.warn("Failed to access versions directory");
            return;
        }

        Arrays.stream(versionDirs)
                .filter(File::isDirectory)
                .forEach(dir -> {
                    final File jsonFile = new File(dir, dir.getName() + ".json");
                    if (jsonFile.exists()) {
                        final String type = dir.getName();
                        final Installation installation = new Installation(
                                dir.getName(),
                                type,
                                dir
                        );
                        registerValue(installation);
                        Logger.info("Found installation: {} ({})", installation.getName(), installation.getClientVersion());
                    }
                });

        Logger.info("Scan completed, found {} installations", values.size());
    }

    public Installation getInstallation(final String name) {
        return values.stream()
                .filter(installation -> installation.getName().equals(name))
                .findFirst()
                .orElse(null);
    }

    public boolean isInstalled(final String name) {
        return getInstallation(name) != null;
    }

    public void checkInstallation(final String versionId, final String versionName){
        if (isInstalled(versionName)) {
            throw new IllegalStateException("Version already exists: " + versionName);
        }
        try {
            Launcher.getInstance().getVersionManager().getVersions().stream()
                    .filter(v -> v.getId().equals(versionId))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Version not found: " + versionId));
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    public void createInstallation(final String versionId, final String versionName, final Consumer<ProgressData> progressCallback) throws IOException {
        checkInstallation(versionId,versionName);

        Logger.info("Installing {} ({})...", versionName, versionId);

        final File versionDir = new File(versionsDirectory, versionName);
        if (!versionDir.exists() && !versionDir.mkdirs()) {
            throw new IllegalStateException();
        }

        final RequestComponent requestComponent = Launcher.getInstance().getComponentManager().get(RequestComponent.class);

        final Version version = Launcher.getInstance().getVersionManager().getValues().stream()
                .filter(v -> v.getId().equals(versionId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Version not found: " + versionId));

        final JsonObject versionJson = requestComponent.getAsJsonObject(
                Launcher.getInstance().getMirrorManager().getCurrentMirror().getMirrorUrl(version.getUrl()),
                requestComponent.createStandardHeaders()
        );

        versionJson.addProperty("id", versionName);
        versionJson.addProperty("version", versionId);

        final File versionJsonFile = new File(versionDir, versionName + ".json");
        try (final FileWriter writer = new FileWriter(versionJsonFile)) {
            writer.write(gson.toJson(versionJson));
        }

        final File assetsDir = new File(Launcher.getInstance().getRunningDirectory(), "assets");
        final File librariesDir = new File(Launcher.getInstance().getRunningDirectory(), "libraries");
        final File nativesDir = new File(versionDir, "natives");

        assetsDir.mkdirs();
        librariesDir.mkdirs();
        nativesDir.mkdirs();

        final List<DownloadTask> downloadTasks = new ArrayList<>();

        final JsonObject clientDownload = versionJson.getAsJsonObject("downloads").getAsJsonObject("client");
        downloadTasks.add(new DownloadTask(
                clientDownload.get("url").getAsString(),
                new File(versionDir, versionName + ".jar").getPath(),
                clientDownload.get("size").getAsLong()
        ));

        final JsonObject assetIndex = versionJson.getAsJsonObject("assetIndex");
        final File indexFile = new File(assetsDir, "indexes/" + assetIndex.get("id").getAsString() + ".json");
        downloadTasks.add(new DownloadTask(
                assetIndex.get("url").getAsString(),
                indexFile.getPath(),
                assetIndex.get("size").getAsLong()
        ));

        addLibs(downloadTasks, versionJson.getAsJsonArray("libraries"), librariesDir);

        CompletableFuture<Void> downloadFuture = Launcher.getInstance().getComponentManager().get(DownloadEngine.class).downloadAsync(downloadTasks, progressCallback);
        downloadFuture.join();
        try {
            downloadFuture.get();
        } catch (InterruptedException | ExecutionException e) {
            throw new IOException("Failed to download client", e);
        }

        downloadTasks.clear();
        final JsonObject assets = JsonParser.parseString(IOUtils.toString(Files.newInputStream(indexFile.toPath()))).getAsJsonObject();
        addAssets(downloadTasks, assets.getAsJsonObject("objects"), assetsDir, progressCallback);

        downloadFuture = Launcher.getInstance().getComponentManager().get(DownloadEngine.class).downloadTotal("Assets", downloadTasks, progressCallback);
        downloadFuture.join();
        try {
            downloadFuture.get();
        } catch (InterruptedException | ExecutionException e) {
            throw new IOException("Failed to download assets", e);
        }

        final JsonArray libraries = versionJson.getAsJsonArray("libraries");
        for (final JsonElement lib : libraries) {
            final JsonObject library = lib.getAsJsonObject();
            if (JsonUtils.isLibraryAllowed(library)) {
                final JsonObject downloads = library.getAsJsonObject("downloads");
                if (downloads.has("classifiers")) {
                    final JsonObject classifiers = downloads.get("classifiers").getAsJsonObject();
                    final String nativeName = getNativeName(classifiers);

                    if (nativeName == null) {
                        continue;
                    }

                    final File path = new File(librariesDir, classifiers.get(nativeName).getAsJsonObject().get("path").getAsString());

                    if (path.exists()) {
                        extractJarEntries(path, nativesDir);
                    }
                }
            }
        }

        scan(Launcher.getInstance());
    }

    private String getNativeName(final JsonObject classifiers) {
        final String osName = System.getProperty("os.name").toLowerCase(Locale.ROOT);
        final String nativeName;

        if (osName.contains("win")) {
            nativeName = classifiers.has("natives-windows") ? "natives-windows" : "natives-windows-64";
        } else if (osName.contains("mac")) {
            nativeName = classifiers.has("natives-osx") ? "natives-osx" : "natives-macos";
        } else {
            nativeName = "natives-linux";
        }

        return classifiers.has(nativeName) ? nativeName : null;
    }

    private void extractJarEntries(final File jarFile, final File nativesDir) throws IOException {
        try (final JarFile jar = new JarFile(jarFile)) {
            final Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                final JarEntry entry = entries.nextElement();
                if (entry.isDirectory() || entry.getName().contains("META-INF")) {
                    continue;
                }
                final File targetFile = new File(nativesDir, entry.getName().substring(entry.getName().lastIndexOf('/') + 1));
                try (final InputStream inputStream = jar.getInputStream(entry);
                     final OutputStream outputStream = Files.newOutputStream(targetFile.toPath())) {
                    final byte[] buffer = new byte[1024];
                    int bytesRead;
                    while ((bytesRead = inputStream.read(buffer)) != -1) {
                        outputStream.write(buffer, 0, bytesRead);
                    }
                }
            }
        }
    }


    private void addAssets(final List<DownloadTask> downloadTasks, final JsonObject objects, final File assetsDir, final Consumer<ProgressData> progressCallback) throws IOException {
        for (final String key : objects.keySet()) {
            final JsonObject asset = objects.getAsJsonObject(key);
            final String hash = asset.get("hash").getAsString();
            final String path = "objects/" + hash.substring(0, 2) + "/" + hash;
            final File file = new File(assetsDir, path);
            if (!file.exists()) {
                downloadTasks.add(new DownloadTask(
                        "https://resources.download.minecraft.net/" + hash.substring(0, 2) + "/" + hash,
                        file.getPath(),
                        asset.get("size").getAsLong()
                ));
            }
        }
    }

    public void addLibs(final List<DownloadTask> libraryTasks, final JsonArray libraries, final File librariesDir) {
        for (final JsonElement lib : libraries) {
            final JsonObject library = lib.getAsJsonObject();
            if (JsonUtils.isLibraryAllowed(library)) {
                if (library.has("downloads")) {

                    final JsonObject downloads = library.getAsJsonObject("downloads");

                    if (downloads.has("artifact")) {
                        final JsonObject artifact = downloads.getAsJsonObject("artifact");
                        final File targetFile = new File(librariesDir, artifact.get("path").getAsString());
                        if (!isFileValid(targetFile, artifact.get("size").getAsLong()) && artifact.get("url").getAsString().startsWith("http")) {
                            libraryTasks.add(new DownloadTask(
                                    artifact.get("url").getAsString(),
                                    targetFile.getPath(),
                                    artifact.get("size").getAsLong()
                            ));
                        }
                    }

                    if (downloads.has("classifiers")) {
                        final JsonObject classifiers = downloads.get("classifiers").getAsJsonObject();
                        final String nativeName;

                        if ((nativeName = getNativeName(classifiers)) == null) {
                            continue;
                        }

                        final File path = new File(librariesDir, classifiers.get(nativeName).getAsJsonObject().get("path").getAsString());
                        final String url = classifiers.get(nativeName).getAsJsonObject().get("url").getAsString();

                        if (!path.exists()) {
                            libraryTasks.add(new DownloadTask(
                                    url,
                                    path.getPath(),
                                    classifiers.get(nativeName).getAsJsonObject().get("size").getAsLong()
                            ));
                        }
                    }
                } else {
                    String path = JsonUtils.convertMavenToPath(library.get("name").getAsString());
                    final File targetFile = new File(librariesDir, path);
                    if (!targetFile.exists()) {
                        try {
                            libraryTasks.add(new DownloadTask(
                                    "https://libraries.minecraft.net/" + path,
                                    targetFile.getPath(),
                                    Launcher.getInstance().getComponentManager().get(RequestComponent.class).getFileSize("https://libraries.minecraft.net/" + path)
                            ));
                        } catch (IOException e) {
                            try {
                                libraryTasks.add(new DownloadTask(
                                        "https://maven.minecraftforge.net/" + path,
                                        targetFile.getPath(),
                                        Launcher.getInstance().getComponentManager().get(RequestComponent.class).getFileSize("https://maven.minecraftforge.net/" + path)
                                ));
                            } catch (IOException ex) {
                                throw new RuntimeException(ex);
                            }
                        }

                    }
                }
            }
        }
    }

    private boolean isFileValid(final File file, final long expectedSize) {
        return file.exists() && file.isFile() && file.length() == expectedSize;
    }
}