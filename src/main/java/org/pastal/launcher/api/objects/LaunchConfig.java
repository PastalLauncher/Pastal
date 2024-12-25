package org.pastal.launcher.api.objects;

import com.google.gson.JsonObject;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LaunchConfig {
    private String javaPath;
    private int minMemory = 1024;
    private int maxMemory = 2048;
    private int width = 1600;
    private int height = 900;
    private String jvmArgs = "";
    private String modProfile;

    public JsonObject toJson() {
        final JsonObject json = new JsonObject();
        if (javaPath != null) {
            json.addProperty("javaPath", javaPath);
        }
        if (modProfile != null) {
            json.addProperty("modProfile", modProfile);
        }
        json.addProperty("minMemory", minMemory);
        json.addProperty("maxMemory", maxMemory);
        json.addProperty("jvmArgs", jvmArgs);
        json.addProperty("width", width);
        json.addProperty("height", height);
        return json;
    }

    public void fromJson(final JsonObject json) {
        if (json.has("modProfile")) {
            modProfile = json.get("modProfile").getAsString();
        }
        if (json.has("javaPath")) {
            javaPath = json.get("javaPath").getAsString();
        }
        if (json.has("minMemory")) {
            minMemory = json.get("minMemory").getAsInt();
        }
        if (json.has("maxMemory")) {
            maxMemory = json.get("maxMemory").getAsInt();
        }
        if (json.has("jvmArgs")) {
            jvmArgs = json.get("jvmArgs").getAsString();
        }
        if (json.has("width")) {
            width = json.get("width").getAsInt();
        }
        if (json.has("height")) {
            height = json.get("height").getAsInt();
        }

    }
}