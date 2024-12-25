package org.pastal.launcher.api;

import org.pastal.launcher.Launcher;
import org.pastal.launcher.event.events.BeanRegisterEvent;
import org.pastal.launcher.event.events.BeanRemovedEvent;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public abstract class ValueManager<T> {
    protected final List<T> values = new ArrayList<>();

    public void registerValue(final T value) {
        values.add(value);
        Launcher.getInstance().getEventManager().call(new BeanRegisterEvent(value));
    }

    public void removeValue(final T value) {
        values.remove(value);
        Launcher.getInstance().getEventManager().call(new BeanRemovedEvent(value));
    }

    public Collection<T> getValues() {
        return values;
    }
}
