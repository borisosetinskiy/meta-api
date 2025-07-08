package com.ob.api.mtx.mt5;

/**
 * Message type.
 */
public enum MsgType {
    /**
     * Trace.
     */
    Trace,
    /**
     * Debug.
     */
    Debug,
    /**
     * Information.
     */
    Info,
    /**
     * Warning.
     */
    Warn,
    /**
     * Error.
     */
    Error,
    /**
     * Exception.
     */
    Exception;

    public static final int SIZE = Integer.SIZE;

    public static MsgType forValue(int value) {
        return values()[value];
    }

    public int getValue() {
        return this.ordinal();
    }
}