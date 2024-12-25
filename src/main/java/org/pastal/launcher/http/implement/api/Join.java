package org.pastal.launcher.http.implement.api;

import com.google.gson.JsonObject;
import org.pastal.launcher.Launcher;
import org.pastal.launcher.api.interfaces.Account;
import org.pastal.launcher.api.interfaces.PremiumAccount;
import org.pastal.launcher.components.network.RequestComponent;
import org.pastal.launcher.event.events.TokenRequestEvent;
import org.pastal.launcher.http.annotations.RequestMapping;
import org.pastal.launcher.http.annotations.RequestParameter;
import org.pastal.launcher.http.annotations.RestController;
import org.pastal.launcher.util.UUIDTypeUtils;

import java.io.IOException;

@RestController("/api")
public class Join {
    @RequestMapping(path = "/join")
    public String join(@RequestParameter("id") String id){
        TokenRequestEvent tokenRequestEvent = new TokenRequestEvent();

        Launcher.getInstance().getEventManager()
                .call(tokenRequestEvent);

        if(tokenRequestEvent.isApproved()){
            Account account = tokenRequestEvent.getAccount();
            if(account == null){
                account = Launcher.getInstance().getAccountManager().getSelectedAccount();
            }

            if(!(account instanceof PremiumAccount)){
                return "Please select a premium account first!";
            }

            PremiumAccount premiumAccount = (PremiumAccount) account;

            RequestComponent requestComponent = Launcher.getInstance().getComponentManager().get(RequestComponent.class);

            JsonObject request = new JsonObject();
            request.addProperty("accessToken",premiumAccount.getAccessToken());
            request.addProperty("selectedProfile", UUIDTypeUtils.fromUUID(premiumAccount.getUUID()));
            request.addProperty("serverId",id);

            try {
                requestComponent.postJson("https://sessionserver.mojang.com/session/minecraft/join", requestComponent.createStandardHeaders(), request.toString());
                return "OK";
            } catch (IOException e) {
                return "Unable to connect mojang verify endpoint.";
            }
        }else {
            return "Auth request is rejected.";
        }
    }
}
