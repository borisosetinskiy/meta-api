package com.ob.api.mtx.mt4.connector.connection;

import lombok.Getter;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

@Getter
public class AdaptiveSocket extends Socket {
    private DataInputStream dataInputStream;
    private DataOutputStream dataOutputStream;

    public AdaptiveSocket() throws Exception {
        this.setTcpNoDelay(true);
        this.setReuseAddress(true);
        this.setKeepAlive(true);
    }

    public synchronized void connect(String remoteHost, int remotePort
            , int connectTimeout, int readTimeout) throws IOException {
        InetSocketAddress remoteAddr = new InetSocketAddress(remoteHost, remotePort);
        this.setSoTimeout(readTimeout);
        try {
            connect(remoteAddr, connectTimeout);
            dataInputStream = new DataInputStream(this.getInputStream());
            dataOutputStream = new DataOutputStream(this.getOutputStream());
        } catch (Exception e) {
            try {
                close();
            } catch (IOException ignored) {
            }
            throw e;
        }
    }

}
