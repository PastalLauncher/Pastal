package org.pastal.launcher.config;

import org.pastal.launcher.option.OptionSet;

public class Settings {
    private final OptionSet optionSet;

    public Settings(){
        optionSet = new OptionSet("settings.json");
    }
}
