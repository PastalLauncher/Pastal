package org.pastal.launcher.api.interfaces;

import org.pastal.launcher.api.objects.AddonVersion;
import org.pastal.launcher.api.objects.Installation;
import org.pastal.launcher.api.objects.download.ProgressData;

import java.io.IOException;
import java.util.List;
import java.util.function.Consumer;

public interface Addon {
    String getName();

    void setup(final Installation installation, final AddonVersion version, final Consumer<ProgressData> progressCallback, final Consumer<String> logCallback) throws IOException;

    List<AddonVersion> getVersions(final Installation installation) throws IOException;
}
