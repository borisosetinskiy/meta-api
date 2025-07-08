package com.ob.broker.util;

import lombok.Getter;
import lombok.experimental.UtilityClass;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;


@UtilityClass
public class ExecutorUtil {
    @Getter
    final static ExecutorService executorService = Executors.newSingleThreadExecutor();

    public static void sleep(long timeout) {
        try {
            Thread.sleep(timeout);
        } catch (Exception ignored) {
        }
    }
    public static void sleep(long start, long delay) {
        long diff = (System.currentTimeMillis() - start);
        long sleepTime = delay - diff;
        if (sleepTime > 0) {
            sleep(sleepTime);
        }
    }

    public static void sleep(Supplier<Boolean> condition, long start, long mills) {
        while (!condition.get() && (System.currentTimeMillis() - start < mills))
            sleep(10);
    }

}
