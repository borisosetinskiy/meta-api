package com.ob.broker.common.event;

import com.ob.broker.common.error.CodeException;

public interface ErrorEvent extends Event {
    CodeException getError();
}
