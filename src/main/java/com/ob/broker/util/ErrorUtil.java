package com.ob.broker.util;

import com.ob.broker.common.error.Code;
import com.ob.broker.common.error.CodeException;

import java.net.UnknownHostException;

public class ErrorUtil {
    public static CodeException toError(Throwable error) {
        if (error == null) {
            return new CodeException(new Exception("Unknown error"), Code.UNKNOWN_ERROR);
        }
        if (error instanceof UnknownHostException || error instanceof java.net.NoRouteToHostException) {
            return new CodeException(error, Code.UNKNOWN_HOST);
        } else if (error.getMessage() != null && (error.getMessage().contains("Connection reset") || error.getMessage().contains("Broken pipe"))) {
            return new CodeException(error, Code.BROKEN_DISCONNECT);
        } else if (error instanceof java.net.SocketTimeoutException || (error.getMessage() != null && error.getMessage().contains("Connection timed out"))) {
            return new CodeException(error, Code.CONNECT_TIMEOUT_ERROR);
        } else if (error instanceof java.net.ConnectException) {
            return new CodeException(error, Code.NO_CONNECTION);
        } else if (error instanceof CodeException) {
            return (CodeException) error;
        }
        return new CodeException(error, Code.COMMON_ERROR);

    }
}
