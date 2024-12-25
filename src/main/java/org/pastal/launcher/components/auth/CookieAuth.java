package org.pastal.launcher.components.auth;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.pastal.launcher.api.interfaces.Component;
import org.pastal.launcher.components.network.RequestComponent;
import org.pastal.launcher.managers.ComponentManager;
import org.pastal.launcher.util.IOUtils;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Pattern;

public class CookieAuth implements Component {
    private RequestComponent requestComponent;

    public String[] login(File file) {
        byte[] data;
        try (FileInputStream fis = new FileInputStream(file)) {
            data = IOUtils.toByteArray(fis);
        } catch (FileNotFoundException e) {
            throw new RuntimeException("File not found: " + file.getAbsolutePath(), e);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read file: " + file.getAbsolutePath(), e);
        }
        return login(data);
    }

    public String[] login(byte[] data) {
        String[] cookieString;
        try (Scanner scanner = new Scanner(new ByteArrayInputStream(data))) {
            List<String> lines = new ArrayList<>();
            while (scanner.hasNextLine()) {
                lines.add(scanner.nextLine());
            }
            cookieString = lines.toArray(new String[0]);
        } catch (Exception e) {
            throw new RuntimeException("Failed to process token", e);
        }

        try {
            return doCookieLogin(cookieString);
        } catch (Exception e) {
            throw new RuntimeException("Token is invalid", e);
        }
    }


    private String[] doCookieLogin(String[] cookieString) throws Exception {
        StringBuilder cookies = new StringBuilder();
        List<String> uniqueCookies = new ArrayList<>();
        for (String cookie : cookieString) {
            String[] parts = cookie.split("\t");
            if (parts[0].endsWith("login.live.com") && !uniqueCookies.contains(parts[5])) {
                cookies.append(parts[5]).append("=").append(parts[6]).append("; ");
                uniqueCookies.add(parts[5]);
            }
        }
        if (cookies.length() > 0) {
            cookies = new StringBuilder(cookies.substring(0, cookies.length() - 2));
        }

        String url = "https://sisu.xboxlive.com/connect/XboxLive/?state=login&cobrandId=8058f65d-ce06-4c30-9559-473c9275a65d&tid=896928775&ru=https%3A%2F%2Fwww.minecraft.net%2Fen-us%2Flogin&aid=1142970254";
        Map<String, String> headers = requestComponent.createStandardHeaders();
        headers.put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7");
        headers.put("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/112.0.0.0 Safari/537.36");
        headers.put("Accept-Encoding", "niggas");
        headers.put("Accept-Language", "en-US;q=0.8");

        String location = requestComponent.getAsHeader(url, headers, "Location");

        headers.put("Cookie", cookies.toString());
        location = requestComponent.getAsHeader(location, headers, "Location");

        String location3 = requestComponent.getAsHeader(location, headers, "Location");

        String accessToken = location3.split("accessToken=")[1];
        String decoded = new String(Base64.getDecoder().decode(accessToken), StandardCharsets.UTF_8).split("\"rp://api.minecraftservices.com/\",")[1];
        String token = decoded.split("\"Token\":\"")[1].split("\"")[0];
        String uhs = decoded.split(Pattern.quote("{\"DisplayClaims\":{\"xui\":[{\"uhs\":\""))[1].split("\"")[0];
        String xbl = "XBL3.0 x=" + uhs + ";" + token;

        headers = requestComponent.createStandardHeaders();
        headers.put("Accept", "application/json");
        headers.put("Content-Type", "application/json");
        String body = "{\"identityToken\":\"" + xbl + "\",\"ensureLegacyEnabled\":true}";

        JsonObject authResponse = JsonParser.parseString(
                requestComponent.postJson("https://api.minecraftservices.com/authentication/login_with_xbox", headers, body)
        ).getAsJsonObject();

        headers = requestComponent.createStandardHeaders();
        headers.put("Authorization", "Bearer " + authResponse.get("access_token").getAsString());

        JsonObject object = JsonParser.parseString(requestComponent.get("https://api.minecraftservices.com/minecraft/profile", headers)).getAsJsonObject();

        return new String[]{object.get("name").getAsString(), object.get("id").getAsString(), authResponse.get("access_token").getAsString()};
    }


    @Override
    public void setup(ComponentManager componentManager) {
        this.requestComponent = componentManager.get(RequestComponent.class);
    }
}
