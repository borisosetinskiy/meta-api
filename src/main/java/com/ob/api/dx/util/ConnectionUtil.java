package com.ob.api.dx.util;

import com.ob.broker.common.model.ConnectionStatus;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@UtilityClass
public class ConnectionUtil {
    public boolean isDead(ConnectionStatus connectionStatus) {
        return ConnectionStatus.DEAD == connectionStatus;
    }

    public boolean isConnected(ConnectionStatus connectionStatus) {
        return ConnectionStatus.ONLINE == connectionStatus;
    }


}
/*
Client:

{
    "type": "Ping",
    "inReplyTo": "test",
    "session": "2gkbiva152dov44d968sf9s0eh",
    "timestamp": "2022-07-27T14:50:30.238Z"
}
 */