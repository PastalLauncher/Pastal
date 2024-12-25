package org.pastal.launcher.option.implement;

import org.pastal.launcher.option.Option;

import java.util.function.Supplier;

public class BoolOption extends Option<Boolean> {

    public BoolOption(String name, String description, Boolean value, Supplier<Boolean> visible) {
        super(name, description, value, visible, 8);
    }

    public BoolOption(String name, String description, Boolean value) {
        super(name, description, value, () -> true, 8);
    }
}
