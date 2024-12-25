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
import org.pastal.launcher.util.JsonUtils;
import org.tinylog.Logger;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

public class Fabric implements Addon {
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    @Override
    public String getName() {
        return "Fabric";
    }

    @Override
    public void setup(Installation installation, AddonVersion version, Consumer<ProgressData> progressCallback, Consumer<String> log) throws IOException {
        final String loaderVersion = version.getUrl();

        final RequestComponent requestComponent = Launcher.getInstance().getComponentManager().get(RequestComponent.class);
        final String fabricJsonUrl = loaderVersion + "/profile/json";
        final String fabricJsonContent = requestComponent.get(fabricJsonUrl, requestComponent.createStandardHeaders());
        final JsonObject fabricJson = JsonParser.parseString(fabricJsonContent).getAsJsonObject();

        final JsonObject vanillaJson = installation.getJson();
        final JsonArray vanillaLibraries = vanillaJson.getAsJsonArray("libraries");
        final JsonArray fabricLibraries = fabricJson.getAsJsonArray("libraries");

        for (JsonElement fabricLib : fabricLibraries) {
            vanillaLibraries.add(fabricLib);
        }

        final List<DownloadTask> libraryTasks = new ArrayList<>();
        for (JsonElement library : fabricLibraries) {
            JsonObject libraryObj = library.getAsJsonObject();
            String name = libraryObj.get("name").getAsString();
            String mvn = JsonUtils.convertMavenToPath(name);
            String url = libraryObj.get("url").getAsString() + mvn;
            libraryTasks.add(new DownloadTask(url, new File(new File(Launcher.getInstance().getRunningDirectory(), "libraries"), mvn).getAbsolutePath(), Launcher.getInstance().getComponentManager().get(RequestComponent.class)
                    .getFileSize(url)));
        }

        CompletableFuture<Void> downloadFuture = Launcher.getInstance().getComponentManager().get(DownloadEngine.class)
                .downloadAsync(libraryTasks, progressCallback);

        try {
            downloadFuture.get();
        } catch (InterruptedException | ExecutionException e) {
            throw new IOException("Failed to download Fabric libraries", e);
        }

        vanillaJson.addProperty("mainClass", fabricJson.get("mainClass").getAsString());
        if (fabricJson.has("arguments")) {
            if (!vanillaJson.has("arguments")) {
                vanillaJson.add("arguments", new JsonObject());
            }
            JsonObject fabricArguments = fabricJson.getAsJsonObject("arguments");
            JsonObject vanillaArguments = vanillaJson.getAsJsonObject("arguments");

            if (fabricArguments.has("game")) {
                if (!vanillaArguments.has("game")) {
                    vanillaArguments.add("game", new JsonArray());
                }
                JsonArray fabricGameArgs = fabricArguments.getAsJsonArray("game");
                JsonArray vanillaGameArgs = vanillaArguments.getAsJsonArray("game");

                for (int i = 0; i < fabricGameArgs.size(); i++) {
                    if (!vanillaGameArgs.contains(fabricGameArgs.get(i))) {
                        vanillaGameArgs.add(fabricGameArgs.get(i));
                    }
                }
            }

            if (fabricArguments.has("jvm")) {
                if (!vanillaArguments.has("jvm")) {
                    vanillaArguments.add("jvm", new JsonArray());
                }
                JsonArray fabricJvmArgs = fabricArguments.getAsJsonArray("jvm");
                JsonArray vanillaJvmArgs = vanillaArguments.getAsJsonArray("jvm");

                for (int i = 0; i < fabricJvmArgs.size(); i++) {
                    if (!vanillaJvmArgs.contains(fabricJvmArgs.get(i))) {
                        vanillaJvmArgs.add(fabricJvmArgs.get(i));
                    }
                }
            }
        }
        File versionJsonFile = new File(installation.getDirectory(), installation.getName() + ".json");
        try (Writer writer = new FileWriter(versionJsonFile)) {
            writer.write(gson.toJson(vanillaJson));
        }

        Launcher.getInstance().getInstallationManager().scan(Launcher.getInstance());
        Logger.info("Fabric installed successfully");
    }

    @Override
    public List<AddonVersion> getVersions(Installation installation) throws IOException {
        List<AddonVersion> versionList = new ArrayList<>();
        final RequestComponent requestComponent = Launcher.getInstance().getComponentManager().get(RequestComponent.class);

        final String loaderVersionsUrl = "https://meta.fabricmc.net/v2/versions/loader/" + installation.getClientVersion();
        final String response = requestComponent.get(loaderVersionsUrl, requestComponent.createStandardHeaders());

        final JsonArray versions = JsonParser.parseString(response).getAsJsonArray();
        if (versions.isEmpty()) {
            return versionList;
        }

        for (JsonElement version : versions) {
            JsonObject loaderVersion = version.getAsJsonObject().getAsJsonObject("loader");
            String versionString = loaderVersion.get("version").getAsString();
            String url = "https://meta.fabricmc.net/v2/versions/loader/" + installation.getClientVersion() + "/" + versionString;
            versionList.add(new AddonVersion(versionString, url, url));
        }

        return versionList;
    }
} 