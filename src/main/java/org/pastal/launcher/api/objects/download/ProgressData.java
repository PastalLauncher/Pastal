package org.pastal.launcher.api.objects.download;

import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
public class ProgressData {
    private final String fileName;
    private final long totalSize;
    private final long downloadedSize;
    private final double progress;
    private final int threadId;

    public ProgressData(final String fileName, final long totalSize, final long downloadedSize, final int threadId) {
        this.fileName = fileName;
        this.totalSize = totalSize;
        this.downloadedSize = downloadedSize;
        this.progress = totalSize == 0 ? 0 : (double) downloadedSize / totalSize;
        this.threadId = threadId;
    }
} 