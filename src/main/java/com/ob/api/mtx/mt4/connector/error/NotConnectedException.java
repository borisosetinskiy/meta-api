package com.ob.api.mtx.mt4.connector.error;

import com.ob.broker.common.error.Code;
import com.ob.broker.common.error.CodeException;


public class NotConnectedException extends CodeException {
    public NotConnectedException() {
        super("No connection with trade server.", Code.NO_CONNECTION);
    }

    public NotConnectedException(String message) {
        super(message, Code.NO_CONNECTION);
    }

}
