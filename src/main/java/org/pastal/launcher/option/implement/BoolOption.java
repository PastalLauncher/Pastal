package org.pastal.launcher.option.implement;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import org.pastal.launcher.option.Option;

import java.util.function.Supplier;

public class BoolOption extends Option<Boolean> {

    public BoolOption(String name, String description, Boolean value, Supplier<Boolean> visible) {
        super(name, description, value, visible);
    }

    public BoolOption(String name, String description, Boolean value) {
        super(name, description, value, () -> true);
    }

    @Override
    public JsonElement asJson() {
        return new JsonPrimitive(get());
    }

    @Override
    public void readJson(JsonElement object) {
        if(object.isJsonPrimitive()){
            JsonPrimitive primitive = object.getAsJsonPrimitive();
            set(primitive.isBoolean() ? primitive.getAsBoolean() : primitive.getAsString().equalsIgnoreCase("true"));
        }
    }
}
