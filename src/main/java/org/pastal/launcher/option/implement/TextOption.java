package org.pastal.launcher.option.implement;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import lombok.Getter;
import lombok.Setter;
import org.pastal.launcher.option.Option;

import java.util.function.Supplier;

public class TextOption extends Option<String>{

    public TextOption(String name, String description, String value, Supplier<Boolean> visible) {
        super(name, description, value, visible);
    }

    public TextOption(String name, String description, String value) {
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
            set(primitive.getAsString());
        }
    }


}
