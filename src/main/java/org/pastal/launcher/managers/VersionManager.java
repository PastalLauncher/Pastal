package org.pastal.launcher.managers;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import lombok.Getter;
import org.pastal.launcher.Launcher;
import org.pastal.launcher.api.ValueManager;
import org.pastal.launcher.api.objects.Version;
import org.pastal.launcher.components.network.RequestComponent;
import org.tinylog.Logger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
public class VersionManager extends ValueManager<Version> {
    private final List<Version> versions = new ArrayList<>();
    private Version latestStable;
    private Version latestSnapshot;

    public List<Version> getVersions() throws IOException {
        if (versions.isEmpty()) {
            fetch(Launcher.getInstance());
        }
        return versions;
    }

    public void fetch(final Launcher launcher) throws IOException {
        Logger.info("Fetching version information from manifest URL.");
        final RequestComponent requestComponent = launcher.getComponentManager().get(RequestComponent.class);
        final JsonObject result = requestComponent.getAsJsonObject(
                launcher.getMirrorManager().getCurrentMirror().getMirrorUrl("https://launchermeta.mojang.com/mc/game/version_manifest_v2.json"),
                requestComponent.createStandardHeaders()
        );

        final Map<String, Version> versionMap = parseVersions(result.getAsJsonArray("versions"));

        final JsonObject latest = result.getAsJsonObject("latest");
        this.versions.addAll(versionMap.values());
        this.latestStable = versionMap.get(latest.get("release").getAsString());
        this.latestSnapshot = versionMap.get(latest.get("snapshot").getAsString());

        Logger.info("Latest stable version: {}", latestStable != null ? latestStable.getId() : "Not found");
        Logger.info("Latest snapshot version: {}", latestSnapshot != null ? latestSnapshot.getId() : "Not found");
    }

    private Map<String, Version> parseVersions(final JsonArray versions) {
        Logger.info("Parsing versions from manifest.");
        final Map<String, Version> versionMap = new HashMap<>();

        versions.forEach(element -> {
            final JsonObject versionObject = element.getAsJsonObject();
            final Version version = new Version(
                    versionObject.get("id").getAsString(),
                    versionObject.get("type").getAsString(),
                    versionObject.get("url").getAsString(),
                    versionObject.get("releaseTime").getAsString()
            );
            registerValue(version);
            versionMap.put(version.getId(), version);
        });

        Logger.info("Completed parsing {} versions.", getValues().size());
        return versionMap;
    }
}
