package org.pastal.launcher.option.implement;

import java.util.function.Supplier;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import lombok.Getter;
import lombok.Setter;
import org.pastal.launcher.option.Option;

import java.util.ArrayList;
import java.util.Arrays;

public class ComboOption extends Option<String> {

    @Getter
    @Setter
    private String[] strings;

    public ComboOption(String name, String description, Supplier<Boolean> visible, String... values) {
        super(name, description, values[0], visible);
        this.strings = values;
    }

    public ComboOption(String name, String description, String... values) {
        super(name, description, values[0], () -> true);
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
        return get().equalsIgnoreCase(value);
    }

    @Override
    public void set(String value) {
        if (isValid(value)) for (String val : strings) if (val.equalsIgnoreCase(value)) super.set(val);
    }

    @Override
    public JsonElement asJson() {
        JsonObject json = new JsonObject();
        json.addProperty("value",get());
        JsonArray array = new JsonArray();
        for (String s : getAsArray()) {
            array.add(s);
        }
        json.add("options",array);
        return json;
    }

    @Override
    public void readJson(JsonElement object) {
        if(object.isJsonObject()){
            JsonObject json = object.getAsJsonObject();
            if(json.has("value")){
                set(json.get("value").getAsString());
            }
        }
    }
}
