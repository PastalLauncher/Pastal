package org.pastal.launcher.api;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

public abstract class ImmutableManager<T> {
    protected final Map<Class<? extends T>, T> elements = new LinkedHashMap<>();

    public ArrayList<T> getElements() {
        return new ArrayList<>(elements.values());
    }

    @SuppressWarnings("unchecked")
    protected final void registerElement(final T value) {
        elements.put((Class<? extends T>) value.getClass(), value);
    }

    @SafeVarargs
    protected final void registerElements(final Class<? extends T>... classes) {
        for (final Class<? extends T> clazz : classes) {
            try {
                elements.put(clazz, clazz.getDeclaredConstructor().newInstance());
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException("Failed to instantiate " + clazz.getName(), e);
            }
        }
    }

    @SuppressWarnings("unchecked")
    public <V extends T> V get(final Class<V> clazz) {
        return (V) elements.get(clazz);
    }
}
