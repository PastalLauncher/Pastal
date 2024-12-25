package org.pastal.launcher.api.interfaces;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.SneakyThrows;
import org.pastal.launcher.Launcher;
import org.pastal.launcher.api.objects.HypixelData;
import org.pastal.launcher.components.network.RequestComponent;
import org.pastal.launcher.util.IOUtils;
import org.pastal.launcher.util.UUIDTypeUtils;
import org.tinylog.Logger;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

public interface PremiumAccount extends Account {
    String getAccessToken();

    void setUsername(String username);

    void reloadProfile();

    default void reload() {
        File assetsPath = new File(Launcher.getInstance().getRunningDirectory(), "assets");
        File avatarPath = new File(assetsPath, getUUID().toString() + ".png");

        if (avatarPath.exists()) {
            try {
                Files.delete(avatarPath.toPath());
            } catch (IOException e) {
                Logger.error(e);
            }
        }

        reloadProfile();
        Launcher.getInstance().getAccountManager().saveConfig();
    }

    default String getNameMCProfile() {
        return "https://namemc.com/profile/" + getUsername();
    }

    default String getPlancke() {
        return "https://plancke.io/hypixel/player/stats/" + getUsername();
    }

    default String getSkyCrypt() {
        return "https://sky.shiiyu.moe/stats/" + getUsername();
    }

    default HypixelData getHypixelData() throws IOException {
        RequestComponent requestComponent = Launcher.getInstance().getComponentManager().get(RequestComponent.class);
        String data = requestComponent.get("https://api.cubk.us/hypixel/player?player=" + getUsername(), requestComponent.createStandardHeaders());
        JsonObject object = JsonParser.parseString(data).getAsJsonObject();
        if (object.get("type").getAsString().equals("ok")) {
            class Helper {
                void addStats(Map<String, String> target, JsonObject source, Map<String, String> mappings) {
                    mappings.forEach((displayName, jsonKey) -> {
                        JsonElement element = source.get(jsonKey);
                        if (element != null && !element.isJsonNull()) {
                            String value = element.isJsonPrimitive() && element.getAsJsonPrimitive().isNumber()
                                    ? formatFloat(element.getAsFloat())
                                    : element.getAsString();
                            target.put(displayName, value);
                        }
                    });
                }

                String formatFloat(float value) {
                    return String.valueOf(Math.round(value));
                }
            }

            JsonObject dataObj = object.getAsJsonObject("data");
            HypixelData hypixelData = new HypixelData();

            Helper helper = new Helper();
            JsonObject player = dataObj.getAsJsonObject("player");
            Map<String, String> playerMapping = new LinkedHashMap<>();
            playerMapping.put("Level", "level");
            playerMapping.put("Rank", "rank");
            playerMapping.put("Guild", "guild");
            helper.addStats(hypixelData.getPlayerInfo(), player, playerMapping);

            JsonObject arcade = dataObj.getAsJsonObject("arcade");
            hypixelData.getPlayerInfo().put("Arcade coins", helper.formatFloat(arcade.get("coins").getAsFloat()));

            JsonObject swData = dataObj.getAsJsonObject("skywars");
            Map<String, String> swMapping = new LinkedHashMap<>();
            swMapping.put("Level", "level");
            swMapping.put("Kills", "kills");
            swMapping.put("K/D", "kdr");
            swMapping.put("W/L", "wlr");
            swMapping.put("Wins", "wins");
            helper.addStats(hypixelData.getSkywars(), swData, swMapping);

            JsonObject bwData = dataObj.getAsJsonObject("bedwars");
            JsonObject bwStats = bwData.getAsJsonObject("stats").getAsJsonObject("overall");
            Map<String, String> bwMapping = new LinkedHashMap<>();
            bwMapping.put("Level", "level");
            bwMapping.put("WS", "winstreak");
            helper.addStats(hypixelData.getBedwars(), bwData, bwMapping);

            Map<String, String> bwStatsMapping = new LinkedHashMap<>();
            bwStatsMapping.put("FK/D", "fkdr");
            bwStatsMapping.put("W/L", "wlr");
            bwStatsMapping.put("Wins", "wins");
            helper.addStats(hypixelData.getBedwars(), bwStats, bwStatsMapping);

            JsonObject duelData = dataObj.getAsJsonObject("duels");
            Map<String, String> duelMapping = new LinkedHashMap<>();
            duelMapping.put("Kills", "kills");
            duelMapping.put("K/D", "kdr");
            duelMapping.put("W/L", "wlr");
            duelMapping.put("Wins", "wins");
            helper.addStats(hypixelData.getDuels(), duelData, duelMapping);

            JsonObject sgData = dataObj.getAsJsonObject("bsg");
            Map<String, String> sgMapping = new LinkedHashMap<>();
            sgMapping.put("Kills", "kills");
            sgMapping.put("Deaths", "deaths");
            sgMapping.put("K/D", "kdr");
            helper.addStats(hypixelData.getBsg(), sgData, sgMapping);

            JsonObject mwData = dataObj.getAsJsonObject("megawalls");
            Map<String, String> mwMapping = new LinkedHashMap<>();
            mwMapping.put("Kills", "kills");
            mwMapping.put("Deaths", "deaths");
            mwMapping.put("Wins", "wins");
            mwMapping.put("K/D", "kdr");
            mwMapping.put("FK/D", "fkdr");
            mwMapping.put("W/L", "wlr");
            helper.addStats(hypixelData.getMegawalls(), mwData, mwMapping);

            JsonObject uhcData = dataObj.getAsJsonObject("uhc");
            Map<String, String> uhcMapping = new LinkedHashMap<>();
            uhcMapping.put("Coins", "coins");
            helper.addStats(hypixelData.getUhc(), uhcData, uhcMapping);

            return hypixelData;
        }
        return null;
    }

    default void rename(String newName) {
        RequestComponent requestComponent = Launcher.getInstance().getComponentManager().get(RequestComponent.class);
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("Authorization", "Bearer " + getAccessToken());
        try {
            requestComponent.put("https://api.minecraftservices.com/minecraft/profile/name/" + newName, headers, "");
            this.setUsername(newName);
            Launcher.getInstance().getAccountManager().saveConfig();
        } catch (IOException e) {
            if (e.getMessage().contains("FORBIDDEN")) {
                throw new RuntimeException("You must wait 30 days before change username again.");
            }
            if (e.getMessage().contains("DUPLICATE")) {
                throw new RuntimeException("This username is already taken.");
            }
            if (e.getMessage().contains("NOT_ALLOWED")) {
                throw new RuntimeException("This username is not allowed.");
            }
            if (e.getMessage().contains("400")) {
                throw new RuntimeException("Invalid username format.");
            }
            throw new RuntimeException(e);
        }
    }

    @SneakyThrows
    default InputStream getAvatar() {
        File assetsPath = new File(Launcher.getInstance().getRunningDirectory(), "assets");
        File avatarPath = new File(assetsPath, getUUID().toString() + ".png");
        if (!avatarPath.exists()) {
            if (!assetsPath.exists()) {
                assetsPath.mkdirs();
            }
            // fuck mojang and fuck microsoft
            String profileUrl = "https://sessionserver.mojang.com/session/minecraft/profile/" + UUIDTypeUtils.fromUUID(getUUID());
            RequestComponent requestComponent = Launcher.getInstance().getComponentManager().get(RequestComponent.class);
            String jsonResponse = requestComponent.get(profileUrl, requestComponent.createStandardHeaders());
            JsonObject profileObject = JsonParser.parseString(jsonResponse).getAsJsonObject();
            JsonArray properties = profileObject.getAsJsonArray("properties");
            String base64Value = properties.get(0).getAsJsonObject().get("value").getAsString();
            String decodedValue = new String(Base64.getDecoder().decode(base64Value));
            JsonObject textureObject = JsonParser.parseString(decodedValue).getAsJsonObject().getAsJsonObject("textures");
            String skinUrl = textureObject.getAsJsonObject("SKIN").get("url").getAsString();
            BufferedImage bufferedImage = ImageIO.read(new URL(skinUrl));
            IOUtils.stripeHeadToFile(bufferedImage, avatarPath);
        }
        return Files.newInputStream(avatarPath.toPath());
    }
}
