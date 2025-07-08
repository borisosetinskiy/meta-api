package com.ob.broker.common;

import com.ob.broker.common.model.HostPort;
import lombok.Data;

import java.util.List;

@Data
public class ApiCredentials {
    final Long brokerId;
    final Object accountId;
    final String password;
    final List<HostPort> hostPorts;
    final Boolean investor;
}
