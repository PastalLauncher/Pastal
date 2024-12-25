package org.pastal.launcher.mirrors;

import org.pastal.launcher.api.interfaces.Mirror;

public class BMCLMirror implements Mirror {

    @Override
    public String getMirrorUrl(String input) {
        input = input.replace("http://launchermeta.mojang.com/", "https://bmclapi2.bangbang93.com/")
                .replace("https://launchermeta.mojang.com/", "https://bmclapi2.bangbang93.com/")
                .replace("https://piston-meta.mojang.com/", "https://bmclapi2.bangbang93.com/")
                .replace("https://launcher.mojang.com/", "https://bmclapi2.bangbang93.com/")
                .replace("https://libraries.minecraft.net/", "https://bmclapi2.bangbang93.com/maven/")
                .replace("https://meta.fabricmc.net", "https://bmclapi2.bangbang93.com/fabric-meta")
                .replace("https://maven.fabricmc.net", "https://bmclapi2.bangbang93.com/maven");
        return input;
    }
}
