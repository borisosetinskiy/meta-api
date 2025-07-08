package com.ob.broker.common;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ApiSetting {
    final Integer maxConnectAttempt;
    final Integer maxConnectOnWeekendAttempt;
    final boolean disableReconnect;
}
