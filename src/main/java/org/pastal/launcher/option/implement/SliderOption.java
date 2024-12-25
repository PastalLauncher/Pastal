package org.pastal.launcher.option.implement;

import java.util.function.Supplier;
import lombok.Getter;
import org.pastal.launcher.option.Option;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class SliderOption extends Option<Double> {

    @Getter
    private final Double min, max, increment;

    @Getter
    private final String suffix;

    public SliderOption(String name, String description, double value, double min, double max, double increment, Supplier<Boolean> visible) {
        super(name, description, value, visible, 16);
        this.min = min;
        this.max = max;
        this.increment = increment;
        this.suffix = "";
    }

    public SliderOption(String name, String description, double value, double min, double max, double increment) {
        super(name, description, value, () -> true, 16);
        this.min = min;
        this.max = max;
        this.increment = increment;
        this.suffix = "";
    }

    public SliderOption(String name, String description, String suffix, double value, double min, double max, double increment, Supplier<Boolean> visible) {
        super(name, description, value, visible, 16);
        this.min = min;
        this.max = max;
        this.increment = increment;
        this.suffix = suffix;
    }

    public SliderOption(String name, String description, String suffix, double value, double min, double max, double increment) {
        super(name, description, value, () -> true, 16);
        this.min = min;
        this.max = max;
        this.increment = increment;
        this.suffix = suffix;
    }

    @Override
    public void setValue(Double value) {
        final double clamped = new BigDecimal(Math.round(value / increment) * increment).setScale(2, RoundingMode.HALF_UP).doubleValue();
        double newValue = Math.min(Math.max(clamped, min), max);
        super.setValue(newValue);
    }
}
