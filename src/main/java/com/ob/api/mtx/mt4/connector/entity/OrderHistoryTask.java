package com.ob.api.mtx.mt4.connector.entity;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

public class OrderHistoryTask {
    public final int from;
    public final int to;

    public OrderHistoryTask(LocalDateTime from, LocalDateTime to) {
        this.from = (int) from.toEpochSecond(ZoneOffset.UTC);
        this.to = (int) to.toEpochSecond(ZoneOffset.UTC);
    }
}
