package org.pastal.launcher.util;

import lombok.experimental.UtilityClass;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

@UtilityClass
public class Multithreading {
    private final String THREAD_NAME = "Multithreading Thread";

    private final ScheduledExecutorService SCHEDULED_RUNNABLE_POOL = Executors.newScheduledThreadPool(3, new ThreadFactory() {
        private final AtomicInteger counter = new AtomicInteger(0);

        @Override
        public Thread newThread(Runnable runnable) {
            return new Thread(runnable, String.format("%s - %d", THREAD_NAME, this.counter.incrementAndGet()));
        }
    });

    public ExecutorService ASYNC_RUNNABLE_POOL = Executors.newCachedThreadPool(new ThreadFactory() {
        private final AtomicInteger counter = new AtomicInteger(0);

        @Override
        public Thread newThread(Runnable runnable) {
            return new Thread(runnable, String.format("%s - %d", THREAD_NAME, this.counter.incrementAndGet()));
        }
    });

    public void schedule(Runnable runnable, long initialDelay, long delay, TimeUnit unit) {
        SCHEDULED_RUNNABLE_POOL.scheduleAtFixedRate(runnable, initialDelay, delay, unit);
    }

    public ScheduledFuture<?> schedule(Runnable runnable, long delay, TimeUnit unit) {
        return Multithreading.SCHEDULED_RUNNABLE_POOL.schedule(runnable, delay, unit);
    }

    public void run(Runnable runnable) {
        ASYNC_RUNNABLE_POOL.execute(runnable);
    }

    public int getAsyncRunnablePoolSize() {
        return ((ThreadPoolExecutor) Multithreading.ASYNC_RUNNABLE_POOL).getActiveCount();
    }
}
