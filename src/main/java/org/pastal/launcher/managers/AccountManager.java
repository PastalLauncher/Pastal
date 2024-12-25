package org.pastal.launcher.managers;

import com.google.gson.*;
import lombok.Getter;
import org.pastal.launcher.Launcher;
import org.pastal.launcher.api.ValueManager;
import org.pastal.launcher.api.interfaces.Account;
import org.pastal.launcher.api.objects.account.AccessTokenAccount;
import org.pastal.launcher.api.objects.account.CookieAccount;
import org.pastal.launcher.api.objects.account.OfflineAccount;
import org.pastal.launcher.api.objects.account.RefreshTokenAccount;
import org.tinylog.Logger;

import java.io.*;
import java.util.function.Predicate;

public class AccountManager extends ValueManager<Account> {
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final File configFile;
    @Getter
    private Account selectedAccount;

    public AccountManager(final Launcher launcher) {
        final File pastalDir = new File(launcher.getRunningDirectory(), "pastal");
        if (!pastalDir.exists()) {
            pastalDir.mkdirs();
        }
        this.configFile = new File(pastalDir, "accounts.json");
    }

    @Override
    public void registerValue(final Account value) {
        if (values.stream().anyMatch(account -> account.getUsername().equals(value.getUsername()))) {
            throw new IllegalArgumentException("Account with username '" + value.getUsername() + "' already exists");
        }
        super.registerValue(value);

        if (selectedAccount == null) {
            selectedAccount = value;
        }

        saveConfig();
    }

    public void forceRegisterValue(final Account value) {
        getValues().removeIf(account -> account.getUsername().equals(value.getUsername()));
        this.registerValue(value);
    }

    public void setSelectedAccount(final Account account) {
        if (!values.contains(account)) {
            throw new IllegalArgumentException("Account is not registered");
        }
        this.selectedAccount = account;
        saveConfig();
    }

    public void load() {
        if (!configFile.exists()) {
            return;
        }

        try (final Reader reader = new FileReader(configFile)) {
            final JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();

            final JsonArray accountsArray = root.getAsJsonArray("accounts");
            final String selectedUsername = root.has("selected") ?
                    root.get("selected").getAsString() : null;

            for (final JsonElement element : accountsArray) {
                final JsonObject obj = element.getAsJsonObject();
                final String type = obj.get("type").getAsString();

                Account account = null;
                if ("Offline".equalsIgnoreCase(type)) {
                    account = new OfflineAccount("");
                    account.fromJson(obj);
                    registerValue(account);
                }
                if ("Access Token".equalsIgnoreCase(type)) {
                    account = new AccessTokenAccount();
                    account.fromJson(obj);
                    registerValue(account);
                }
                if ("Cookie".equalsIgnoreCase(type)) {
                    account = new CookieAccount();
                    account.fromJson(obj);
                    registerValue(account);
                }
                if ("Refresh Token".equalsIgnoreCase(type)) {
                    account = new RefreshTokenAccount();
                    account.fromJson(obj);
                    registerValue(account);
                }

                if (account == null) {
                    Logger.warn("Unknown account type: {}", type);
                    continue;
                }

                if (account.getUsername().equals(selectedUsername)) {
                    selectedAccount = account;
                }
            }

            Logger.info("Loaded {} account(s)", values.size());
        } catch (IOException e) {
            Logger.warn("Failed to load accounts", e);
        }
    }

    public void saveConfig() {
        final JsonObject root = new JsonObject();

        final JsonArray accountsArray = new JsonArray();
        for (final Account account : values) {
            accountsArray.add(account.asJson());
        }
        root.add("accounts", accountsArray);

        if (selectedAccount != null) {
            root.addProperty("selected", selectedAccount.getUsername());
        }

        try (final Writer writer = new FileWriter(configFile)) {
            writer.write(gson.toJson(root));
        } catch (IOException e) {
            Logger.warn("Failed to save accounts", e);
        }
    }
}
