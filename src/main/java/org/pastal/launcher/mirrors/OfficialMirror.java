package org.pastal.launcher.mirrors;

import org.pastal.launcher.api.interfaces.Mirror;

public class OfficialMirror implements Mirror {
    @Override
    public String getMirrorUrl(String input) {
        return input;
    }
}
