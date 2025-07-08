package com.ob.broker.common.model;

import lombok.Getter;

@Getter
public enum ContractType {
    UNKNOWN(-1),
    FOREX(0),
    CFD(1),
    FUTURES(2),
    CFD_INDEX(3),
    CFD_LEVERAGE(4),
    CRYPTO(5),
    STOCK(6);
    final int value;

    ContractType(int value) {
        this.value = value;
    }

    public static ContractType fromValue(int value) {
        return switch (value) {
            case 0 -> FOREX;
            case 1 -> CFD;
            case 2 -> FUTURES;
            case 3 -> CFD_INDEX;
            case 4 -> CFD_LEVERAGE;
            case 5 -> CRYPTO;
            case 6 -> STOCK;
            default -> UNKNOWN;
        };
    }
}
