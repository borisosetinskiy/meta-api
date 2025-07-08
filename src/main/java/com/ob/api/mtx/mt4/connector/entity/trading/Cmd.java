package com.ob.api.mtx.mt4.connector.entity.trading;

public enum Cmd {
    INSTANT(0x40), MARKET(0x42), PENDING(0x43), CLOSE_INSTANT(0x44), CLOSE_MARKET(0x46),
    MODIFY(0x47), DELETE_PENDING(0x48), CLOSE_BY(0x49), MULTIPLE_CLOSE_BY(0x4A);

    private int value;

    Cmd(int value) {
        this.value = value;
    }

    public int value() {
        return value;
    }
}
