package org.pastal.launcher.event.events;

import lombok.Getter;
import lombok.Setter;
import org.pastal.launcher.api.interfaces.PremiumAccount;
import org.pastal.launcher.event.impl.Event;

@Getter
@Setter
public class TokenRequestEvent implements Event {
    private PremiumAccount account;
    private boolean approved;
}
