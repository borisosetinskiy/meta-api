package com.ob.api.mtx.mt4;

import com.ob.broker.common.ApiCredentials;
import com.ob.broker.common.model.HostPort;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@EqualsAndHashCode(callSuper = true)
public class Mt4ApiCredentials extends ApiCredentials {
    final String loginIdExServerUrl;
    final String dataLoginIdServerUrl;
    @Setter
    String brokerName;


    public Mt4ApiCredentials(Long brokerId, Long account
            , String password, List<HostPort> hostPorts, Boolean investor
            , String loginIdExServerUrl
            , String dataLoginIdServerUrl) {
        super(brokerId, account, password, hostPorts, investor);
        this.loginIdExServerUrl = loginIdExServerUrl;
        this.dataLoginIdServerUrl = dataLoginIdServerUrl;
    }

}
