package com.ob.api.mtx.mt5;

import com.ob.broker.common.error.Code;
import com.ob.broker.common.error.CodeException;

import java.io.Serial;


/**
 * Trade timeout exception.
 */
public class TimeoutException extends CodeException {
    /**
     *
     */
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * Initialize TimeoutException.
     *
     * @param message Exception message.
     */
    public TimeoutException(String message) {
        super(message, Code.REQUEST_TIMEOUT);
    }
}