package com.ob.broker.common.event;

import lombok.experimental.UtilityClass;

@UtilityClass
public class Util {
    public static String key(Long brokerId, Object accountId) {
        return brokerId + "_" + accountId;
    }
}
