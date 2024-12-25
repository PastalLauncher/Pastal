package org.pastal.launcher.migrators;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.SneakyThrows;
import org.pastal.launcher.Launcher;
import org.pastal.launcher.api.interfaces.Migrator;
import org.pastal.launcher.api.interfaces.Importable;
import org.pastal.launcher.api.objects.account.RefreshTokenAccount;
import org.pastal.launcher.enums.ClientIdentification;
import org.tinylog.Logger;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class HMCLMigrator implements Migrator {
    private final Map<String, String> tokens = new LinkedHashMap<>();

    @Override
    public String getName() {
        return "HMCL";
    }

    @Override
    public List<Importable> findData() {
        List<Importable> importables = new ArrayList<>();

        for (String s : tokens.keySet()) {
            importables.add(new Importable() {
                @Override
                public String getDisplayString() {
                    return "Account " + s;
                }

                @Override
                public void importData() {
                    Launcher.getInstance().getAccountManager()
                            .forceRegisterValue(new RefreshTokenAccount(tokens.get(s), ClientIdentification.HMCL, true));
                }
            });
        }

        return importables;
    }

    @Override
    @SneakyThrows
    public boolean hasData() {

        Path path = Paths.get(System.getenv("APPDATA"), ".hmcl", "accounts.json");
        if (Files.exists(path) && Files.isReadable(path)) {
            byte[] pathData = toByteArray(Files.newInputStream(path));
            JsonArray array = JsonParser.parseString(new String(pathData)).getAsJsonArray();
            for (JsonElement jsonElement : array) {
                JsonObject object = jsonElement.getAsJsonObject();
                if (object.has("refreshToken")) {
                    tokens.put(object.get("displayName").getAsString(), object.get("refreshToken").getAsString());
                }
            }
            return !tokens.isEmpty();
        }
        return false;
    }

    public byte[] toByteArray(final InputStream inputStream) {
        final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        int nRead;
        final byte[] data = new byte[16384];
        try {
            while ((nRead = inputStream.read(data, 0, data.length)) != -1) {
                buffer.write(data, 0, nRead);
            }
        } catch (Exception e) {
            Logger.error(e);
        }
        return buffer.toByteArray();
    }

}
