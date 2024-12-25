package org.pastal.launcher.option.implement;

import lombok.Getter;
import lombok.Setter;
import org.pastal.launcher.option.Option;

import java.util.function.Supplier;

public class TextOption extends Option<String> {

    @Getter
    @Setter
    private String selectedString;

    public TextOption(String name, String description, String value, Supplier<Boolean> visible) {
        super(name, description, value, visible, 8);
        this.selectedString = "";
    }

    public TextOption(String name, String description, String value) {
        super(name, description, value, () -> true, 8);
        this.selectedString = "";
    }
}
