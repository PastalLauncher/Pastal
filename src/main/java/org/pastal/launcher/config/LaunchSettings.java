package org.pastal.launcher.config;

import lombok.Getter;
import org.pastal.launcher.option.OptionSet;
import org.pastal.launcher.option.implement.BoolOption;
import org.pastal.launcher.option.implement.ComboOption;
import org.pastal.launcher.option.implement.SliderOption;
import org.pastal.launcher.option.implement.TextOption;

import java.awt.*;
import java.io.File;
import java.util.Collections;

@Getter
public class LaunchSettings extends OptionSet {

    private final TextOption javaExcutable = new TextOption("Java Executable", "Path to java executable", "");
    private final TextOption javaArguments = new TextOption("Java Arguments", "Arguments to pass to java", "");
    private final TextOption gameArguments = new TextOption("Game Arguments", "Arguments to pass to game", "");
    private final SliderOption minMemory = new SliderOption("Minimum Memory", "Memory to allocate to game", "MB", 1024, 128, 1024, 1);
    private final SliderOption maxMemory = new SliderOption("Maximum Memory", "Memory to allocate to game", "MB", 2048, 128, 2048, 1);
    private final SliderOption windowWidth = new SliderOption("Window Width", "Width of game window", 1600, 128, 1920, 1);
    private final SliderOption windowHeight = new SliderOption("Window Height", "Height of game window", 900, 128, 1080, 1);
    private final ComboOption modProfile = new ComboOption("Mod Profile", "Mod profile to use", "Disabled");
    private final BoolOption tokenProtection = new BoolOption("Token Protection", "Protect your access token avoid some shitty rats stealing", false);

    public LaunchSettings(File path) {
        super(path);
        add(javaExcutable);
        add(javaArguments);
        add(gameArguments);
        add(minMemory);
        add(maxMemory);
        add(windowWidth);
        add(windowHeight);
        add(modProfile);
        add(tokenProtection);
    }
}
