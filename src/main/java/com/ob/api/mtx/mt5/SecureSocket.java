package com.ob.api.mtx.mt5;


import com.ob.broker.common.error.Code;
import com.ob.broker.common.error.CodeException;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

public class SecureSocket {

    DataOutputStream Output;
    DataInputStream Input;
    private Socket Sock;

    public SecureSocket() {
    }


    public final void Send(byte[] buf) throws IOException {
        Output.write(buf);
        Output.flush();
    }

    public final byte[] Receive(int count) throws IOException {
        try {
            byte[] buf = new byte[count];
            int rest = buf.length;
            while (rest > 0) {
                int len = Input.read(buf, buf.length - rest, rest);
                if (len == 0) {
                    throw new CodeException("Server disconnected", Code.NETWORK_ERROR);
                } else if (len == -1) {
                    throw new CodeException("Server closed the stream", Code.NETWORK_ERROR);
                } else {
                    rest -= len;
                }
            }
            return buf;
        } catch (Exception e) {
            throw new CodeException("Network problem", Code.NETWORK_ERROR);
        }
    }


    final void Connect(String host, int port) throws IOException {
        Sock = new Socket();
        Sock.setTcpNoDelay(true);
        Sock.setReuseAddress(true);
        Sock.setKeepAlive(true);
        Sock.connect(new InetSocketAddress(host, port), 15000);
        Output = new DataOutputStream(Sock.getOutputStream());
        Input = new DataInputStream(Sock.getInputStream());
    }

    final void Close() throws IOException {
        try {
            if (Input != null) {
                Input.close();
            }
        } catch (Exception e) {
        }
        try {
            if (Output != null) {
                Output.close();
            }
        } catch (Exception e) {
        }
        try {
            if (Sock != null) {
                Sock.close();
            }
            Sock = null;
        } catch (Exception e) {
        }
    }

    int avaliable() throws IOException {
        return Input.available();
    }
}