package org.pastal.launcher.option.implement;

import java.util.function.Supplier;
import lombok.Getter;
import org.pastal.launcher.option.Option;

import java.util.ArrayList;
import java.util.Arrays;

public class ComboOption extends Option<String> {

    @Getter
    private final String[] strings;

    public ComboOption(String name, String description, Supplier<Boolean> visible, String... values) {
        super(name, description, values[0], visible, 20);
        this.strings = values;
    }

    public ComboOption(String name, String description, String... values) {
        super(name, description, values[0], () -> true, 20);
        this.strings = values;
    }

    public ArrayList<String> getAsArray() {
        return new ArrayList<>(Arrays.asList(strings));
    }

    public boolean isValid(String name) {
        for (String val : strings) if (val.equalsIgnoreCase(name)) return true;
        return false;
    }

    public boolean isMode(String value) {
        return getValue().equalsIgnoreCase(value);
    }

    @Override
    public void setValue(String value) {
        if (isValid(value)) for (String val : strings) if (val.equalsIgnoreCase(value)) super.setValue(val);
    }
}
