package org.pastal.launcher.managers;

import org.pastal.launcher.addons.Fabric;
import org.pastal.launcher.addons.Forge;
import org.pastal.launcher.addons.Optifine;
import org.pastal.launcher.api.ImmutableManager;
import org.pastal.launcher.api.interfaces.Addon;

public class AddonManager extends ImmutableManager<Addon> {
    public AddonManager() {
        registerElements(Forge.class, Optifine.class, Fabric.class);
    }
}
