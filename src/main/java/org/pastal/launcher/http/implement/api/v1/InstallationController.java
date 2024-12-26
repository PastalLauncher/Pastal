package org.pastal.launcher.http.implement.api.v1;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.pastal.launcher.Launcher;
import org.pastal.launcher.api.objects.ApiResponse;
import org.pastal.launcher.api.objects.Installation;
import org.pastal.launcher.api.objects.Version;
import org.pastal.launcher.api.objects.download.ProgressData;
import org.pastal.launcher.api.objects.dto.InstallationProgress;
import org.pastal.launcher.http.annotations.RequestMapping;
import org.pastal.launcher.http.annotations.RequestParameter;
import org.pastal.launcher.http.annotations.RestController;
import org.pastal.launcher.managers.InstallationManager;
import org.pastal.launcher.util.HashUtils;
import org.pastal.launcher.util.IOUtils;
import org.pastal.launcher.util.Multithreading;
import org.tinylog.Logger;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

@RestController("/api/v1/installations")
public class InstallationController {

    private static final List<InstallationProgress> installationProgresses = new CopyOnWriteArrayList<>();

    @RequestMapping(path = "/list")
    public ApiResponse<Collection<Installation>> getInstallations() {
        return ApiResponse.success(Launcher.getInstance()
                .getInstallationManager()
                .getValues());
    }

    @RequestMapping(path = "/versions")
    public ApiResponse<Collection<Version>> getVersions() {
        try {
            return ApiResponse.success(Launcher.getInstance()
                    .getVersionManager()
                    .getVersions());
        } catch (IOException e) {
            return ApiResponse.failure(e.getMessage());
        }
    }

    @RequestMapping(path = "/install", method = "POST")
    public ApiResponse<String> installVersion(
            @RequestParameter("versionId") String versionId,
            @RequestParameter("name") String name) {
        
        InstallationManager manager = Launcher.getInstance().getInstallationManager();
        
        if (manager.isInstalled(name)) {
            return ApiResponse.failure("Version already installed");
        }

        String taskId = generateTaskId(name);
        InstallationProgress progress = new InstallationProgress(taskId, 0, "Starting installation...");
        installationProgresses.add(progress);

        try {
            manager.checkInstallation(versionId,name);
        }catch (IllegalStateException | IllegalArgumentException t){
            return ApiResponse.failure(t.getMessage());
        }

        Multithreading.run(() -> {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Logger.error(e);
            }
            try {
                manager.createInstallation(versionId, name, data -> {
                    progress.setProgress((int) (data.getProgress() * 100));
                    progress.setStatus(data.getFileName());
                });
                progress.setProgress(100);
                progress.setStatus("Installation completed");
            } catch (IOException e) {
                progress.setStatus("Installation failed: " + e.getMessage());
            }
        });

        return ApiResponse.success(taskId);
    }

    @RequestMapping(path = "/progress")
    public ApiResponse<InstallationProgress> getProgress(@RequestParameter("taskId") String taskId) {
        return installationProgresses.stream()
                .filter(p -> p.getTaskId().equals(taskId))
                .findFirst()
                .map(ApiResponse::success)
                .orElse(ApiResponse.failure("Task not found"));
    }

    @RequestMapping(path = "/remove", method = "POST")
    public ApiResponse<String> removeInstallation(@RequestParameter("name") String name) {
        InstallationManager manager = Launcher.getInstance().getInstallationManager();
        Installation installation = manager.getInstallation(name);
        
        if (installation == null) {
            return ApiResponse.failure("Installation not found");
        }

        try {
            IOUtils.deleteDirectory(installation.getDirectory());
            manager.removeValue(installation);
            return ApiResponse.success("Installation removed");
        } catch (Throwable e) {
            return ApiResponse.failure("Failed to remove installation: " + e.getMessage());
        }
    }

    private String generateTaskId(String name) {
        return HashUtils.sha1((name + "-" + System.currentTimeMillis()));
    }
}
