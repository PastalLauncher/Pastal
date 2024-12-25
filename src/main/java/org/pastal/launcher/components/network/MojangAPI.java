package org.pastal.launcher.components.network;

import com.google.gson.JsonObject;
import org.pastal.launcher.api.interfaces.Component;
import org.pastal.launcher.managers.ComponentManager;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;

public class MojangAPI implements Component {
    private RequestComponent requestComponent;

    @Override
    public void setup(ComponentManager componentManager) {
        this.requestComponent = componentManager.get(RequestComponent.class);
    }

    public Optional<JsonObject> getProfile(String accessToken) {
        Map<String, String> headers = requestComponent.createStandardHeaders();
        headers.put("Authorization", "Bearer " + accessToken);
        try {
            JsonObject object = requestComponent.getAsJsonObject("https://api.minecraftservices.com/minecraft/profile", headers);
            return Optional.of(object);
        } catch (IOException e) {
            return Optional.empty();
        }
    }
}
