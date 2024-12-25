package org.pastal.launcher.api.objects;

import lombok.Getter;

import java.io.File;

@Getter
public class JavaInstallation {
    private final String executable;
    private final String version;
    private final int majorVersion;

    public JavaInstallation(final File executable, final String version, final int majorVersion) {
        this.executable = executable.getAbsolutePath();
        this.version = version;
        this.majorVersion = majorVersion;
    }

    public String getPath() {
        return executable;
    }
}