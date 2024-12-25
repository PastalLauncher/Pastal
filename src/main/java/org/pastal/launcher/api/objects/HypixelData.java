package org.pastal.launcher.api.objects;

import lombok.Getter;

import java.util.LinkedHashMap;
import java.util.Map;

@Getter
public class HypixelData {
    private final Map<String, String> playerInfo = new LinkedHashMap<>();
    private final Map<String, String> bedwars = new LinkedHashMap<>();
    private final Map<String, String> skywars = new LinkedHashMap<>();
    private final Map<String, String> duels = new LinkedHashMap<>();
    private final Map<String, String> bsg = new LinkedHashMap<>();
    private final Map<String, String> megawalls = new LinkedHashMap<>();
    private final Map<String, String> uhc = new LinkedHashMap<>();
}
