package com.ob.broker.common;

public abstract class SimpleTask implements Task{
    final long timestamp = System.currentTimeMillis();
    @Override
    public long timestamp() {
        return timestamp;
    }
}
