package com.ob.api.mtx.mt4.connector.error;

import com.ob.broker.common.error.Code;
import com.ob.broker.common.error.CodeException;


public class DecompressorException extends CodeException {
    public DecompressorException() {
        super("API response: Error during decompression.", Code.DECOMPRESSION_ERROR);
    }

    public DecompressorException(String message) {
        super(message, Code.DECOMPRESSION_ERROR);
    }

}
