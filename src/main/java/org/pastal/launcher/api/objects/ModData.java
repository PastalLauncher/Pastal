package org.pastal.launcher.api.objects;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.moandjiezana.toml.Toml;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.tinylog.Logger;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

@Getter
@Setter
@ToString
public class ModData {
    private final File file;
    private boolean blank = false;
    private String modId;
    private String name;
    private String description;
    private List<String> authors;

    ModData(File file) {
        this.authors = new ArrayList<>();
        this.file = file;
    }

    public static ModData create(File jarFile) {
        try (JarFile jar = new JarFile(jarFile)) {
            JarEntry mcmodInfo = jar.getJarEntry("mcmod.info");
            JarEntry fabricModJson = jar.getJarEntry("fabric.mod.json");
            JarEntry modsToml = jar.getJarEntry("META-INF/neoforge.mods.toml");
            if (modsToml == null) {
                modsToml = jar.getJarEntry("META-INF/mods.toml");
            }

            if (mcmodInfo != null) {
                try (InputStream is = jar.getInputStream(mcmodInfo)) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                    JsonArray jsonArray = JsonParser.parseReader(reader).getAsJsonArray();
                    JsonObject modInfo = jsonArray.get(0).getAsJsonObject();

                    ModData modData = new ModData(jarFile);
                    modData.setModId(modInfo.get("modid").getAsString());
                    modData.setName(modInfo.get("name").getAsString());
                    modData.setDescription(modInfo.get("description").getAsString());

                    JsonArray authorList = modInfo.getAsJsonArray("authorList");
                    if (authorList != null) {
                        for (int i = 0; i < authorList.size(); i++) {
                            modData.addAuthor(authorList.get(i).getAsString());
                        }
                    }

                    return modData;
                }
            } else if (fabricModJson != null) {
                try (InputStream is = jar.getInputStream(fabricModJson)) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                    JsonObject modJson = JsonParser.parseReader(reader).getAsJsonObject();

                    ModData modData = new ModData(jarFile);
                    modData.setModId(modJson.get("id").getAsString());
                    modData.setName(modJson.get("name").getAsString());
                    modData.setDescription(modJson.get("description").getAsString());

                    JsonArray authors = modJson.getAsJsonArray("authors");
                    if (authors != null) {
                        for (int i = 0; i < authors.size(); i++) {
                            modData.addAuthor(authors.get(i).getAsString());
                        }
                    }

                    return modData;
                }
            } else if (modsToml != null) {
                try (InputStream is = jar.getInputStream(modsToml)) {
                    Toml toml = new Toml();
                    toml.read(is);
                    ModData modData = new ModData(jarFile);
                    modData.setModId(toml.getString("mods[0].modId"));
                    modData.setName(toml.getString("mods[0].displayName"));
                    modData.setDescription(toml.getString("mods[0].description"));
                    modData.addAuthor(toml.getString("mods[0].authors"));
                    return modData;
                }
            }

            ModData modData = new ModData(jarFile);
            modData.setBlank(true);
            return modData;
        } catch (Exception e) {
            Logger.error(e);
        }
        return null;
    }

    public void addAuthor(String author) {
        this.authors.add(author);
    }
}
