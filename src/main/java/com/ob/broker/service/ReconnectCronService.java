package com.ob.broker.service;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

@Data
@Slf4j
public class ReconnectCronService {
    private final ScheduledExecutorService scheduler;
    private final ExecutorService taskExecutor;
    private final Map<String, ReconnectAgent> agents = new ConcurrentHashMap<>();
    private final Map<String, AtomicBoolean> agentLocks = new ConcurrentHashMap<>();

    public ReconnectCronService(int parallelism) {
        this.scheduler = Executors.newSingleThreadScheduledExecutor();
        this.taskExecutor = Executors.newFixedThreadPool(parallelism);
    }

    public void register(ReconnectAgent agent) {
        log.info("Registering agent {}", agent);
        agents.put(agent.getId(), agent);
        agentLocks.put(agent.getId(), new AtomicBoolean(false));
    }

    public void unregister(ReconnectAgent agent) {
        if(agent == null) return;
        try {
            log.info("Unregistering reconnect agent {}", agent.getId());
            agents.remove(agent.getId());
            agentLocks.remove(agent.getId());
        }catch (Exception ignored) {}
    }

    public void start() {
        scheduler.scheduleAtFixedRate(() -> {
            try {
                for (ReconnectAgent agent : agents.values()) {
                    AtomicBoolean lock = agentLocks.get(agent.getId());
                    if (lock == null || !lock.compareAndSet(false, true)) {
                        continue; // already running or unregistered
                    }
                    if (agent.isConnected() || agent.isDead()) {
                        lock.set(false);
                        continue;
                    }
                    taskExecutor.submit(() -> {
                        try {
                            if (agent.isConnected() || agent.isDead()) {
                                return;
                            }
                            agent.reconnect();
                        } catch (Exception e) {
                            log.error("Error handling reconnect for agent {}", agent.getId(), e);
                        } finally {
                            lock.set(false);
                        }
                    });
                }
            } catch (Exception e) {
                log.error("Reconnect cron scheduler error", e);
            }
        }, 50, 50, TimeUnit.MILLISECONDS);
    }
}
