package org.pastal.launcher.migrators;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.SneakyThrows;
import org.pastal.launcher.Launcher;
import org.pastal.launcher.api.interfaces.Migrator;
import org.pastal.launcher.api.interfaces.Importable;
import org.pastal.launcher.api.objects.account.RefreshTokenAccount;
import org.pastal.launcher.enums.ClientIdentification;
import org.pastal.launcher.natives.NativeUtil;
import org.pastal.launcher.util.IOUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MCLMigrator implements Migrator {
    protected final List<String> tokens = new ArrayList<>();

    @Override
    public String getName() {
        return "Minecraft Official Launcher";
    }

    @Override
    public List<Importable> findData() {
        List<Importable> importables = new ArrayList<>();

        for (String token : tokens) {
            importables.add(new Importable() {
                @Override
                public String getDisplayString() {
                    return "XBox User " + token.hashCode();
                }

                @Override
                public void importData() {
                    Launcher.getInstance().getAccountManager()
                            .forceRegisterValue(new RefreshTokenAccount(token, ClientIdentification.OFFICIAL, true));
                }
            });
        }

        return importables;
    }

    @Override
    @SneakyThrows
    public boolean hasData() {
        Path path = Paths.get(System.getenv("APPDATA"), ".minecraft", "launcher_msa_credentials.bin");
        if (Files.exists(path) && Files.isReadable(path)) {
            byte[] pathData = IOUtils.toByteArray(Files.newInputStream(path));
            String dat = new String(NativeUtil.unprotectData(pathData));
            tokens.addAll(getTokens(JsonParser.parseString(dat).getAsJsonObject()));
            return !tokens.isEmpty();
        }
        return false;
    }

    protected List<String> getTokens(JsonObject json) {
        List<String> tokens = new ArrayList<>();
        if (json.has("credentials") && json.get("credentials").isJsonObject()) {
            JsonObject credentialsObject = json.getAsJsonObject("credentials");

            Pattern msaPattern = Pattern.compile("^Xal\\.\\d+\\.Production\\.Msa\\..+$");
            Pattern credentialNamePattern = Pattern.compile("^\\d+$");

            for (String name : credentialsObject.keySet()) {
                Matcher nameMatcher = credentialNamePattern.matcher(name);
                if (nameMatcher.matches()) {
                    JsonElement credentialElement = credentialsObject.get(name);
                    if (credentialElement.isJsonObject()) {
                        JsonObject credential = credentialElement.getAsJsonObject();

                        JsonObject tokenData = null;
                        for (String msaName : credential.keySet()) {
                            Matcher msaMatcher = msaPattern.matcher(msaName);
                            if (msaMatcher.matches()) {
                                JsonElement msaElement = credential.get(msaName);
                                if (msaElement.isJsonPrimitive()) {
                                    String msaJsonString = msaElement.getAsString();
                                    tokenData = JsonParser.parseString(msaJsonString).getAsJsonObject();
                                    break;
                                }
                            }
                        }

                        if (tokenData != null && tokenData.has("refresh_token")) {
                            tokens.add(tokenData.get("refresh_token").getAsString());
                        }
                    }
                }
            }
        }
        return tokens;
    }
}
