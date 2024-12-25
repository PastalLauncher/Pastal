package org.pastal.launcher.managers;

import lombok.Getter;
import org.pastal.launcher.Launcher;
import org.pastal.launcher.api.ValueManager;
import org.pastal.launcher.api.objects.ModData;
import org.pastal.launcher.components.file.FileListenerComponent;
import org.pastal.launcher.event.annotations.EventTarget;
import org.pastal.launcher.event.events.FileEvent;

import java.io.File;

@Getter
public class ModManager extends ValueManager<ModData> {
    private final File modDirectory;

    public ModManager(Launcher launcher) {
        this.modDirectory = new File(launcher.getRunningDirectory(), "mods");

        if (!modDirectory.exists()) {
            if (!modDirectory.mkdirs()) {
                throw new IllegalStateException();
            }
        }

        launcher.getComponentManager().get(FileListenerComponent.class)
                .addListener(modDirectory);
        launcher.getEventManager().register(this);
    }

    @EventTarget
    public void onFile(FileEvent event) {
        scan();
    }

    public void scan() {
        for (File file : modDirectory.listFiles()) {
            if (file.getName().endsWith(".jar")) {
                registerValue(ModData.create(file));
            }
        }
    }
}
