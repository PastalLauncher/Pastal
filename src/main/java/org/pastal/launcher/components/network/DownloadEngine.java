package org.pastal.launcher.components.network;

import okhttp3.Headers;
import okhttp3.Request;
import okhttp3.Response;
import org.pastal.launcher.Launcher;
import org.pastal.launcher.api.interfaces.Component;
import org.pastal.launcher.api.objects.download.DownloadTask;
import org.pastal.launcher.api.objects.download.ProgressData;
import org.pastal.launcher.managers.ComponentManager;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public class DownloadEngine implements Component {
    private final ExecutorService downloadExecutor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

    public CompletableFuture<Void> downloadAsync(final List<DownloadTask> tasks, final Consumer<ProgressData> progressCallback) {
        final List<CompletableFuture<Void>> futures = new ArrayList<>();

        for (final DownloadTask task : tasks) {
            final CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                try {
                    handleDownloadTask(task, progressCallback);
                } catch (IOException e) {
                    throw new CompletionException(e);
                }
            }, downloadExecutor);

            futures.add(future);
        }

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
    }

    public CompletableFuture<Void> downloadTotal(final String taskName, final List<DownloadTask> tasks, final Consumer<ProgressData> progressCallback) {
        final List<CompletableFuture<Void>> futures = new ArrayList<>();
        final AtomicInteger completedCount = new AtomicInteger(0);
        final int totalTasks = tasks.size();

        for (final DownloadTask task : tasks) {
            final CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                try {
                    handleDownloadTask(task, progressData -> {
                    });
                } catch (IOException e) {
                    throw new CompletionException(e);
                } finally {
                    int completed = completedCount.incrementAndGet();
                    progressCallback.accept(new ProgressData(taskName, totalTasks, completed, 1));
                }
            }, downloadExecutor);

            futures.add(future);
        }

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
    }

    private void handleDownloadTask(final DownloadTask task, final Consumer<ProgressData> progressCallback) throws IOException {
        final RequestComponent requestComponent = Launcher.getInstance().getComponentManager().get(RequestComponent.class);
        final File targetFile = new File(task.getPath());
        targetFile.getParentFile().mkdirs();

        try (final Response response = requestComponent.getClient().newCall(
                new Request.Builder()
                        .url(task.getUrl())
                        .headers(Headers.of(requestComponent.createStandardHeaders()))
                        .build()
        ).execute()) {

            if (!response.isSuccessful()) {
                throw new IOException(response.code() + " " + task.getUrl());
            }

            long downloadedSize = 0;
            final int threadId = (int) Thread.currentThread().getId();

            try (final InputStream is = response.body().byteStream();
                 final BufferedInputStream bis = new BufferedInputStream(is);
                 final FileOutputStream fos = new FileOutputStream(targetFile)) {

                final byte[] buffer = new byte[8192];
                int read;

                while ((read = bis.read(buffer)) != -1) {
                    fos.write(buffer, 0, read);
                    downloadedSize += read;

                    if (progressCallback != null) {
                        progressCallback.accept(new ProgressData(
                                targetFile.getName(),
                                task.getSize(),
                                downloadedSize,
                                threadId
                        ));
                    }
                }
            }
        }
    }

    public void close() {
        downloadExecutor.shutdown();
        try {
            if (!downloadExecutor.awaitTermination(60, TimeUnit.SECONDS)) {
                downloadExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            downloadExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public void setup(ComponentManager componentManager) {

    }
}
