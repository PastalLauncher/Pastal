package org.pastal.launcher.components.auth;

import lombok.SneakyThrows;
import org.pastal.launcher.Launcher;
import org.pastal.launcher.api.interfaces.Component;
import org.pastal.launcher.api.objects.account.RefreshTokenAccount;
import org.pastal.launcher.enums.ClientIdentification;
import org.pastal.launcher.event.annotations.EventTarget;
import org.pastal.launcher.event.events.RequestEvent;
import org.pastal.launcher.managers.ComponentManager;

public class MicrosoftLogin implements Component {
    private final String clientId = ClientIdentification.STYLES.getClientId();
    private final String redirectUri = "http://localhost:41275/auth";
    private final String scope = ClientIdentification.STYLES.getScope();

    private final String url = "https://login.live.com/oauth20_authorize.srf?client_id=<client_id>&redirect_uri=<redirect_uri>&response_type=code&display=touch&scope=<scope>&prompt=select_account"
            .replace("<client_id>", clientId)
            .replace("<redirect_uri>", redirectUri)
            .replace("<scope>", scope);

    public String getRedirectUrl() {
        return redirectUri;
    }

    public String getAuthUrl() {
        return url;
    }

    @Override
    public void setup(ComponentManager componentManager) {

    }
}
