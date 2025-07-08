package com.ob.api.mtx.mt4.connector.connection;

public interface Delegate {
    byte[] invoke(byte[] hardId, Session cp, Connection connection);
}
