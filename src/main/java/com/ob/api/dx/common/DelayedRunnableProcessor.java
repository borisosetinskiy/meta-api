package com.ob.api.dx.common;

import com.ob.broker.common.MetricService;
import com.ob.broker.util.ExecutorUtil;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

@Data
@Slf4j
@EqualsAndHashCode(of = {"name", "accountId"})
@ToString(of = {"name", "accountId"})
public class DelayedRunnableProcessor {
    final String name;
    final Integer delay;
    final Object accountId;
    final ExecutorService eventLoop;
    final MetricService metricService;
    final LinkedBlockingQueue<Runnable> commandQueue = new LinkedBlockingQueue<>();
    final AtomicBoolean active = new AtomicBoolean(false);


    public void start() {
        active.getAndSet(true);
        eventLoop.execute(this::run);
    }

    public void stop() {
        active.getAndSet(false);
        commandQueue.clear();
    }

    public void clear() {
        commandQueue.clear();
    }


    public void add(Runnable request) {
        commandQueue.offer(request);
    }

    public void run() {
        long time;
        try {
            while (active.get() && !Thread.currentThread().isInterrupted()) {
                time = System.currentTimeMillis();
                var request = commandQueue.take();
                try {
                    request.run();
                } catch (Exception e) {
                    log.error("Error in runner name {} account {}", name(), accountId, e);
                }finally {
                    metricService.record(name() + "_command_request_execute_delay", System.currentTimeMillis() - time, "accountId", String.valueOf(accountId));
                }
                ExecutorUtil.sleep(time, delay);
            }
        } catch (Exception e) {
            log.error("Error in runner name {} account {}", name(), accountId, e);
        }
    }

    private String name() {
        return name;
    }

}
