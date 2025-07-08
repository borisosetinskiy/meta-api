package com.ob.api.mtx.mt4.connector.entity.trading;

public enum ExecutionType {
    REQUEST(0), INSTANT(1), MARKET(2);

    private int value;

    ExecutionType(int value) {
        this.value = value;
    }

    public int value() {
        return this.value;
    }
}
