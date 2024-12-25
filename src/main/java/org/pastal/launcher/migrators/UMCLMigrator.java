package org.pastal.launcher.migrators;

import com.google.gson.JsonParser;
import lombok.SneakyThrows;
import org.pastal.launcher.Launcher;
import org.pastal.launcher.natives.NativeUtil;
import org.pastal.launcher.util.IOUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class UMCLMigrator extends MCLMigrator {
    @Override
    public String getName() {
        return "UWP Minecraft Official Launcher";
    }

    @Override
    @SneakyThrows
    public boolean hasData() {
        Path path = Paths.get(System.getenv("APPDATA"), ".minecraft", "launcher_msa_credentials_microsoft_store.bin");
        if (Files.exists(path) && Files.isReadable(path)) {
            byte[] pathData = IOUtils.toByteArray(Files.newInputStream(path));
            String dat = new String(NativeUtil.unprotectData(pathData));
            tokens.addAll(getTokens(JsonParser.parseString(dat).getAsJsonObject()));
            return !tokens.isEmpty();
        }
        return false;
    }
}
