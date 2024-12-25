package org.pastal.launcher.managers;

import lombok.Getter;
import lombok.Setter;
import org.pastal.launcher.api.ImmutableManager;
import org.pastal.launcher.api.interfaces.Mirror;
import org.pastal.launcher.mirrors.BMCLMirror;
import org.pastal.launcher.mirrors.OfficialMirror;
import org.pastal.launcher.util.Multithreading;
import org.tinylog.Logger;

import java.net.Socket;

@Setter
@Getter
public class MirrorManager extends ImmutableManager<Mirror> {

    private Mirror currentMirror;

    public MirrorManager() {
        registerElements(
                OfficialMirror.class,
                BMCLMirror.class
        );

        Multithreading.run(() -> {
            if (isChina()) {
                currentMirror = get(BMCLMirror.class);
            } else {
                currentMirror = get(OfficialMirror.class);
            }
            Logger.info("Mirror: " + currentMirror.getClass().getName());
        });
    }

    public boolean isChina() {
        final String[] dnsServers = {"223.5.5.5", "223.6.6.6"};
        for (final String dns : dnsServers) {
            try (Socket ignored = new Socket(dns, 53)) {
                return true;
            } catch (Exception ignored) {
            }
        }
        return false;
    }
}
