package org.pastal.launcher.util;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import lombok.experimental.UtilityClass;


@UtilityClass
public class JsonUtils {

    public String convertMavenToPath(String mavenNotation) {
        if (mavenNotation == null || !mavenNotation.matches(".+:.+:.+")) {
            throw new IllegalArgumentException("Invalid Maven notation");
        }

        String[] parts = mavenNotation.split(":");
        String groupId = parts[0];
        String artifactId = parts[1];
        String version = parts[2];

        String groupPath = groupId.replace('.', '/');

        String jarName = artifactId + "-" + version + ".jar";

        return groupPath + "/" + artifactId + "/" + version + "/" + jarName;
    }

    public boolean isLibraryAllowed(final JsonObject library) {
        if (!library.has("rules")) {
            return true;
        }

        final JsonArray rules = library.getAsJsonArray("rules");
        boolean allow = false;

        for (final JsonElement rule : rules) {
            final JsonObject ruleObj = rule.getAsJsonObject();
            final String action = ruleObj.get("action").getAsString();

            if (ruleObj.has("os")) {
                final JsonObject os = ruleObj.getAsJsonObject("os");
                final String name = os.get("name").getAsString();
                if (name.equals(getOSName())) {
                    allow = action.equals("allow");
                }
            } else {
                allow = action.equals("allow");
            }
        }

        return allow;
    }

    private static String getOSName() {
        final String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) return "windows";
        if (os.contains("mac")) return "osx";
        return "linux";
    }
}
