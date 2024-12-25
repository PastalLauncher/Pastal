package org.pastal.launcher.option;

import lombok.Getter;
import lombok.Setter;

import java.util.function.Supplier;

public class Option<T> {

    @Getter
    private final String name, description;

    @Setter
    @Getter
    public T value;

    @Getter
    private final int height;

    private final Supplier<Boolean> visible;

    public Option(String name, String description, T value, Supplier<Boolean> visible, int height) {
        this.name = name;
        this.description = description;
        this.value = value;
        this.visible = visible;
        this.height = height;
    }

    public Option(String name, String description, Supplier<Boolean> visible, int height) {
        this.name = name;
        this.description = description;
        this.visible = visible;
        this.height = height;
    }

    public boolean isVisible() {
        return visible.get();
    }

    public String getSimplifyName() {
        return name.replace(" ", "").replace("(", "").replace(")", "");
    }

}
