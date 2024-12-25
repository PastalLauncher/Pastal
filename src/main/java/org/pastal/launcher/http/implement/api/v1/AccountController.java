package org.pastal.launcher.http.implement.api.v1;

import org.pastal.launcher.Launcher;
import org.pastal.launcher.api.interfaces.Account;
import org.pastal.launcher.api.interfaces.PremiumAccount;
import org.pastal.launcher.api.objects.ApiResponse;
import org.pastal.launcher.api.objects.HypixelData;
import org.pastal.launcher.enums.MediaType;
import org.pastal.launcher.event.events.RequestEvent;
import org.pastal.launcher.http.annotations.RequestMapping;
import org.pastal.launcher.http.annotations.RequestParameter;
import org.pastal.launcher.http.annotations.RestController;
import org.pastal.launcher.api.objects.dto.AccountDTO;
import org.pastal.launcher.managers.AccountManager;
import org.pastal.launcher.util.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

@RestController("/api/v1/accounts")
public class AccountController {
    
    @RequestMapping(path = "/list")
    public ApiResponse<List<AccountDTO>> getAccounts() {
        Collection<Account> accounts = Launcher.getInstance()
                                      .getAccountManager()
                                      .getValues();
                                      
        return ApiResponse.success(accounts.stream()
                .map(AccountDTO::convertToDTO)
                .collect(Collectors.toList()));
    }


    @RequestMapping(path = "/current")
    public ApiResponse<AccountDTO> getCurrentAccount() {
        Account account = Launcher.getInstance()
                .getAccountManager()
                .getSelectedAccount();

        return account != null ? ApiResponse.success(AccountDTO.convertToDTO(account)) : ApiResponse.failure(null);
    }

    @RequestMapping(path = "/select", method = "POST")
    public ApiResponse<AccountDTO> selectAccount(@RequestParameter("username") String username) {
        AccountManager manager = Launcher.getInstance().getAccountManager();

        Optional<Account> account = manager.getValues().stream()
                .filter(a -> a.getUsername().equals(username))
                .findFirst();

        if(account.isPresent()) {
            manager.setSelectedAccount(account.get());
            return ApiResponse.success(AccountDTO.convertToDTO(account.get()));
        }

        return ApiResponse.failure("Account not found");
    }

    @RequestMapping(path = "/remove", method = "POST")
    public ApiResponse<String> removeAccount(@RequestParameter("username") String username) {
        AccountManager manager = Launcher.getInstance().getAccountManager();

        Optional<Account> account = manager.getValues().stream()
                .filter(a -> a.getUsername().equals(username))
                .findFirst();

        if(account.isPresent()) {
            manager.removeValue(account.get());
            return ApiResponse.success("");
        }

        return ApiResponse.success("Account not found");
    }

    @RequestMapping(path = "/rename", method = "POST")
    public ApiResponse<AccountDTO> renameAccount(
            @RequestParameter("username") String username,
            @RequestParameter("newName") String newName) {
        AccountManager manager = Launcher.getInstance().getAccountManager();

        Optional<Account> account = manager.getValues().stream()
                .filter(a -> a.getUsername().equals(username))
                .findFirst();

        if(account.isPresent() && account.get() instanceof PremiumAccount) {
            try {
                PremiumAccount premiumAccount = (PremiumAccount) account.get();
                premiumAccount.rename(newName);
                return ApiResponse.success(AccountDTO.convertToDTO(premiumAccount));
            } catch (RuntimeException e) {
                return ApiResponse.failure(e.getMessage());
            }
        }

        return ApiResponse.failure("Account not found or not premium");
    }

    @RequestMapping(path = "/reload", method = "POST")
    public ApiResponse<AccountDTO> reloadAccount(@RequestParameter("username") String username) {
        AccountManager manager = Launcher.getInstance().getAccountManager();

        Optional<Account> account = manager.getValues().stream()
                .filter(a -> a.getUsername().equals(username))
                .findFirst();

        if(account.isPresent() && account.get() instanceof PremiumAccount) {
            PremiumAccount premiumAccount = (PremiumAccount) account.get();
            premiumAccount.reload();
            return ApiResponse.success(AccountDTO.convertToDTO(premiumAccount));
        }

        return ApiResponse.failure("Account not found or not premium");
    }

    @RequestMapping(path = "/hypixel")
    public ApiResponse<HypixelData> getHypixelData(@RequestParameter("username") String username) {
        AccountManager manager = Launcher.getInstance().getAccountManager();

        Optional<Account> account = manager.getValues().stream()
                .filter(a -> a.getUsername().equals(username))
                .findFirst();

        if(account.isPresent() && account.get() instanceof PremiumAccount) {
            try {
                PremiumAccount premiumAccount = (PremiumAccount) account.get();
                HypixelData data = premiumAccount.getHypixelData();
                return data != null ?
                        ApiResponse.success(data) :
                        ApiResponse.failure("Failed to fetch Hypixel data");
            } catch (IOException e) {
                return ApiResponse.failure("Error fetching Hypixel data: " + e.getMessage());
            }
        }

        return ApiResponse.failure("Account not found or not premium");
    }

    @RequestMapping(path = "/links")
    public ApiResponse<Map<String, String>> getAccountLinks(@RequestParameter("username") String username) {
        AccountManager manager = Launcher.getInstance().getAccountManager();

        Optional<Account> account = manager.getValues().stream()
                .filter(a -> a.getUsername().equals(username))
                .findFirst();

        if(account.isPresent() && account.get() instanceof PremiumAccount) {
            PremiumAccount premiumAccount = (PremiumAccount) account.get();
            Map<String, String> links = new HashMap<>();
            links.put("namemc", premiumAccount.getNameMCProfile());
            links.put("plancke", premiumAccount.getPlancke());
            links.put("skycrypt", premiumAccount.getSkyCrypt());
            return ApiResponse.success(links);
        }

        return ApiResponse.failure("Account not found or not premium");
    }

    @RequestMapping(path = "/avatar")
    public void getAvatar(@RequestParameter("username") String username, RequestEvent event) {
        AccountManager manager = Launcher.getInstance().getAccountManager();

        Optional<Account> account = manager.getValues().stream()
                .filter(a -> a.getUsername().equals(username))
                .findFirst();

        if(account.isPresent()) {
            try (InputStream is = account.get().getAvatar()) {
                byte[] imageData = IOUtils.toByteArray(is);
                event.getExchange().getResponseHeaders().set("Content-Type", "image/png");
                event.doResponse(200, imageData);
            } catch (IOException e) {
                event.doResponse(500, "Error fetching avatar", MediaType.PLAIN_TEXT);
            }
        } else {
            event.doResponse(404, "Account not found", MediaType.PLAIN_TEXT);
        }
    }
    

}
