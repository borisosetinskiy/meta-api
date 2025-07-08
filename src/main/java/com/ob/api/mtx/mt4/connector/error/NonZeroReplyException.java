package com.ob.api.mtx.mt4.connector.error;

import com.ob.broker.common.error.Code;
import com.ob.broker.common.error.CodeException;

public class NonZeroReplyException extends CodeException {
    public NonZeroReplyException() {
        super("Server response: Protocol error.", Code.PROTOCOL_ERROR);
    }

}
