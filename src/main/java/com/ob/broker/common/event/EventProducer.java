package com.ob.broker.common.event;

import com.ob.broker.common.IBaseApi;
import com.ob.broker.common.Task;
import com.ob.broker.common.TaskExecutor;
import com.ob.broker.util.ErrorUtil;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

@Slf4j
@Data
public class EventProducer {
    final Map<EventTopic, Map<String, EventConsumer>> consumers = new ConcurrentHashMap<>();
    private final TaskExecutor taskExecutor;
    private final Key key;

    public void listener(EventTopic topic, EventConsumer consumer) {
        map(topic).put(consumer.getId(), consumer);
    }

    public void shutdown() {
        var id = Util.key(key.brokerId, key.accountId);
        log.info("Shutting down event producer ... {}", id);
        taskExecutor.shutdown(id);
        taskExecutor.shutdown(EventTopic.PRICE + id);
        consumers.clear();
    }

    private Map<String, EventConsumer> map(EventTopic topic) {
        return consumers.computeIfAbsent(topic, t -> new ConcurrentHashMap<>());
    }

    public void execute(EventTopic topic, Runnable runnable) {
        if (runnable == null) return;
        var id = Util.key(key.brokerId, key.accountId);
        long timestamp = System.currentTimeMillis();

        Task task = new Task() {
            @Override
            public void run() {
                try {
                    runnable.run();
                } catch (Exception e) {
                    log.error("Event {}", runnable, e);
                }
            }

            @Override
            public String connectorId() {
                if (EventTopic.PRICE.equals(topic)) {
                    return EventTopic.PRICE + id;
                }
                return id;
            }

            @Override
            public long timestamp() {
                return timestamp;
            }
        };
        taskExecutor.submit(id, task);
    }

    public BiConsumer<IBaseApi, Event> eventConsumer(EventTopic topic) {

        return (api, event) -> {
            execute(topic, () -> {
                try {
                    map(topic)
                            .values().forEach(ec -> ec.onNext(topic, api, event));
                } catch (Exception e) {
                    log.error("Event {}", event, e);
                    try {
                        var error = ErrorUtil.toError(e);
                        var errorEvent = new GeneralErrorEvent(event.getBrokerId(),
                                event.getAccountId(),
                                event.getTID(),
                                error, event.getEventType());
                        eventConsumer(EventTopic.ERROR).accept(api, errorEvent);
                    } catch (Exception e2) {
                        log.error("Event {}", event, e2);
                    }
                }
                if (event instanceof ErrorEvent && !EventTopic.ERROR.equals(topic)) {
                    try {
                        map(EventTopic.ERROR)
                                .values().forEach(ec -> ec.onNext(topic, api, event));
                    } catch (Exception e2) {
                        log.error("Event {}", event, e2);
                    }
                }
                if (!EventTopic.ALL.equals(topic)) {
                    try {
                        map(EventTopic.ALL)
                                .values().forEach(ec -> ec.onNext(topic, api, event));
                    } catch (Exception e2) {
                        log.error("Event {}", event, e2);
                    }
                }
            });
        };
    }

}
