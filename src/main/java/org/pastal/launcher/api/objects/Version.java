package org.pastal.launcher.api.objects;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.pastal.launcher.Launcher;
import org.pastal.launcher.api.objects.download.ProgressData;

import java.io.IOException;
import java.util.function.Consumer;

@Getter
@AllArgsConstructor
public class Version {
    private String id;
    private String type;
    private String url;
    private String releaseTime;

    public void createInstallation(final Consumer<ProgressData> progressCallback) throws IOException {
        Launcher.getInstance().getInstallationManager().createInstallation(id, id, progressCallback);
    }

    public void createInstallation(final String installationName, final Consumer<ProgressData> progressCallback) throws IOException {
        Launcher.getInstance().getInstallationManager().createInstallation(id, installationName, progressCallback);
    }
}