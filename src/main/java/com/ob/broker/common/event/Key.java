package com.ob.broker.common.event;

import lombok.Data;


@Data
public class Key {
    Long brokerId ;
    Object accountId ;
    String key;
}
