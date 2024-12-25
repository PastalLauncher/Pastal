package org.pastal.launcher.loader;

import org.pastal.launcher.Launcher;
import org.tinylog.Logger;

import java.io.File;
import java.util.function.Consumer;

public class Main {
    public static void main(String[] args) {
        Logger.info("Starting launcher as standalone mode...");

        Launcher launcher = new Launcher(
                new File(".minecraft"),
                Logger::error
        );
        launcher.setup();

        Logger.info("Launcher loaded.");

    }
}
