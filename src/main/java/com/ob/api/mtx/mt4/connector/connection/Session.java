package com.ob.api.mtx.mt4.connector.connection;

import com.ob.api.mtx.mt4.Mt4ApiCredentials;
import com.ob.api.mtx.mt4.connector.connection.codec.MT4Crypt;
import com.ob.api.mtx.mt4.connector.entity.dto.ServerDetails;
import com.ob.api.mtx.mt4.connector.util.TimeUtil;
import lombok.Data;

import java.security.NoSuchAlgorithmException;

@Data
public class Session {
    final Mt4ApiCredentials apiCredentials;
    public short serverVersion;
    public short serverBuild;
    public int session;
    public long loginId;
    public long dataLoginId;
    public long loginIdEx;
    public int account;
    public long seed;
    public byte[] transactionKey1 = new byte[16];
    public byte[] transactionKey2 = new byte[16];
    public byte[] hashKey;
    public ServerDetails serverDetails;
    private byte[] hardId;

    public Session(Mt4ApiCredentials apiCredentials) {
        this.apiCredentials = apiCredentials;
    }

    public Session(Session session) {
        this.hardId = session.hardId;
        this.apiCredentials = session.apiCredentials;
    }

    public Session copyAndReset() {
        return new Session(this);
    }

    public byte[] getHardIdByte() throws NoSuchAlgorithmException {
        while (hardId == null || hardId[0] == 0 || hardId[15] == 0) {
            int tickInt = (int) TimeUtil.tick();
            hardId = MT4Crypt.createHardId(tickInt);
        }
        return hardId;
    }


    public String loginIdExServerUrl() {
        return apiCredentials.getLoginIdExServerUrl();
        //cp.loginIdExServerUrl, cp.dataLoginIdServerUrl
    }

    public String dataLoginIdServerUrl() {
        return apiCredentials.getDataLoginIdServerUrl();
    }

}
