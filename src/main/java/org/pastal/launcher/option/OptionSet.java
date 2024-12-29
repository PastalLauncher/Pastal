package org.pastal.launcher.option;

import com.google.gson.*;
import lombok.SneakyThrows;
import org.pastal.launcher.Launcher;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;

public class OptionSet extends ArrayList<Option<?>> {
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final Path filePath;

    public OptionSet(String path) {
        this.filePath = new File(new File(Launcher.getInstance().getRunningDirectory(),"pastal"), path).toPath();
    }

    public OptionSet(File path) {
        this.filePath = path.toPath();
    }

    @SneakyThrows
    public void save() {
        try (BufferedWriter writer = Files.newBufferedWriter(filePath, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
            JsonArray jsonArray = new JsonArray();
            for (Option<?> option : this) {
                JsonObject jsonObject = new JsonObject();
                jsonObject.addProperty("name", option.getName());
                jsonObject.add("value", option.asJson());
                jsonArray.add(jsonObject);
            }
            gson.toJson(jsonArray, writer);
        }
    }

    @SneakyThrows
    public void load() {
        if (!Files.exists(filePath)) {
            return;
        }

        Gson gson = new Gson();
        try (BufferedReader reader = Files.newBufferedReader(filePath)) {
            JsonArray jsonArray = gson.fromJson(reader, JsonArray.class);
            for (JsonElement element : jsonArray) {
                JsonObject jsonObject = element.getAsJsonObject();
                String name = jsonObject.get("name").getAsString();
                Option<?> option = findByName(name);
                if (option != null) {
                    option.readJson(jsonObject.get("value"));
                }
            }
        }
    }

    private Option<?> findByName(String name) {
        for (Option<?> option : this) {
            if (option.getName().equals(name)) {
                return option;
            }
        }
        return null;
    }
}
