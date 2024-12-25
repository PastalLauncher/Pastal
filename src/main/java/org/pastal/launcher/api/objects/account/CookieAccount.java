package org.pastal.launcher.api.objects.account;

import com.google.gson.JsonObject;
import lombok.Getter;
import lombok.Setter;
import org.pastal.launcher.Launcher;
import org.pastal.launcher.api.interfaces.PremiumAccount;
import org.pastal.launcher.components.auth.CookieAuth;
import org.pastal.launcher.components.network.MojangAPI;
import org.pastal.launcher.event.events.AuthFailedEvent;
import org.pastal.launcher.util.IOUtils;
import org.pastal.launcher.util.UUIDTypeUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

@Getter
public class CookieAccount implements PremiumAccount {
    @Setter
    private String username;
    private String accessToken;
    private byte[] cookie;
    @Setter
    private UUID UUID;
    private long lastCheck;

    public CookieAccount(final File cookie, final boolean fetchProfile) throws IOException {
        this.cookie = IOUtils.toByteArray(Files.newInputStream(cookie.toPath()));
        if (fetchProfile) {
            reloadProfile();
        }
    }

    public CookieAccount(final byte[] cookie) {
        this.cookie = cookie;

        reloadProfile();
    }

    public CookieAccount() {
    }

    public void reloadProfile() {
        try {
            CookieAuth cookieAuth = Launcher.getInstance().getComponentManager().get(CookieAuth.class);
            String[] sessionData = cookieAuth.login(cookie);
            this.username = sessionData[0];
            this.UUID = UUIDTypeUtils.fromString(sessionData[1]);
            this.accessToken = sessionData[2];
        } catch (Exception e) {
            Launcher.getInstance().getEventManager().call(new AuthFailedEvent(this));
            throw new RuntimeException("Token is invalid");
        }
    }

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
        return accessToken;
    }

    @Override
    public String getTypeName() {
        return "Cookie";
    }

    @Override
    public JsonObject asJson() {
        final JsonObject object = new JsonObject();
        object.addProperty("type", getTypeName());
        object.addProperty("name", getUsername());
        object.addProperty("cookie", Base64.getEncoder().encodeToString(cookie));
        object.addProperty("access-token", accessToken);
        object.addProperty("uuid", getUUID().toString());
        return object;
    }

    @Override
    public void fromJson(JsonObject json) {
        this.username = json.get("name").getAsString();
        this.cookie = Base64.getDecoder().decode(json.get("cookie").getAsString());
        this.accessToken = json.get("access-token").getAsString();
        this.UUID = java.util.UUID.fromString(json.get("uuid").getAsString());
    }
}
