package org.pastal.launcher.option;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import lombok.Getter;
import lombok.Setter;
import org.pastal.launcher.http.implement.api.Join;

import java.util.function.Supplier;

public abstract class Option<T> {

    @Getter
    private final String name, description;

    private T value;


    private final Supplier<Boolean> visible;

    public Option(String name, String description, T value, Supplier<Boolean> visible) {
        this.name = name;
        this.description = description;
        this.value = value;
        this.visible = visible;
    }

    public Option(String name, String description, Supplier<Boolean> visible) {
        this.name = name;
        this.description = description;
        this.visible = visible;
    }

    public boolean isVisible() {
        return visible.get();
    }

    public T get() {
        return value;
    }

    public void set(T value) {
        this.value = value;
    }

    public abstract JsonElement asJson();
    public abstract void readJson(JsonElement object);
}
