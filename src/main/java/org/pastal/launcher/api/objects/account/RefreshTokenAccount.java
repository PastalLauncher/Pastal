package org.pastal.launcher.api.objects.account;

import com.google.gson.JsonObject;
import lombok.Getter;
import lombok.Setter;
import org.pastal.launcher.Launcher;
import org.pastal.launcher.api.interfaces.PremiumAccount;
import org.pastal.launcher.components.auth.TokenAuth;
import org.pastal.launcher.components.network.MojangAPI;
import org.pastal.launcher.enums.ClientIdentification;
import org.pastal.launcher.event.events.AuthFailedEvent;
import org.pastal.launcher.util.UUIDTypeUtils;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

public class RefreshTokenAccount implements PremiumAccount {
    @Setter
    @Getter
    private String username;
    @Getter
    private String refreshToken;
    private String accessToken;
    @Getter
    private ClientIdentification clientId;
    private UUID uuid;
    private long lastCheck;

    public RefreshTokenAccount(final String refreshToken, final ClientIdentification clientIdentification, boolean fetchProfile) {
        this.refreshToken = refreshToken;
        this.clientId = clientIdentification;

        if (fetchProfile) {
            reloadProfile();
        }
    }

    public RefreshTokenAccount() {
    }

    public void reloadProfile() {
        try {
            TokenAuth tokenAuth = Launcher.getInstance().getComponentManager().get(TokenAuth.class);
            String[] sessionData = tokenAuth.requestToken(refreshToken, clientId);
            this.username = sessionData[0];
            this.uuid = UUIDTypeUtils.fromString(sessionData[1]);
            this.accessToken = sessionData[2];
        } catch (IOException e) {
            if (e.getMessage().contains("invalid_grant")) {
                Launcher.getInstance().getEventManager().call(new AuthFailedEvent(this));
                throw new RuntimeException("Token is invalid");
            }
            throw new RuntimeException(e);
        }
    }


    @Override
    public String getTypeName() {
        return "Refresh Token";
    }

    @Override
    public String getAccessToken() {
        if (System.currentTimeMillis() - lastCheck > 1000 * 60 * 60) {
            Optional<JsonObject> object = Launcher.getInstance().getComponentManager().get(MojangAPI.class).getProfile(this.accessToken);
            if (object.isPresent()) {
                this.username = object.get().get("name").getAsString();
            } else {
                reloadProfile();
            }
            lastCheck = System.currentTimeMillis();
        }

        return this.accessToken;
    }

    @Override
    public UUID getUUID() {
        return uuid;
    }

    @Override
    public JsonObject asJson() {
        final JsonObject object = new JsonObject();
        object.addProperty("type", getTypeName());
        object.addProperty("name", getUsername());
        object.addProperty("refresh-token", getRefreshToken());
        object.addProperty("client-id", getClientId().name());
        object.addProperty("access-token", accessToken);
        object.addProperty("uuid", getUUID().toString());
        return object;
    }

    @Override
    public void fromJson(final JsonObject json) {
        this.username = json.get("name").getAsString();
        this.refreshToken = json.get("refresh-token").getAsString();
        this.accessToken = json.get("access-token").getAsString();
        this.clientId = ClientIdentification.valueOf(json.get("client-id").getAsString());
        this.uuid = UUID.fromString(json.get("uuid").getAsString());
    }
}
