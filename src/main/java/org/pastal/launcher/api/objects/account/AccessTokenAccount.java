package org.pastal.launcher.api.objects.account;

import com.google.gson.JsonObject;
import lombok.Getter;
import lombok.Setter;
import org.pastal.launcher.Launcher;
import org.pastal.launcher.api.interfaces.PremiumAccount;
import org.pastal.launcher.components.network.MojangAPI;
import org.pastal.launcher.event.events.AuthFailedEvent;
import org.pastal.launcher.util.UUIDTypeUtils;

import java.util.Optional;
import java.util.UUID;

public class AccessTokenAccount implements PremiumAccount {
    @Setter
    @Getter
    private String username;
    private String accessToken;
    @Setter
    private UUID uuid;
    private long lastCheck;

    public AccessTokenAccount() {
    }

    public AccessTokenAccount(final String accessToken) {
        this.accessToken = accessToken;
        reloadProfile();
    }

    public void reloadProfile() {
        Optional<JsonObject> object = Launcher.getInstance().getComponentManager().get(MojangAPI.class).getProfile(accessToken);
        if (object.isPresent()) {
            username = object.get().get("name").getAsString();
            uuid = UUIDTypeUtils.fromString(object.get().get("id").getAsString());
        } else {
            throw new RuntimeException("Token is invalid");
        }
    }

    @Override
    public String getTypeName() {
        return "Access Token";
    }


    @Override
    public String getAccessToken() {
        if (System.currentTimeMillis() - lastCheck > 1000 * 60 * 60) {
            Optional<JsonObject> object = Launcher.getInstance().getComponentManager().get(MojangAPI.class).getProfile(accessToken);
            if (object.isPresent()) {
                lastCheck = System.currentTimeMillis();
                return this.accessToken;
            } else {
                Launcher.getInstance().getEventManager().call(new AuthFailedEvent(this));
                throw new RuntimeException("Token is invalid");
            }
        }
        return this.accessToken;
    }

    @Override
    public UUID getUUID() {
        return this.uuid;
    }

    @Override
    public JsonObject asJson() {
        final JsonObject object = new JsonObject();
        object.addProperty("type", getTypeName());
        object.addProperty("name", getUsername());
        object.addProperty("access-token", accessToken);
        object.addProperty("uuid", getUUID().toString());
        return object;
    }

    @Override
    public void fromJson(final JsonObject json) {
        this.username = json.get("name").getAsString();
        this.accessToken = json.get("access-token").getAsString();
        this.uuid = UUID.fromString(json.get("uuid").getAsString());
    }
}