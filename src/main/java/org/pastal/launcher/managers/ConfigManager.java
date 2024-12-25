package org.pastal.launcher.managers;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import org.pastal.launcher.Launcher;

import java.io.*;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class ConfigManager {
    private final Gson gson;
    private final File file;
    private Map<String, Object> valueMap;

    public ConfigManager(Launcher launcher) {
        final File pastalDir = new File(launcher.getRunningDirectory(), "pastal");
        if (!pastalDir.exists()) {
            pastalDir.mkdirs();
        }
        this.file = new File(pastalDir, "settings.json");
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        this.loadConfig();
    }

    private void loadConfig() {
        if (file.exists()) {
            try (Reader reader = new FileReader(file)) {
                Type type = new TypeToken<Map<String, Object>>() {
                }.getType();
                this.valueMap = gson.fromJson(reader, type);
                if (this.valueMap == null) {
                    this.valueMap = new HashMap<>();
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else {
            this.valueMap = new HashMap<>();
        }
    }

    private void saveConfig() {
        try (Writer writer = new FileWriter(file)) {
            gson.toJson(this.valueMap, writer);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public <T> void saveValue(String key, T value) {
        this.valueMap.put(key, value);
        this.saveConfig();
    }

    public <T> T getValue(String key, Class<T> clazz) {
        Object value = this.valueMap.get(key);
        if (value != null) {
            return gson.fromJson(gson.toJson(value), clazz);
        }
        throw new NullPointerException("Config key not found: " + key);
    }

    public <T> Optional<T> findValue(String key, Class<T> clazz) {
        Object value = this.valueMap.get(key);
        if (value != null) {
            return Optional.ofNullable(gson.fromJson(gson.toJson(value), clazz));
        }
        return Optional.empty();
    }
}
