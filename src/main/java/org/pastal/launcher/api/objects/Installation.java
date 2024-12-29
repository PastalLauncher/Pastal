package org.pastal.launcher.api.objects;

import com.google.gson.*;
import lombok.Getter;
import org.pastal.launcher.Launcher;
import org.pastal.launcher.api.interfaces.PremiumAccount;
import org.pastal.launcher.api.objects.download.DownloadTask;
import org.pastal.launcher.api.objects.download.ProgressData;
import org.pastal.launcher.components.network.DownloadEngine;
import org.pastal.launcher.config.LaunchSettings;
import org.pastal.launcher.option.OptionSet;
import org.pastal.launcher.util.IOUtils;
import org.pastal.launcher.util.JsonUtils;
import org.tinylog.Logger;

import java.io.*;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

@Getter
public class Installation {
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    private final String name;
    private final String type;
    private final File directory;
    private final int javaVersion;
    private final LaunchSettings launchConfig;
    private final File configFile;
    private final JsonObject json;

    public Installation(String name, String type, File directory) {
        this.name = name;
        this.type = type;
        this.directory = directory;
        this.configFile = new File(directory, "launch_config.json");
        this.launchConfig = new LaunchSettings(configFile);

        File jsonFile = new File(directory, name + ".json");
        try {
            json = JsonParser.parseString(IOUtils.toString(Files.newInputStream(jsonFile.toPath()))).getAsJsonObject();
            JsonObject javaVersion = json.get("javaVersion").getAsJsonObject();
            this.javaVersion = javaVersion.get("majorVersion").getAsInt();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        launchConfig.getJavaExcutable().set(getBestJava().getExecutable());
        launchConfig.load();
    }

    public void saveConfig() {
        launchConfig.save();
    }

    private boolean enableAdvancedTokenProtection(){
   //     Optional<Boolean> enabled = Launcher.getInstance().getConfigManager().findValue("enableAdvancedTokenProtection",Boolean.class);
        return false;
    }

    public Process launch(final Consumer<String> logListener, final Consumer<String> errorListener, final Consumer<ProgressData> progressCallback) throws IOException {

        if (Launcher.getInstance().getAccountManager().getSelectedAccount() == null)
            throw new IllegalStateException("No account selected.");

        reSyncLibraries(progressCallback);

        if (!launchConfig.getModProfile().isMode("Disabled")) {
            Optional<ModProfile> profile = Launcher.getInstance().getModProfileManager().getProfile(launchConfig.getModProfile().get());
            if (!profile.isPresent()) {
                throw new IllegalStateException("Selected mod profile not existed.");
            }
            Launcher.getInstance().getModProfileManager().activateProfile(profile.get());
        }

        final List<String> command = new ArrayList<>();

        command.add(launchConfig.getJavaExcutable().get());

        command.add("-Xms" + launchConfig.getMinMemory() + "M");
        command.add("-Xmx" + launchConfig.getMaxMemory() + "M");

        if (!launchConfig.getJavaArguments().get().isEmpty()) {
            command.addAll(Arrays.asList(launchConfig.getJavaArguments().get().split(" ")));
        }

        command.add("-XX:+UnlockExperimentalVMOptions");

        if(enableAdvancedTokenProtection()){
            File agentLoader = new File(Launcher.getInstance().getRunningDirectory(),"PastalAgent.jar");
            IOUtils.writeFile(agentLoader,Launcher.class.getResourceAsStream("/agent-1.0-all.jar"));
            command.add("-javaagent:" + agentLoader.getAbsolutePath());
        }

        command.add("-Djava.library.path=" + new File(directory, "natives").getAbsolutePath());
        command.add("-Dminecraft.launcher.brand=pastal");
        command.add("-Dminecraft.launcher.version=pastal");

        if (javaVersion >= 9) {
            // 史诗级调优参数，提升20.00%fps
            command.add("--add-modules=ALL-SYSTEM");
            command.add("--add-exports");
            command.add("java.base/jdk.internal.loader=ALL-UNNAMED");
            command.add("--add-exports");
            command.add("java.base/java.lang=ALL-UNNAMED");
        }

        final StringBuilder classpath = new StringBuilder();
        final File librariesDir = new File(Launcher.getInstance().getRunningDirectory(), "libraries");

        final JsonArray libraries = json.getAsJsonArray("libraries");
        List<JsonElement> elements = new ArrayList<>(libraries.asList());
        Collections.reverse(elements);

        Set<String> addedDependencies = new HashSet<>();
        Set<String> existingClassFiles = new HashSet<>();

        for (final JsonElement lib : elements) {
            final JsonObject library = lib.getAsJsonObject();
            if (JsonUtils.isLibraryAllowed(library)) {
                String dependencyPath = null;
                String uniqueIdentifier = null;

                if (library.has("downloads")) {
                    final JsonObject downloads = library.getAsJsonObject("downloads");
                    if (downloads.has("artifact")) {
                        final JsonObject artifact = downloads.getAsJsonObject("artifact");
                        dependencyPath = new File(librariesDir, artifact.get("path").getAsString()).getAbsolutePath();
                        uniqueIdentifier = artifact.get("path").getAsString();
                    }
                } else if (library.has("name")) {
                    final String name = library.get("name").getAsString();
                    final String[] parts = name.split(":");
                    if (parts.length == 3) {
                        final String group = parts[0];
                        final String artifact = parts[1];
                        final String version = parts[2];
                        final String jarName = artifact + "-" + version + ".jar";
                        final File jarFile = new File(librariesDir, group.replace('.', File.separatorChar) + File.separator + artifact + File.separator + version + File.separator + jarName);
                        dependencyPath = jarFile.getAbsolutePath();
                        uniqueIdentifier = group + ":" + artifact + ":" + version;
                    } else {
                        Logger.warn("Invalid library name format: " + name);
                    }
                } else {
                    Logger.warn("Library does not have a name field.");
                }

                if (dependencyPath != null && uniqueIdentifier != null && !addedDependencies.contains(uniqueIdentifier)) {
                    File jarFile = new File(dependencyPath);
                    boolean duplicateClassFound = false;

                    try (java.util.jar.JarFile jar = new java.util.jar.JarFile(jarFile)) {
                        for (Enumeration<java.util.jar.JarEntry> entries = jar.entries(); entries.hasMoreElements(); ) {
                            String entryName = entries.nextElement().getName();
                            if (entryName.endsWith(".class") && !entryName.endsWith("module-info.class")) {
                                if (existingClassFiles.contains(entryName)) {
                                    Logger.debug("Duplicate class found: " + entryName + " in " + dependencyPath);
                                    duplicateClassFound = true;
                                    break;
                                }
                            }
                        }
                    } catch (Exception e) {
                        Logger.error("Failed to read jar file: " + dependencyPath, e);
                    }

                    if (!duplicateClassFound) {
                        addedDependencies.add(uniqueIdentifier);
                        classpath.append(File.pathSeparator).append(dependencyPath);
                        try (java.util.jar.JarFile jar = new java.util.jar.JarFile(jarFile)) {
                            for (Enumeration<java.util.jar.JarEntry> entries = jar.entries(); entries.hasMoreElements(); ) {
                                String entryName = entries.nextElement().getName();
                                if (entryName.endsWith(".class")) {
                                    existingClassFiles.add(entryName);
                                }
                            }
                        } catch (Exception e) {
                            Logger.error("Failed to process jar file: " + dependencyPath, e);
                        }
                    }
                }
            }
        }


        classpath.append(";").append(new File(directory, name + ".jar").getAbsolutePath());

        command.add("-cp");
        command.add(classpath.toString().replaceFirst(";", ""));

        command.add(json.get("mainClass").getAsString());

        if (json.has("arguments")) {
            final JsonArray gameArgs = json.getAsJsonObject("arguments").getAsJsonArray("game");
            for (final JsonElement arg : gameArgs) {
                if (arg.isJsonPrimitive()) {
                    command.add(processGameArgument(arg.getAsString()));
                }
            }
        } else if (json.has("minecraftArguments")) {
            final String[] args = json.get("minecraftArguments").getAsString().split(" ");
            for (final String arg : args) {
                command.add(processGameArgument(arg));
            }
        }

        final ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.directory(Launcher.getInstance().getRunningDirectory());

        final Process process = processBuilder.start();

        IOUtils.readStream(logListener, process.getErrorStream());

        new Thread(() -> {
            try {
                try (InputStream inputStream = process.getInputStream();
                     BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        errorListener.accept(line);
                    }
                } finally {
                    process.destroy();
                    Launcher.getInstance().getModProfileManager().restoreBackup();
                }
            } catch (Exception ignored) {
            }
        }).start();

        return process;
    }

    private String processGameArgument(String arg) {
        arg = arg.replace("${auth_player_name}", Launcher.getInstance().getAccountManager().getSelectedAccount().getUsername())
                .replace("${version_name}", name)
                .replace("${game_directory}", directory.getAbsolutePath())
                .replace("${assets_root}", new File(Launcher.getInstance().getRunningDirectory(), "assets").getAbsolutePath())
                .replace("${assets_index_name}", getAssetsIndex())
                .replace("${auth_uuid}", Launcher.getInstance().getAccountManager().getSelectedAccount().getUUID().toString().replace("-", ""))
                .replace("${user_type}", "mojang")
                .replace("${version_type}", type)
                .replace("${user_properties}", "{}");

        if(!enableAdvancedTokenProtection()){
            arg = arg.replace("${auth_access_token}", Launcher.getInstance().getAccountManager().getSelectedAccount() instanceof PremiumAccount ? ((PremiumAccount) Launcher.getInstance().getAccountManager().getSelectedAccount()).getAccessToken() : "0");
        }else {
            arg = arg.replace("${auth_access_token}", "0");
        }

        return arg;
    }

    private String getAssetsIndex() {
        return json.getAsJsonObject("assetIndex").get("id").getAsString();
    }

    public JavaInstallation getBestJava() {
        return Launcher.getInstance().getJavaManager().getBestJavaFor(javaVersion);
    }

    public boolean exists() {
        return directory.exists();
    }

    public void reSyncLibraries(final Consumer<ProgressData> progressCallback) {
        final File librariesDir = new File(Launcher.getInstance().getRunningDirectory(), "libraries");
        final List<DownloadTask> tasks = new ArrayList<>();
        Launcher.getInstance().getInstallationManager().addLibs(tasks, json.getAsJsonArray("libraries"), librariesDir);

        final CompletableFuture<Void> downloadFuture = Launcher.getInstance().getComponentManager().get(DownloadEngine.class).downloadAsync(tasks, progressCallback);
        try {
            downloadFuture.get();
        } catch (InterruptedException | ExecutionException e) {
            throw new IllegalStateException("Failed to download client", e);
        }
    }

    public String getClientVersion() {
        return this.getJson().has("version") ? this.getJson().get("version").getAsString() : this.getName();
    }
}