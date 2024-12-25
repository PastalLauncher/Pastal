package org.pastal.launcher.api.objects;

import com.google.gson.JsonObject;
import lombok.Getter;
import lombok.Setter;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class ModProfile {
    private String name;
    private List<ModData> mods = new ArrayList<>();
    private File directory;

    public ModProfile(String name, File directory) {
        this.name = name;
        this.directory = directory;
    }

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("name", name);
        return json;
    }
} 