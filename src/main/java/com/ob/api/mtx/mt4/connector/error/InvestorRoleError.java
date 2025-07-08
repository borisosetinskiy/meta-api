package com.ob.api.mtx.mt4.connector.error;

import com.ob.broker.common.error.Code;
import com.ob.broker.common.error.CodeException;

public class InvestorRoleError extends CodeException {
    public InvestorRoleError() {
        super("Investor password was used", Code.INVESTOR_PASSWORD);
    }
}
