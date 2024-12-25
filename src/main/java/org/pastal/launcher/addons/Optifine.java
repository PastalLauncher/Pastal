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

import java.io.*;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;

public class Optifine implements Addon {
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final Pattern optifineTypePattern = Pattern.compile("/([^/]+)/[^/]+$");
    private final Pattern optifinePatchPattern = Pattern.compile("/([^/]+)$");

    private static Process getProcess(final Installation installation, final File optifineJar, final File optifineLibFile) throws IOException {
        final File minecraftJar = new File(installation.getDirectory(), installation.getName() + ".jar");
        final ProcessBuilder processBuilder = new ProcessBuilder(
                installation.getBestJava().getPath(),
                "-cp",
                optifineJar.getAbsolutePath(),
                "optifine.Patcher",
                minecraftJar.getAbsolutePath(),
                optifineJar.getAbsolutePath(),
                optifineLibFile.getAbsolutePath()
        );
        return processBuilder.start();
    }

    @Override
    public String getName() {
        return "Optifine";
    }

    @Override
    public void setup(final Installation installation, final AddonVersion version, final Consumer<ProgressData> progressCallback, final Consumer<String> log) throws IOException {
        final String optifineUrl = version.getUrl();
        final File optifineJar = new File(installation.getDirectory(), "optifine.jar");

        final CompletableFuture<Void> completableFuture = Launcher.getInstance().getComponentManager().get(DownloadEngine.class)
                .downloadAsync(Collections.singletonList(
                        new DownloadTask(
                                optifineUrl,
                                optifineJar.getPath(),
                                Launcher.getInstance().getComponentManager().get(RequestComponent.class)
                                        .getFileSize(optifineUrl)
                        )
                ), progressCallback);

        try {
            completableFuture.get();
        } catch (InterruptedException | ExecutionException e) {
            throw new IOException("Failed to download optifine", e);
        }

        final JsonObject json = installation.getJson();
        final JsonArray libraries = json.has("libraries") ?
                json.getAsJsonArray("libraries") :
                new JsonArray();
        if (!json.has("libraries")) {
            json.add("libraries", libraries);
        }

        final String mcVersion = installation.getClientVersion();
        final String type = match(optifineTypePattern, optifineUrl);
        final String patch = match(optifinePatchPattern, optifineUrl);

        final JsonObject optifineLib = new JsonObject();
        optifineLib.addProperty("name", String.format("optifine:OptiFine:%s_%s_%s", mcVersion, type, patch));
        libraries.add(optifineLib);

        final File optifineLibFile = new File(Launcher.getInstance().getRunningDirectory(),
                "libraries/optifine/OptiFine/" + mcVersion + "_" + type + "_" + patch + "/OptiFine-" + mcVersion + "_" + type + "_" + patch + ".jar");
        optifineLibFile.getParentFile().mkdirs();

        boolean launchWrapperExisted = false;
        try (final JarFile installerJar = new JarFile(optifineJar)) {
            if (installerJar.getEntry("optifine/Patcher.class") != null) {
                final Process process = getProcess(installation, optifineJar, optifineLibFile);

                IOUtils.readStream(log, process.getInputStream());

                IOUtils.readStream(log, process.getErrorStream());


                final int exitCode = process.waitFor();
                if (exitCode != 0) {
                    throw new IOException("Optifine patch failed with exit code: " + exitCode);
                }
            } else {
                Files.copy(optifineJar.toPath(), optifineLibFile.toPath());
            }

            final ZipEntry launchWrapperJar = installerJar.getEntry("launchwrapper-2.0.jar");
            if (launchWrapperJar != null) {
                final JsonObject lwLib = new JsonObject();
                lwLib.addProperty("name", "optifine:launchwrapper:2.0");
                libraries.add(lwLib);

                final File lwFile = new File(Launcher.getInstance().getRunningDirectory(),
                        "libraries/net/minecraft/launchwrapper/2.0/launchwrapper-2.0.jar");
                lwFile.getParentFile().mkdirs();
                try (InputStream is = installerJar.getInputStream(launchWrapperJar)) {
                    Files.copy(is, lwFile.toPath());
                }
                launchWrapperExisted = true;
            }

            final ZipEntry launchWrapperVersionText = installerJar.getEntry("launchwrapper-of.txt");
            if (launchWrapperVersionText != null) {
                final String launchWrapperVersion = new BufferedReader(new InputStreamReader(installerJar.getInputStream(launchWrapperVersionText)))
                        .readLine().trim();
                final ZipEntry launchWrapperJarOf = installerJar.getEntry("launchwrapper-of-" + launchWrapperVersion + ".jar");

                if (launchWrapperJarOf != null) {
                    final JsonObject lib = new JsonObject();
                    lib.addProperty("name", "optifine:launchwrapper-of:" + launchWrapperVersion);
                    libraries.add(lib);

                    final File file = new File(Launcher.getInstance().getRunningDirectory(),
                            "libraries/optifine/launchwrapper-of/" + launchWrapperVersion + "/launchwrapper-of-" + launchWrapperVersion + ".jar");
                    file.getParentFile().mkdirs();
                    try (final InputStream is = installerJar.getInputStream(launchWrapperJarOf)) {
                        Files.copy(is, file.toPath());
                    }
                    launchWrapperExisted = true;
                }
            }

            final ZipEntry buildofText = installerJar.getEntry("buildof.txt");
            if (buildofText != null) {
                final String buildof = new BufferedReader(new InputStreamReader(installerJar.getInputStream(buildofText)))
                        .readLine().trim();

                if ("cpw.mods.bootstraplauncher.BootstrapLauncher".equals(json.get("mainClass").getAsString())) {
                    try {
                        String[] s = buildof.split("-");
                        if (s.length >= 2) {
                            if (Integer.parseInt(s[0]) < 20210924 ||
                                    (Integer.parseInt(s[0]) == 20210924 && Integer.parseInt(s[1]) < 190833)) {
                                throw new IOException("This version of Optifine is incompatible with Forge");
                            }
                        }
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
        } catch (InterruptedException e) {
            Logger.error(e);
        }

        if (!launchWrapperExisted) {
            final JsonObject lwLib = new JsonObject();
            lwLib.addProperty("name", "net.minecraft:launchwrapper:1.12");
            libraries.add(lwLib);
        }

        if (json.has("minecraftArguments")) {
            final String minecraftArguments = json.get("minecraftArguments").getAsString();
            json.addProperty("minecraftArguments", "--tweakClass optifine.OptiFineTweaker " + minecraftArguments);
        } else {
            final JsonObject arguments = json.has("arguments") ?
                    json.getAsJsonObject("arguments") :
                    new JsonObject();
            if (!json.has("arguments")) {
                json.add("arguments", arguments);
            }
            final JsonArray game = arguments.has("game") ?
                    arguments.getAsJsonArray("game") :
                    new JsonArray();
            if (!arguments.has("game")) {
                arguments.add("game", game);
            }
            game.add("--tweakClass");
            game.add("optifine.OptiFineTweaker");
        }

        final String mainClass = json.get("mainClass").getAsString();
        if (!mainClass.equals("cpw.mods.bootstraplauncher.BootstrapLauncher") &&
                !mainClass.equals("cpw.mods.modlauncher.Launcher")) {
            json.addProperty("mainClass", "net.minecraft.launchwrapper.Launch");
        }

        final File versionJson = new File(installation.getDirectory(), installation.getName() + ".json");
        try (final Writer writer = new FileWriter(versionJson)) {
            writer.write(gson.toJson(json));
        }

        optifineJar.delete();

        Launcher.getInstance().getInstallationManager().scan(Launcher.getInstance());
        Logger.info("Optifine installed successfully");
    }

    @Override
    public List<AddonVersion> getVersions(Installation installation) throws IOException {
        List<AddonVersion> versionList = new ArrayList<>();
        final RequestComponent requestComponent = Launcher.getInstance().getComponentManager().get(RequestComponent.class);

        final String apiUrl = "https://bmclapi2.bangbang93.com/optifine/" + installation.getClientVersion();
        final String response = requestComponent.get(apiUrl, requestComponent.createStandardHeaders());

        final JsonArray versions = JsonParser.parseString(response).getAsJsonArray();
        if (versions.isEmpty()) {
            return versionList;
        }

        for (JsonElement version : versions) {

            final String type = version.getAsJsonObject().get("type").getAsString();
            final String patch = version.getAsJsonObject().get("patch").getAsString();
            final String url = "https://bmclapi2.bangbang93.com/optifine/" + installation.getClientVersion() + "/" + type + "/" + patch;

            versionList.add(new AddonVersion(version.getAsJsonObject().get("filename").getAsString().replace(".jar", ""), url, url));
        }

        Collections.reverse(versionList);
        return versionList;
    }

    private String match(final Pattern pattern, final String url) {
        final Matcher matcher = pattern.matcher(url);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return "unknown";
    }
}
