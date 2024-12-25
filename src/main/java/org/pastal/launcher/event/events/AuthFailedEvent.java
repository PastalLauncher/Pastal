package org.pastal.launcher.event.events;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.pastal.launcher.api.interfaces.Account;
import org.pastal.launcher.event.impl.Event;

@AllArgsConstructor
@Getter
public class AuthFailedEvent implements Event {
    private final Account account;
}
