package org.pastal.launcher.option.implement;

import java.util.function.Supplier;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import lombok.Getter;
import lombok.Setter;
import org.pastal.launcher.option.Option;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class SliderOption extends Option<Double> {

    @Getter@Setter
    private Double min, max, increment;

    @Getter
    private final String suffix;

    public SliderOption(String name, String description, double value, double min, double max, double increment, Supplier<Boolean> visible) {
        super(name, description, value, visible);
        this.min = min;
        this.max = max;
        this.increment = increment;
        this.suffix = "";
    }

    public SliderOption(String name, String description, double value, double min, double max, double increment) {
        super(name, description, value, () -> true);
        this.min = min;
        this.max = max;
        this.increment = increment;
        this.suffix = "";
    }

    public SliderOption(String name, String description, String suffix, double value, double min, double max, double increment, Supplier<Boolean> visible) {
        super(name, description, value, visible);
        this.min = min;
        this.max = max;
        this.increment = increment;
        this.suffix = suffix;
    }

    public SliderOption(String name, String description, String suffix, double value, double min, double max, double increment) {
        super(name, description, value, () -> true);
        this.min = min;
        this.max = max;
        this.increment = increment;
        this.suffix = suffix;
    }

    @Override
    public void set(Double value) {
        final double clamped = new BigDecimal(Math.round(value / increment) * increment).setScale(2, RoundingMode.HALF_UP).doubleValue();
        double newValue = Math.min(Math.max(clamped, min), max);
        super.set(newValue);
    }

    @Override
    public JsonElement asJson() {
        JsonObject object = new JsonObject();
        object.addProperty("value",get());
        object.addProperty("minimum",min);
        object.addProperty("maximum",max);
        object.addProperty("increment",increment);
        object.addProperty("suffix",suffix);
        return object;
    }

    @Override
    public void readJson(JsonElement object) {
        if(object.isJsonObject()){
            JsonObject json = object.getAsJsonObject();
            if(json.has("value")){
                set(json.get("value").getAsDouble());
            }
        }
    }
}
