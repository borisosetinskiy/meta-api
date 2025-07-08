package com.ob.api.mtx.mt5;

/**
 * Stage of order processing by server.
 */
public enum ProgressType {
    /**
     * Order was rejected.
     */
    Rejected,
    /**
     * Order was accepted by server.
     */
    Accepted,
    /**
     * Server started to execute the order.
     */
    InProcess,
    /**
     * Order was opened.
     */
    Opened,
    /**
     * Order was closed.
     */
    Closed,
    /**
     * Order was modified.
     */
    Modified,
    /**
     * Pending order was deleted.
     */
    PendingDeleted,
    /**
     * Closed of pair of opposite orders.
     */
    ClosedBy,
    /**
     * Closed of multiple orders.
     */
    MultipleClosedBy,
    /**
     * Trade timeout.
     */
    Timeout,
    /**
     * Price data.
     */
    Price,
    /**
     * Exception.
     */
    Exception;

    public static final int SIZE = Integer.SIZE;

    public static ProgressType forValue(int value) {
        return values()[value];
    }

    public int getValue() {
        return this.ordinal();
    }
}