package com.ob.api.mtx.mt4.connector.connection;

import com.ob.api.mtx.mt4.Mt4ApiCredentials;
import lombok.Data;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import static com.ob.broker.util.Util.toInt;

@Data
public class ConnectionData {
//     public static final int CLIENT_EXE_SIZE = 0x872FB00B;
//    public static final short CURRENT_BUILD = 1431;

    public static final int CLIENT_EXE_SIZE = 0xD078F9C4;
    public static final short CURRENT_BUILD = 1440;
    public static final int CURRENT_BUILD_INT = Short.toUnsignedInt(CURRENT_BUILD);
    public final int user;
    public final byte[] key;

    public ConnectionData(int user
            , byte[] key) {
        this.user = user;
        this.key = key;
    }

    public ConnectionData(Mt4ApiCredentials apiCredentials) {
        this.user = toInt(apiCredentials.getAccountId());
        this.key = key(apiCredentials);
    }

    public static byte[] key(Mt4ApiCredentials apiCredentials) {
        byte[] k;
        try {
            final MessageDigest m = MessageDigest.getInstance("MD5");
            k = m.digest(apiCredentials.getPassword().getBytes(StandardCharsets.US_ASCII));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return k;
    }

    public ConnectionData copy() {
        return new ConnectionData(user, key);
    }

}
