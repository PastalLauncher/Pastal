package org.pastal.launcher.http.implement;

import org.pastal.launcher.Launcher;
import org.pastal.launcher.api.objects.account.RefreshTokenAccount;
import org.pastal.launcher.components.auth.MicrosoftLogin;
import org.pastal.launcher.components.auth.TokenAuth;
import org.pastal.launcher.enums.ClientIdentification;
import org.pastal.launcher.http.annotations.RequestMapping;
import org.pastal.launcher.http.annotations.RequestParameter;
import org.pastal.launcher.http.annotations.RestController;
import org.pastal.launcher.util.Multithreading;

import java.io.IOException;

@RestController
public class Auth {
    @RequestMapping(path = "/auth")
    public String auth(@RequestParameter("code") String code) {
        Multithreading.run(() -> {
            TokenAuth tokenAuth = Launcher.getInstance().getComponentManager().get(TokenAuth.class);
            MicrosoftLogin microsoftLogin = Launcher.getInstance().getComponentManager().get(MicrosoftLogin.class);
            try {
                String token = tokenAuth.getToken(code,microsoftLogin.getRedirectUrl());
                RefreshTokenAccount refreshTokenAccount = new RefreshTokenAccount(token, ClientIdentification.STYLES, true);
                Launcher.getInstance().getAccountManager().registerValue(refreshTokenAccount);
            } catch (IOException| IllegalArgumentException e) {
                Launcher.getInstance().getExceptionHandler().accept(e);
            }
        });
        return code;
    }
}
