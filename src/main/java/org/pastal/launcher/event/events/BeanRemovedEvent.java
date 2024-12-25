package org.pastal.launcher.event.events;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.pastal.launcher.event.impl.Event;

@AllArgsConstructor
@Getter
public class BeanRemovedEvent implements Event {
    private final Object value;
}