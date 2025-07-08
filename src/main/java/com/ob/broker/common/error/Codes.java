package com.ob.broker.common.error;

import lombok.experimental.UtilityClass;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@UtilityClass
public class Codes {
    public final static Map<Integer, Code> CODES = new ConcurrentHashMap<>();
}
