package com.ob.api.dx.model.data;

public interface Types {
    enum Status {
        ACCEPTED, WORKING, CANCELED, COMPLETED, EXPIRED, REJECTED
    }

    enum Type {
        MARKET, LIMIT, STOP
    }

    enum Tif {
        DAY, GTC, FOK, IOC, GTD
    }

    enum Side {
        BUY, SELL
    }

    enum PriceLink {
        TRIGGERED_STOP, TRIGGERED_LIMIT
    }

    enum LinkType {
        PARENT, CHILD, OCO
    }

    enum PositionEffect {
        OPEN, CLOSE
    }
}
