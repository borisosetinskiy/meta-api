package com.ob.api.mtx.mt4.connector.error;

import com.ob.broker.common.error.Code;
import com.ob.broker.common.error.CodeException;

public class ArraySizeError extends CodeException {

    public ArraySizeError(int size) {
        super("API response: Server response with wrong size " + size + ". Connection is broken."
                , Code.PROTOCOL_ERROR);
    }

}
