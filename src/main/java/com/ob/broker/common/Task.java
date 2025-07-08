package com.ob.broker.common;

public interface Task extends Runnable {
    String connectorId();
    long timestamp();
}
