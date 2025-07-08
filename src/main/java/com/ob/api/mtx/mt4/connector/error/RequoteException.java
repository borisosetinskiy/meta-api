package com.ob.api.mtx.mt4.connector.error;

import com.ob.broker.common.error.Code;
import com.ob.broker.common.error.CodeException;
import lombok.Getter;

@Getter
public class RequoteException extends CodeException {
    private final double bid;
    private final double ask;

    public RequoteException(double bid, double ask) {
        super("Requote. Price is changed. bid: " + bid + ", ask: " + ask, Code.REQUOTE);
        this.bid = bid;
        this.ask = ask;
    }


}
