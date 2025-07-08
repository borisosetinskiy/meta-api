package com.ob.api.mtx.mt4.connector.error;

import com.ob.broker.common.error.Code;
import com.ob.broker.common.error.CodeException;

public class TradesNotAllowedError extends CodeException {
    public TradesNotAllowedError() {
        super("Trades is not allowed", Code.TRADE_DISABLED);
    }
}
