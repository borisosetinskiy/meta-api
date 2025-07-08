package com.ob.api.mtx.mt5;

import com.ob.broker.common.ApiCredentials;
import com.ob.broker.common.model.HostPort;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@EqualsAndHashCode(callSuper = true)
public class Mt5ApiCredentials extends ApiCredentials {
    final List<String> LoginIdWebServerUrls;
    @Setter
    String brokerName;

    public Mt5ApiCredentials(Long brokerId, Long account
            , String password, List<HostPort> hostPorts, Boolean investor
            , List<String> LoginIdWebServerUrls) {
        super(brokerId, account, password, hostPorts, investor);
        this.LoginIdWebServerUrls = LoginIdWebServerUrls;
    }

}
