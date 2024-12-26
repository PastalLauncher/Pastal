package org.pastal.launcher.api.objects.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class InstallationProgress {
    private String taskId;
    private int progress;
    private String status;
} 