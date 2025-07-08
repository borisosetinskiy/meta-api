package com.ob.api.mtx.mt4.connector.connection;

import com.ob.api.mtx.mt4.connector.connection.codec.DecoderJava;
import com.ob.api.mtx.mt4.connector.connection.codec.Decompressor;
import com.ob.api.mtx.mt4.connector.connection.codec.EncoderJava;
import com.ob.api.mtx.mt4.connector.connection.codec.MT4Crypt;
import com.ob.api.mtx.mt4.connector.entity.StatusCode;
import com.ob.api.mtx.mt4.connector.error.*;
import com.ob.api.mtx.mt4.connector.util.ByteUtil;
import com.ob.broker.common.error.CodeException;
import lombok.Data;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static com.ob.api.mtx.mt4.connector.connection.ConnectionUtil.createAccountRequest;
import static com.ob.api.mtx.mt4.connector.util.ArrayUtil.arrByte;

@Data
public class Connection {

    final static byte[] DISCONNECT = new byte[]{0xD};
    final static byte[] PING = new byte[]{0x2};
    private static final int CONNECT_TIMEOUT = 10_000;
    private static final int READ_TIMEOUT = 30_000;
    private static final byte[] SERVER_REQUEST = new byte[]{0x10};
    private static final byte[] SYMBOL_GET_REQUEST = new byte[]{0x8, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
    private DecoderJava decoder;
    private EncoderJava encoder;
    private AdaptiveSocket sock;
    private Lock readLock = new ReentrantLock();
    private Lock writeLock = new ReentrantLock();
    private String host;
    private int port;

    public Connection() {
    }

    public synchronized void connect(String host, int port) throws Exception {
        this.host = host;
        this.port = port;

        if (decoder != null)
            decoder.reset();
        if (encoder != null)
            encoder.reset();
        close();
        sock = new AdaptiveSocket();
        sock.connect(host, port, CONNECT_TIMEOUT, READ_TIMEOUT);


    }

    public synchronized void disconnect() {
        try {
            send(DISCONNECT);
        } catch (Exception ignore) {
        }
        close();
    }

    public void send(final byte[] buf) throws IOException {
        checkConnect();
        writeLock.lock();
        try {
            final byte[] out = encoder.encode(buf);
            write(out);
        } finally {
            writeLock.unlock();
        }

    }

    void checkConnect() {
        if (!isConnected()) throw new NotConnectedException();
    }

    public boolean isConnected() {
        return sock != null && !sock.isClosed();
    }

    public synchronized void close() {
        try {
            if (sock != null) {
                sock.close();
            }
        } catch (Exception ignored) {
        } finally {
            sock = null;
        }
    }

    public void setNewKey(byte[] key) {
        if (decoder == null)
            decoder = new DecoderJava(key);
        else
            decoder.changeKey(key);
        if (encoder == null)
            encoder = new EncoderJava(key);
        else
            encoder.changeKey(key);
    }

    public byte[] getKey() {
        if (decoder != null)
            return decoder.getKey();
        if (encoder != null)
            return encoder.getKey();
        return null;
    }

    public void ping() throws IOException {
        send(PING);
    }

    public byte[] receiveAccount(Session session) throws Exception {
        checkConnect();

        OutBuf buf = new OutBuf();
        buf.byteToBuffer((byte) 0x1E); // Equivalent to buf.ByteToBuffer(0x1E);
        buf.intToBuffer(0); // Equivalent to buf.IntToBuffer(0);
        buf.add(createAccountRequest(session.session)); // Assuming createAccountRequest() is implemented based on previous discussions

        // Placeholder for SendEncode - actual implementation will depend on specific requirements
        send(buf.toByteArray()); // Assuming this method is implemented to send the encoded buffer

        // Placeholder for ReceiveDecode - assuming it returns a byte array
        byte[] res = receiveDecode(1); // Assuming this method is implemented to receive and decode the response

        if (res[0] == 0) {
            // Placeholder for ReceiveCompressed - assuming it returns a byte array
            return readCompressed(); // Assuming this method is implemented to handle compressed data reception
        } else {
            var statusCode = StatusCode.getById(res[0]);
            throw new CodeException(StatusCode.codeByNumber(statusCode.getId()));
        }


    }


    public byte[] receiveMailHistory() throws Exception {
        send(new byte[]{0x21});
        byte[] buf = receiveDecode(1);
        if (buf[0] == 0) {
            buf = receiveDecode(4);
            int numRecords = ByteUtil.getInt(buf, 0);
            if ((numRecords < 1) || (numRecords > 1024))
                return null;
            return receiveDecode(numRecords * 0x44);
        }
        return null;

    }

    final static byte[] RECEIVE_GROUP = new byte[]{0xA, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};

    public byte[] receiveGroups() throws Exception {
        checkConnect();
        send(RECEIVE_GROUP);
        byte[] buf = receiveDecode(1);
        if (buf[0] == 0)
            return readCompressed();
        else {
            var statusCode = StatusCode.getById(buf[0]);
            throw new CodeException(StatusCode.codeByNumber(statusCode.getId()));
        }
    }

    public byte[] receiveServersList() throws Exception {
        send(SERVER_REQUEST);
        byte[] buf = receiveDecode(1);
        if (buf[0] == 0) {
            receiveDecode(1);
            return readCompressed();
        } else
            throw new NonZeroReplyException();

    }

    public byte[] receiveSymbols() throws Exception {

        checkConnect();
        send(SYMBOL_GET_REQUEST);
        byte[] buf = receiveDecode(1);
        if (buf[0] == 0)
            return readCompressed();
        else {
            if (buf[0] == 1)
                throw new TradesNotAllowedError();
            var statusCode = StatusCode.getById(buf[0]);
            throw new CodeException(StatusCode.codeByNumber(statusCode.getId()));
        }


    }


    public void resetEncoder() {
        encoder.reset();
        decoder.reset();
    }

    public void sendNoCrypt(byte[] buf) throws IOException {
        checkConnect();
        write(buf);
    }


    private void write(byte[] buf) throws IOException {
        final OutputStream dOut = sock.getDataOutputStream();
        dOut.write(buf);
        dOut.flush();
    }

    public void sendEasyCrypt(byte[] buf) throws IOException {
        checkConnect();
        buf = createEasyCrypt(buf);
        write(buf);
    }

    byte[] createEasyCrypt(byte[] buf) {
        byte value = 0;
        for (int i = 1; i < buf.length; i++) {
            value = (byte) ((Byte.toUnsignedInt(value) + Byte.toUnsignedInt(MT4Crypt.CryptKey[(i - 1) & 0xF])) ^ buf[i]);
            buf[i] = value;
        }
        return buf;
    }

    private int read(byte[] buf, final int size) throws Exception {
        int rest = size;
        int read = 0;
        final DataInputStream dIn = sock.getDataInputStream();
        int len;
        while (rest > 0 && (len = dIn.read(buf, size - rest, rest)) != -1) {
            if (len == 0)
                throw new ServerSocketClosed();
            else {
                rest -= len;
                read += len;
            }
        }
        return read;
    }

    public byte[] receive(final int size) throws Exception {
        byte[] buf = arrByte(size);
        readLock.lock();
        try {
            read(buf, size);
            return buf;
        } finally {
            readLock.unlock();
        }
    }


    public byte[] receiveDecode(int len) throws Exception {
        byte[] buf = receive(len);
        decoder.decode(buf);
        return buf;
    }

    public byte[] readCompressed() throws Exception {
        byte[] buf = receiveDecode(4);
        int decompressSize = ByteUtil.getInt(buf, 0);
        if (decompressSize <= 0)
            throw new ArraySizeError(decompressSize);
        buf = receiveDecode(4);
        int len = ByteUtil.getInt(buf, 0);
        buf = receiveDecode(len);
        buf = Decompressor.decompress(buf, decompressSize);
        return buf;
    }


    public int available() throws IOException {
        DataInputStream dataInputStream;
        if (sock == null
            || sock.isInputShutdown()
            || (dataInputStream = sock.getDataInputStream()) == null
        ) {
            throw new NotConnectedException();
        }
        return dataInputStream.available();
    }


}
