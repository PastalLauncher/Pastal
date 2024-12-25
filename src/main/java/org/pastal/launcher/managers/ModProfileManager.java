package org.pastal.launcher.managers;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.pastal.launcher.Launcher;
import org.pastal.launcher.api.ValueManager;
import org.pastal.launcher.api.objects.ModData;
import org.pastal.launcher.api.objects.ModProfile;
import org.pastal.launcher.event.annotations.EventTarget;
import org.pastal.launcher.event.events.ShutdownEvent;
import org.pastal.launcher.util.IOUtils;
import org.tinylog.Logger;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Optional;

public class ModProfileManager extends ValueManager<ModProfile> {
    private final File profilesDir;
    private final File modsBackupDir;
    private ModProfile activeProfile;

    public ModProfileManager(Launcher launcher) {
        this.profilesDir = new File(Launcher.getInstance().getRunningDirectory(), "profiles");
        this.modsBackupDir = new File(profilesDir, "mods_backup");
        if (!profilesDir.exists()) profilesDir.mkdirs();
        if (!modsBackupDir.exists()) modsBackupDir.mkdirs();
        loadProfiles();
        launcher.getEventManager().register(this);
    }

    private void loadProfiles() {
        File[] profileDirs = profilesDir.listFiles(File::isDirectory);
        if (profileDirs == null) return;

        for (File dir : profileDirs) {
            if (dir.equals(modsBackupDir)) continue;

            File configFile = new File(dir, "profile.json");
            if (!configFile.exists()) continue;

            try {
                JsonObject json = JsonParser.parseReader(new FileReader(configFile)).getAsJsonObject();
                ModProfile profile = new ModProfile(json.get("name").getAsString(), dir);
                File[] mods = dir.listFiles((dir1, name) -> name.endsWith(".jar"));
                if (mods != null) {
                    for (File mod : mods) {
                        profile.getMods().add(ModData.create(mod));
                    }
                }
                registerValue(profile);
            } catch (Exception e) {
                Logger.error("Failed to load profile: " + dir.getName(), e);
            }
        }
    }

    public void createProfile(String name) {
        File profileDir = new File(profilesDir, name);
        if (profileDir.exists()) throw new IllegalArgumentException("Profile already exists: " + name);

        profileDir.mkdirs();
        ModProfile profile = new ModProfile(name, profileDir);
        saveProfile(profile);
        registerValue(profile);
    }

    public void renameProfile(ModProfile profile, String newName) {
        if (getProfile(newName).isPresent()) {
            throw new IllegalArgumentException("Profile already exists: " + newName);
        }

        File newDir = new File(profile.getDirectory().getParentFile(), newName);
        if (!profile.getDirectory().renameTo(newDir)) {
            throw new IllegalStateException("Failed to rename profile directory");
        }

        profile.setName(newName);
        profile.setDirectory(newDir);
        saveProfile(profile);
    }

    public void saveProfile(ModProfile profile) {
        File profileDir = new File(profilesDir, profile.getName());
        if (!profileDir.exists()) profileDir.mkdirs();

        try (Writer writer = new FileWriter(new File(profileDir, "profile.json"))) {
            writer.write(profile.toJson().toString());
        } catch (IOException e) {
            Logger.error("Failed to save profile: " + profile.getName(), e);
        }
    }

    public void activateProfile(ModProfile profile) throws IOException {
        File modsDir = new File(Launcher.getInstance().getRunningDirectory(), "mods");

        if (modsDir.exists()) {
            Files.move(modsDir.toPath(), new File(modsBackupDir, "mods").toPath(), StandardCopyOption.REPLACE_EXISTING);
        }

        modsDir.mkdirs();

        for (ModData mod : profile.getMods()) {
            Files.copy(mod.getFile().toPath(), new File(modsDir, mod.getName()).toPath());
        }

        this.activeProfile = profile;
    }

    public void restoreBackup() throws IOException {
        if (activeProfile == null) return;

        File modsDir = new File(Launcher.getInstance().getRunningDirectory(), "mods");
        File backupDir = new File(modsBackupDir, "mods");

        if (modsDir.exists()) {
            IOUtils.deleteDirectory(modsDir);
        }

        if (backupDir.exists()) {
            Files.move(backupDir.toPath(), modsDir.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }

        activeProfile = null;
    }

    @EventTarget
    public void onShutdown(ShutdownEvent event) {
        try {
            restoreBackup();
        } catch (IOException e) {
            Logger.error(e);
        }
    }

    public Optional<ModProfile> getProfile(String name) {
        return getValues().stream()
                .filter(p -> p.getName().equals(name))
                .findFirst();
    }
} 