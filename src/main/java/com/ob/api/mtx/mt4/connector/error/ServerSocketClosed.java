package com.ob.api.mtx.mt4.connector.error;

import com.ob.broker.common.error.Code;
import com.ob.broker.common.error.CodeException;

public class ServerSocketClosed extends CodeException {
    public ServerSocketClosed() {
        super("Broker response: Socket closed.", Code.NETWORK_ERROR);
    }

}
