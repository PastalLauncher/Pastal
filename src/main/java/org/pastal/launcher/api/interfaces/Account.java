package org.pastal.launcher.api.interfaces;

import com.google.gson.JsonObject;
import org.pastal.launcher.Launcher;

import java.io.InputStream;
import java.util.UUID;

public interface Account {
    String getTypeName();

    String getUsername();

    UUID getUUID();

    default InputStream getAvatar() {
        return Launcher.class.getResourceAsStream("/steve.png");
    }

    JsonObject asJson();

    void fromJson(final JsonObject json);
}
