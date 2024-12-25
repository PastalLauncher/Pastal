package org.pastal.launcher;

import lombok.Getter;
import org.pastal.launcher.event.EventManager;
import org.pastal.launcher.event.events.ShutdownEvent;
import org.pastal.launcher.http.RouterManager;
import org.pastal.launcher.managers.*;

import java.io.File;
import java.util.function.Consumer;

@Getter
public class Launcher {

    @Getter
    private static Launcher instance;

    private final File runningDirectory;
    private final Consumer<Throwable> exceptionHandler;

    private final EventManager eventManager;
    private final ComponentManager componentManager;
    private final MirrorManager mirrorManager;
    private final VersionManager versionManager;
    private final InstallationManager installationManager;
    private final JavaManager javaManager;
    private final AccountManager accountManager;
    private final AddonManager addonManager;
    private final ConfigManager configManager;
    private final ModManager modManager;
    private final ModProfileManager modProfileManager;
    private final MigratManager migratManager;
    private final RouterManager routerManager;

    public Launcher(final File runningDirectory, final Consumer<Throwable> exceptionHandler) {
        instance = this;

        if (!runningDirectory.exists()) {
            if (!runningDirectory.mkdirs()) {
                throw new IllegalStateException();
            }
        }

        this.runningDirectory = runningDirectory;
        this.exceptionHandler = exceptionHandler;
        this.eventManager = new EventManager();
        this.configManager = new ConfigManager(this);
        this.componentManager = new ComponentManager();
        this.mirrorManager = new MirrorManager();
        this.versionManager = new VersionManager();
        this.installationManager = new InstallationManager();
        this.javaManager = new JavaManager(this);
        this.accountManager = new AccountManager(this);
        this.addonManager = new AddonManager();
        this.modManager = new ModManager(this);
        this.modProfileManager = new ModProfileManager(this);
        this.migratManager = new MigratManager();
        this.routerManager = new RouterManager(this);

        Runtime.getRuntime().addShutdownHook(new Thread(this::callShutdown));
    }

    private void callShutdown() {
        eventManager.call(new ShutdownEvent());
    }

    public void setup() {
        try {
            this.javaManager.scan();
            this.installationManager.scan(this);
            this.accountManager.load();
            this.modManager.scan();
        } catch (Exception e) {
            exceptionHandler.accept(e);
        }
    }
}
