package com.ob.broker.common.model;

import com.ob.broker.common.event.Event;

public interface SymbolData extends Event {
    String getSymbol();
}
