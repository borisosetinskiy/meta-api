package com.ob.broker.common;

public interface TaskExecutor {
    void submit(String connectorId, Task task);

    default void shutdown() {
    }

    ;

    default void shutdown(String connectorId) {
    }
}
