package com.ob.api.mtx.mt4.connector.connection;

import lombok.Getter;

@Getter
public class ConnectionWrapper {
    private final ConnectionData connectionData;
    private final Session session;
    private Connection connection;

    public ConnectionWrapper(ConnectionData connectionData, Session session) {
        this.connectionData = connectionData;
        this.session = session;
    }

    public ConnectionWrapper copyAndReset() {
        ConnectionWrapper connectionWrapper = new ConnectionWrapper(this.getConnectionData().copy(), this.getSession().copyAndReset());
        connectionWrapper.resetConnection();
        return connectionWrapper;
    }

    public void resetConnection() {
        try {
            if (connection != null) {
                connection.close();
            }
        } catch (Exception ignored) {
        }
        this.connection = new Connection();
    }
}
