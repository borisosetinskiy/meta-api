package com.ob.api.mtx.mt4.connector.error;

import com.ob.broker.common.error.Code;
import com.ob.broker.common.error.CodeException;

public class OldServerException extends CodeException {
    public OldServerException(String s) {
        super(s, Code.OLD_SERVER);
    }

}
