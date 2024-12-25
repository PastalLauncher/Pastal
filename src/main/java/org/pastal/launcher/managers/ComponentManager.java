package org.pastal.launcher.managers;

import org.pastal.launcher.Launcher;
import org.pastal.launcher.api.ImmutableManager;
import org.pastal.launcher.api.interfaces.Component;
import org.pastal.launcher.components.auth.CookieAuth;
import org.pastal.launcher.components.auth.MicrosoftLogin;
import org.pastal.launcher.components.auth.TokenAuth;
import org.pastal.launcher.components.file.FileListenerComponent;
import org.pastal.launcher.components.network.DownloadEngine;
import org.pastal.launcher.components.network.LocalServer;
import org.pastal.launcher.components.network.MojangAPI;
import org.pastal.launcher.components.network.RequestComponent;

public class ComponentManager extends ImmutableManager<Component> {
    public ComponentManager() {
        registerElements(
                RequestComponent.class,
                DownloadEngine.class,
                TokenAuth.class,
                MojangAPI.class,
                CookieAuth.class,
                LocalServer.class,
                MicrosoftLogin.class,
                FileListenerComponent.class
              //  AgentAuth.class
        );
        getElements().forEach(component -> component.setup(this));
        for (Component element : getElements()) {
            Launcher.getInstance().getEventManager().register(element);
        }
    }
}
