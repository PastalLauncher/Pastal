package org.pastal.launcher.http.implement.api.v1.accounts;

import org.pastal.launcher.Launcher;
import org.pastal.launcher.api.objects.ApiResponse;
import org.pastal.launcher.api.objects.account.AccessTokenAccount;
import org.pastal.launcher.api.objects.account.CookieAccount;
import org.pastal.launcher.api.objects.account.OfflineAccount;
import org.pastal.launcher.api.objects.account.RefreshTokenAccount;
import org.pastal.launcher.api.objects.dto.AccountDTO;
import org.pastal.launcher.components.auth.MicrosoftLogin;
import org.pastal.launcher.enums.ClientIdentification;
import org.pastal.launcher.http.annotations.RequestBody;
import org.pastal.launcher.http.annotations.RequestMapping;
import org.pastal.launcher.http.annotations.RequestParameter;
import org.pastal.launcher.http.annotations.RestController;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@RestController("/api/v1/accounts/create")
public class CreateController {
    @RequestMapping(path = "/client-id")
    public ApiResponse<List<String>> clientId(){
        List<String> strings = new ArrayList<>();
        for (ClientIdentification value : ClientIdentification.values()) {
            strings.add(value.name());
        }
        return ApiResponse.success(strings);
    }

    @RequestMapping(path = "/offline",method = "POST")
    public ApiResponse<AccountDTO> offline(@RequestParameter("username") String username) {
        OfflineAccount offlineAccount = new OfflineAccount(username);
        try {
            Launcher.getInstance().getAccountManager().registerValue(offlineAccount);
            return ApiResponse.success(AccountDTO.convertToDTO(offlineAccount));
        }catch (IllegalArgumentException exception){
            return ApiResponse.failure(exception.getMessage());
        }
    }

    @RequestMapping(path = "/microsoft")
    public ApiResponse<String> microsoft() {
        return ApiResponse.success(Launcher.getInstance().getComponentManager().get(MicrosoftLogin.class).getAuthUrl());
    }

    @RequestMapping(path = "/refresh-token",method = "POST")
    public ApiResponse<AccountDTO> refresh(@RequestParameter("token") String token,@RequestParameter("client-id") String clientId){
        ClientIdentification identification = ClientIdentification.valueOf(clientId);
        try {
            RefreshTokenAccount account = new RefreshTokenAccount(token, identification, true);
            Launcher.getInstance().getAccountManager().registerValue(account);
            return ApiResponse.success(AccountDTO.convertToDTO(account));
        }catch (RuntimeException exception){
            return ApiResponse.failure(exception.getMessage());
        }
    }

    @RequestMapping(path = "/access-token",method = "POST")
    public ApiResponse<AccountDTO> access(@RequestParameter("token") String token){
        try {
            AccessTokenAccount account = new AccessTokenAccount(token);
            Launcher.getInstance().getAccountManager().registerValue(account);
            return ApiResponse.success(AccountDTO.convertToDTO(account));
        }catch (RuntimeException exception){
            return ApiResponse.failure(exception.getMessage());
        }
    }

    @RequestMapping(path = "/cookie",method = "POST")
    public ApiResponse<AccountDTO> cookie(@RequestBody String cookie){
        try {
            CookieAccount account = new CookieAccount(cookie.getBytes(StandardCharsets.UTF_8));
            Launcher.getInstance().getAccountManager().registerValue(account);
            return ApiResponse.success(AccountDTO.convertToDTO(account));
        }catch (RuntimeException exception){
            return ApiResponse.failure(exception.getMessage());
        }
    }
}
