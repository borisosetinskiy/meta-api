package com.ob.api.dx;

import com.ob.broker.common.ApiCredentials;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@EqualsAndHashCode(callSuper = true)
public class DxApiCredentials extends ApiCredentials {
    String userName;

    String domain = "default";

    public DxApiCredentials(String userName, Long brokerId, Object accountId
            , String password) {
        super(brokerId, accountId, password, null, false);
        this.userName = userName;
    }
}
