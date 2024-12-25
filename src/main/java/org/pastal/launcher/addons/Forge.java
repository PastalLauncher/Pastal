package org.pastal.launcher.addons;

import com.google.gson.*;
import org.pastal.launcher.Launcher;
import org.pastal.launcher.api.interfaces.Addon;
import org.pastal.launcher.api.objects.AddonVersion;
import org.pastal.launcher.api.objects.Installation;
import org.pastal.launcher.api.objects.download.DownloadTask;
import org.pastal.launcher.api.objects.download.ProgressData;
import org.pastal.launcher.components.network.DownloadEngine;
import org.pastal.launcher.components.network.RequestComponent;
import org.pastal.launcher.util.IOUtils;
import org.tinylog.Logger;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

public class Forge implements Addon {
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    @Override
    public String getName() {
        return "Forge";
    }

    @Override
    public void setup(final Installation installation, final AddonVersion version, final Consumer<ProgressData> progressCallback, final Consumer<String> log) throws IOException {
        final String forgeUrl = version.getUrl();
        final File workDir = new File(installation.getDirectory(), "PastalForge");

        if (workDir.exists()) {
            IOUtils.deleteDirectory(workDir);
        }

        if (!workDir.mkdir())
            throw new IOException();

        final File tempDir = new File(workDir, "libraries");
        final File librariesDir = new File(Launcher.getInstance().getRunningDirectory(), "libraries");

        if (!librariesDir.exists()) {
            throw new IOException();
        }

        IOUtils.copyDir(librariesDir, tempDir);

        final File forgeInstaller = new File(workDir, "forge-installer.jar");

        CompletableFuture<Void> downloadFuture = Launcher.getInstance().getComponentManager().get(DownloadEngine.class)
                .downloadAsync(Collections.singletonList(
                        new DownloadTask(
                                forgeUrl,
                                forgeInstaller.getPath(),
                                Launcher.getInstance().getComponentManager().get(RequestComponent.class)
                                        .getFileSize(forgeUrl)
                        )
                ), progressCallback);

        try {
            downloadFuture.get();
        } catch (InterruptedException | ExecutionException e) {
            throw new IOException("Failed to download Forge installer", e);
        }

        final File agent = new File(workDir, "ModLoaderAgent.jar");

        IOUtils
                .writeFile(
                        agent,
                        IOUtils
                                .toByteArray(
                                        Launcher.class.getResourceAsStream("/ModLoaderAgent.jar")
                                )
                );

        final File launchProfile = new File(workDir, "launcher_profiles.json");

        IOUtils
                .writeFile(
                        launchProfile,
                        createLaunchProfile(installation.getClientVersion()).toString().getBytes(StandardCharsets.UTF_8)
                );

        final ProcessBuilder pb = new ProcessBuilder(
                installation.getBestJava().getPath(),
                "-cp",
                forgeInstaller.getAbsolutePath() + ";" + agent.getAbsolutePath(),
                "org.pastal.mla.ForgeAgent"
        );

        pb.directory(workDir);
        final Process process = pb.start();

        IOUtils.readStream(log, process.getInputStream());

        IOUtils.readStream(log, process.getErrorStream());

        int exitCode = 0;
        try {
            exitCode = process.waitFor();
        } catch (InterruptedException e) {
            Logger.error(e);
        }

        if (exitCode != 0) {
            throw new IOException("Forge patch failed with exit code: " + exitCode);
        }

        IOUtils.copyDir(tempDir, librariesDir);

        try (final JarFile jarFile = new JarFile(forgeInstaller)) {
            final ZipEntry profileEntry = jarFile.getEntry("install_profile.json");
            if (profileEntry == null) {
                throw new IOException("Invalid Forge installer: missing install_profile.json");
            }

            final JsonObject installProfile = JsonParser.parseString(
                    IOUtils
                            .toString(jarFile.getInputStream(profileEntry))
            ).getAsJsonObject();

            final List<DownloadTask> libraryTasks = new ArrayList<>();
            JsonArray libraries = installProfile.getAsJsonArray("libraries");

            if (libraries == null) {
                libraries = installProfile.get("versionInfo").getAsJsonObject().get("libraries").getAsJsonArray();
            }

            Launcher.getInstance().getInstallationManager().addLibs(libraryTasks, libraries, librariesDir);

            downloadFuture = Launcher.getInstance().getComponentManager().get(DownloadEngine.class)
                    .downloadAsync(libraryTasks, progressCallback);
            downloadFuture.join();
            try {
                downloadFuture.get();
            } catch (InterruptedException | ExecutionException e) {
                throw new IOException("Failed to download Forge libraries", e);
            }

            final ZipEntry versionEntry = jarFile.getEntry("version.json");
            if (versionEntry == null) {
                final JsonObject vanillaJson = installation.getJson();

                final JsonArray vanillaLibraries = vanillaJson.getAsJsonArray("libraries");
                final JsonArray forgeLibraries = installProfile.get("versionInfo").getAsJsonObject().getAsJsonArray("libraries");

                for (final JsonElement forgeLib : forgeLibraries) {
                    final JsonObject forgeLibObj = forgeLib.getAsJsonObject();
                    if (!forgeLibObj.has("name")) continue;

                    final String forgeName = forgeLibObj.get("name").getAsString();

                    boolean isDuplicate = false;

                    for (final JsonElement vanillaLib : vanillaLibraries) {
                        final JsonObject vanillaLibObj = vanillaLib.getAsJsonObject();
                        if (vanillaLibObj.has("name") &&
                                forgeName.equals(vanillaLibObj.get("name").getAsString())) {
                            isDuplicate = true;
                            break;
                        }
                    }

                    if (!isDuplicate) {
                        vanillaLibraries.add(forgeLib);
                    }
                }

                vanillaJson.addProperty("mainClass", installProfile.get("versionInfo").getAsJsonObject().get("mainClass").getAsString());
                if (installProfile.get("versionInfo").getAsJsonObject().has("minecraftArguments")) {
                    final String arguments = installProfile.get("versionInfo").getAsJsonObject().get("minecraftArguments").getAsString();
                    vanillaJson.addProperty("minecraftArguments", arguments);
                }
                final File versionJsonFile = new File(installation.getDirectory(), installation.getName() + ".json");
                try (final Writer writer = new FileWriter(versionJsonFile)) {
                    writer.write(gson.toJson(vanillaJson));
                }
            } else {
                final JsonObject forgeJson = JsonParser.parseString(
                        IOUtils
                                .toString(jarFile.getInputStream(versionEntry))
                ).getAsJsonObject();

                final JsonObject vanillaJson = installation.getJson();

                final JsonArray vanillaLibraries = vanillaJson.getAsJsonArray("libraries");
                final JsonArray forgeLibraries = forgeJson.getAsJsonArray("libraries");

                for (final JsonElement forgeLib : forgeLibraries) {
                    final JsonObject forgeLibObj = forgeLib.getAsJsonObject();
                    if (!forgeLibObj.has("name")) continue;

                    final String forgeName = forgeLibObj.get("name").getAsString();
                    boolean isDuplicate = false;

                    for (final JsonElement vanillaLib : vanillaLibraries) {
                        final JsonObject vanillaLibObj = vanillaLib.getAsJsonObject();
                        if (vanillaLibObj.has("name") &&
                                forgeName.equals(vanillaLibObj.get("name").getAsString())) {
                            isDuplicate = true;
                            break;
                        }
                    }

                    if (!isDuplicate) {
                        vanillaLibraries.add(forgeLib);
                    }
                }

                vanillaJson.addProperty("mainClass", forgeJson.get("mainClass").getAsString());
                if (forgeJson.has("arguments")) {
                    final JsonObject arguments = forgeJson.getAsJsonObject("arguments");
                    if (arguments.has("game")) {
                        final JsonArray gameArgs = arguments.getAsJsonArray("game");
                        if (!vanillaJson.has("arguments")) {
                            vanillaJson.add("arguments", new JsonObject());
                        }
                        if (!vanillaJson.getAsJsonObject("arguments").has("game")) {
                            vanillaJson.getAsJsonObject("arguments").add("game", new JsonArray());
                        }
                        vanillaJson.getAsJsonObject("arguments").getAsJsonArray("game").addAll(gameArgs);
                    }
                    if (arguments.has("jvm")) {
                        final JsonArray jvmArgs = arguments.getAsJsonArray("jvm");
                        if (!vanillaJson.getAsJsonObject("arguments").has("jvm")) {
                            vanillaJson.getAsJsonObject("arguments").add("jvm", new JsonArray());
                        }
                        vanillaJson.getAsJsonObject("arguments").getAsJsonArray("jvm").addAll(jvmArgs);
                    }
                }
                final File versionJsonFile = new File(installation.getDirectory(), installation.getName() + ".json");
                try (final Writer writer = new FileWriter(versionJsonFile)) {
                    writer.write(gson.toJson(vanillaJson));
                }
            }


        }

        IOUtils
                .deleteDirectory(workDir);

        Launcher.getInstance().getInstallationManager().scan(Launcher.getInstance());
        Logger.info("Forge installed successfully");
    }

    @Override
    public List<AddonVersion> getVersions(Installation installation) throws IOException {
        final List<AddonVersion> versionList = new ArrayList<>();
        final RequestComponent requestComponent = Launcher.getInstance().getComponentManager().get(RequestComponent.class);
        final String apiUrl = "https://bmclapi2.bangbang93.com/forge/minecraft/" + installation.getClientVersion();
        final String response = requestComponent.get(apiUrl, requestComponent.createStandardHeaders());

        final JsonArray versions = JsonParser.parseString(response).getAsJsonArray();
        if (versions.isEmpty()) {
            return versionList;
        }

        for (final JsonElement element : versions) {
            final JsonObject version = element.getAsJsonObject();
            final JsonArray files = version.getAsJsonArray("files");

            if (files != null && !files.isEmpty()) {
                for (final JsonElement fileElement : files) {
                    final JsonObject file = fileElement.getAsJsonObject();
                    if ("installer".equals(file.get("category").getAsString()) &&
                            "jar".equals(file.get("format").getAsString())) {

                        final String forgeVersion = version.get("version").getAsString();
                        final String branch = version.has("branch") ? version.get("branch") instanceof JsonNull ? "" : version.get("branch").getAsString() : "";

                        final String versionString = installation.getClientVersion() + "-" + forgeVersion +
                                (!branch.isEmpty() ? "-" + branch : "");

                        final String fileName1 = "forge-" + versionString + "-installer.jar";
                        final String fileName2 = "forge-" + versionString + "-" + installation.getClientVersion() + "-installer.jar";

                        final String url1 = "https://maven.minecraftforge.net/net/minecraftforge/forge/" +
                                versionString + "/" + fileName1;
                        final String url2 = "https://maven.minecraftforge.net/net/minecraftforge/forge/" +
                                versionString + "-" + installation.getClientVersion() + "/" + fileName2;

                        versionList.add(new AddonVersion(versionString, url1, url2));
                    }
                }
            }
        }
        Collections.reverse(versionList);
        return versionList;
    }

    private JsonObject createLaunchProfile(String client) {
        JsonObject jsonObject = new JsonObject();

        JsonObject profiles = new JsonObject();
        JsonObject forgeProfile = new JsonObject();
        forgeProfile.add("name", new JsonPrimitive("forge"));
        forgeProfile.add("type", new JsonPrimitive("custom"));
        forgeProfile.add("lastVersionId", new JsonPrimitive(client));
        forgeProfile.add("icon", new JsonPrimitive(""));
        profiles.add("forge", forgeProfile);

        jsonObject.add("profiles", profiles);

        jsonObject.add("selectedProfile", new JsonPrimitive("forge"));

        jsonObject.add("clientToken", new JsonPrimitive(""));

        return jsonObject;
    }

    private boolean isUrlValid(String url) throws IOException {
        RequestComponent requestComponent = Launcher.getInstance().getComponentManager().get(RequestComponent.class);
        try {
            requestComponent.getFileSize(url);
            return true;
        } catch (IOException e) {
            return false;
        }
    }
}