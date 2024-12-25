package org.pastal.launcher.components.auth;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.RequiredArgsConstructor;
import org.pastal.launcher.api.interfaces.Component;
import org.pastal.launcher.components.network.RequestComponent;
import org.pastal.launcher.enums.ClientIdentification;
import org.pastal.launcher.managers.ComponentManager;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.StringJoiner;

@RequiredArgsConstructor
public class TokenAuth implements Component {

    private RequestComponent requestComponent;

    public String[] requestToken(String token, ClientIdentification clientIdentification) throws IOException {
        final String microsoftTokenAndRefreshToken = getMicrosoftTokenFromRefreshToken(token, clientIdentification.getScope(), clientIdentification.getClientId());
        final String xBoxLiveToken = xboxTokenAuth(microsoftTokenAndRefreshToken, clientIdentification != ClientIdentification.OFFICIAL);
        final String[] xstsTokenAndHash = xboxUserHash(xBoxLiveToken);
        final String accessToken = getAccessToken(xstsTokenAndHash[1], xstsTokenAndHash[0]);
        final String[] profile = getProfile(accessToken);
        return new String[]{profile[1], profile[0], accessToken};
    }

    public String getToken(String code, String redirectUrl) throws IOException {
        final String url = "https://login.live.com/oauth20_token.srf";
        final Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/x-www-form-urlencoded");

        final Map<String, String> params = new LinkedHashMap<>();
        params.put("client_id", ClientIdentification.STYLES.getClientId());
        params.put("code", code);
        params.put("grant_type", "authorization_code");
        params.put("redirect_uri", redirectUrl);
        params.put("scope", "XboxLive.signin offline_access");

        String param = paramsToUrlEncoded(params);

        String response = requestComponent.postUrlEncoded(url, headers, param);

        JsonObject responseObj = JsonParser.parseString(response).getAsJsonObject();

        return responseObj.get("refresh_token").getAsString();
    }


    private String getMicrosoftTokenFromRefreshToken(String refreshToken, String scope, String clientId) throws IOException {
        final String url = "https://login.live.com/oauth20_token.srf";
        final Map<String, String> headers = new LinkedHashMap<>();
        final Map<String, String> params = new LinkedHashMap<>();
        params.put("client_id", clientId);
        params.put("refresh_token", refreshToken);
        params.put("grant_type", "refresh_token");
        params.put("redirect_uri", "https://login.live.com/oauth20_desktop.srf");
        params.put("scope", scope);
        String body = paramsToUrlEncoded(params);
        String response = requestComponent.postUrlEncoded(url, headers, body);
        JsonObject resp = JsonParser.parseString(response).getAsJsonObject();
        return resp.get("access_token").getAsString();
    }

    private String xboxTokenAuth(String authToken, boolean direct) throws IOException {
        final String url = "https://user.auth.xboxlive.com/user/authenticate";
        final Map<String, String> headers = requestComponent.createStandardHeaders();
        headers.put("Content-Type", "application/json");
        headers.put("Accept", "application/json");

        JsonObject requestProps = new JsonObject();
        requestProps.addProperty("AuthMethod", "RPS");
        requestProps.addProperty("SiteName", "user.auth.xboxlive.com");
        requestProps.addProperty("RpsTicket", (direct ? "d=" : "") + authToken);

        JsonObject request = new JsonObject();
        request.add("Properties", requestProps);
        request.addProperty("RelyingParty", "http://auth.xboxlive.com");
        request.addProperty("TokenType", "JWT");

        String response = requestComponent.postUrlEncoded(url, headers, request.toString());
        JsonObject resp = JsonParser.parseString(response).getAsJsonObject();
        return resp.get("Token").getAsString();
    }

    private String[] xboxUserHash(String xboxToken) throws IOException {
        final String url = "https://xsts.auth.xboxlive.com/xsts/authorize";
        final Map<String, String> headers = requestComponent.createStandardHeaders();
        headers.put("Content-Type", "application/json");
        headers.put("Accept", "application/json");

        JsonObject requestProps = new JsonObject();
        JsonArray userTokens = new JsonArray();
        userTokens.add(xboxToken);
        requestProps.add("UserTokens", userTokens);
        requestProps.addProperty("SandboxId", "RETAIL");

        JsonObject request = new JsonObject();
        request.add("Properties", requestProps);
        request.addProperty("RelyingParty", "rp://api.minecraftservices.com/");
        request.addProperty("TokenType", "JWT");

        String response = requestComponent.postUrlEncoded(url, headers, request.toString());
        JsonObject resp = JsonParser.parseString(response).getAsJsonObject();

        String token = resp.get("Token").getAsString();
        String userHash = resp.getAsJsonObject("DisplayClaims").getAsJsonArray("xui").get(0).getAsJsonObject().get("uhs").getAsString();
        return new String[]{token, userHash};
    }

    private String getAccessToken(String userHash, String xstsToken) throws IOException {
        final String url = "https://api.minecraftservices.com/authentication/login_with_xbox";
        final Map<String, String> headers = requestComponent.createStandardHeaders();
        headers.put("Accept", "application/json");
        JsonObject request = new JsonObject();
        request.addProperty("identityToken", "XBL3.0 x=" + userHash + ";" + xstsToken);

        String response = requestComponent.postJson(url, headers, request.toString());
        JsonObject resp = JsonParser.parseString(response).getAsJsonObject();
        return resp.get("access_token").getAsString();
    }

    private String[] getProfile(String accessToken) throws IOException {
        final String url = "https://api.minecraftservices.com/minecraft/profile";
        final Map<String, String> headers = requestComponent.createStandardHeaders();
        headers.put("Authorization", "Bearer " + accessToken);

        String response = requestComponent.get(url, headers);
        JsonObject resp = JsonParser.parseString(response).getAsJsonObject();
        return new String[]{resp.get("id").getAsString(), resp.get("name").getAsString()};
    }

    private String paramsToUrlEncoded(Map<String, String> params) {
        StringJoiner sj = new StringJoiner("&");

        for (Map.Entry<String, String> entry : params.entrySet()) {
            try {
                sj.add(URLEncoder.encode(entry.getKey(), "UTF-8") + "=" + URLEncoder.encode(entry.getValue(), "UTF-8"));
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }

        return sj.toString();
    }

    @Override
    public void setup(ComponentManager componentManager) {
        requestComponent = componentManager.get(RequestComponent.class);
    }
}
