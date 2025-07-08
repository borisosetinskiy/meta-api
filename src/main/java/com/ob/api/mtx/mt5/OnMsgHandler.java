package com.ob.api.mtx.mt5;

@FunctionalInterface
public interface OnMsgHandler {
    /**
     * New message event handler.
     *
     * @param sender Object that sent message
     * @param msg    Message
     * @param type   Message type
     */
    void invoke(Object sender, String msg, MsgType type);
}
