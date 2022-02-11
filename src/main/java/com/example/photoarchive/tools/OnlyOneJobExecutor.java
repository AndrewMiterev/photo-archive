package com.example.photoarchive.tools;

import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class OnlyOneJobExecutor {
    private final ScheduledExecutorService service;
    private final Runnable runnable;
    private ScheduledFuture<?> planned = null;

    public OnlyOneJobExecutor(Runnable runnable) {
        service = Executors.newScheduledThreadPool(1);
        this.runnable = runnable;
    }

    public void runAfter(int seconds) {
        if (Objects.nonNull(planned)) {
            planned.cancel(true);
        }
        if (seconds >= 0)
            planned = service.scheduleWithFixedDelay(runnable, seconds, seconds, TimeUnit.SECONDS);
    }

    public void stop() {
        if (Objects.nonNull(planned)) {
            planned.cancel(true);
        }
    }
}
