package org.pastal.launcher.api.objects.download;

import lombok.Getter;
import lombok.Setter;
import org.pastal.launcher.Launcher;

@Getter
public class DownloadTask {
    private final String url;
    private final long size;
    private final String path;

    @Setter
    private long downloadedSize;

    public DownloadTask(final String url, final String path, final long size) {
        this.url = Launcher.getInstance().getMirrorManager().getCurrentMirror().getMirrorUrl(url);
        this.size = size;
        this.path = path;
        this.downloadedSize = 0;
    }

    public double getProgress() {
        return size == 0 ? 0 : (double) downloadedSize / size;
    }
} 