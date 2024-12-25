package org.pastal.launcher.api.objects.account;

import com.google.gson.JsonObject;
import org.pastal.launcher.api.interfaces.Account;

import java.util.UUID;

public class OfflineAccount implements Account {
    private String name;
    private UUID uuid;

    public OfflineAccount(String name) {
        this.name = name;
        this.uuid = UUID.randomUUID();
    }

    @Override
    public String getTypeName() {
        return "Offline";
    }

    @Override
    public String getUsername() {
        return name;
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
        object.addProperty("uuid", getUUID().toString());
        return object;
    }

    @Override
    public void fromJson(final JsonObject json) {
        this.name = json.get("name").getAsString();
        this.uuid = UUID.fromString(json.get("uuid").getAsString());
    }
}
