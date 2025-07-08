package com.ob.api.dx;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;

public record ExecutorManager(ExecutorService outcome, ExecutorService income, ExecutorService event,
                              ScheduledExecutorService scheduler) {

    public void executeOutcome(Runnable runnable) {
        outcome.execute(runnable);
    }
}
