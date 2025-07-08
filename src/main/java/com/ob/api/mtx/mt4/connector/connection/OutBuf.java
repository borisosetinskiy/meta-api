package com.ob.api.mtx.mt4.connector.connection;

import org.apache.commons.lang3.ArrayUtils;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class OutBuf {
    private List<Byte> list;

    public OutBuf() {
        this.list = new ArrayList<>();
    }

    public OutBuf(byte[] bytes) {
        this.list = new ArrayList<>();
        add(bytes);
    }


    public void createHeader(byte type, int id, boolean compressed) {
        ByteBuffer buffer = ByteBuffer.allocate(9);
        buffer.put(type); // Type
        buffer.putInt(list.size()); // Size
        buffer.putShort((short) id); // ID
        buffer.putShort((short) (compressed ? 3 : 2)); // Flags PHF_COMPLETE
        byte[] hdr = buffer.array();
        List<Byte> tempList = new ArrayList<>();
        for (byte b : hdr) {
            tempList.add(b);
        }
        tempList.addAll(list);
        list = tempList;
    }

    public byte[] toArray() {
        int size = list.size(); // Cache the size
        byte[] bytes = new byte[size];
        for (int i = 0; i < size; i++) {
            bytes[i] = list.get(i); // Direct unboxing to byte
        }
        return bytes;
    }


    ///


    public void add(byte b) {
        list.add(b);
    }

    public void add(int value) {
        ByteBuffer buffer = ByteBuffer.allocate(Integer.BYTES);
        buffer.putInt(value);
        addRevert(buffer.array());
    }

    public void add(short value) {
        ByteBuffer buffer = ByteBuffer.allocate(Short.BYTES);
        buffer.putShort(value);
        addRevert(buffer.array());
    }

    public void add(long value) {
        ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
        buffer.putLong(value);
        addRevert(buffer.array());
    }


    ///


    public void add(double value) {
        ByteBuffer buffer = ByteBuffer.allocate(Double.BYTES);
        buffer.putDouble(value);
        addRevert(buffer.array());
    }

    private void addRevert(byte[] bytes) {
        ArrayUtils.reverse(bytes);
        add(bytes);
    }

    public void add(byte[] bytes) {
        for (byte b : bytes) {
            list.add(b);
        }
    }


    public void byteToBuffer(byte v) {
        add(v);
    }

    public void longToBuffer(long v) {
        add((int) v);
    }

    public void intToBuffer(int v) {
        add(v);
    }

    public void wordToBuffer(int v) {
        add((short) v);
    }

    public void longLongToBuffer(long v) {
        add(v);
    }

    public void dataToBuffer(byte[] v) {
        add(v);
    }

    public void add(String str, int len) {
        byte[] bytes = str != null ? str.getBytes(StandardCharsets.UTF_16LE) : new byte[0];
        if (bytes.length > len * 4) {
            byte[] temp = new byte[len * 4];
            System.arraycopy(bytes, 0, temp, 0, temp.length);
            bytes = temp;
        }
        add(bytes);
    }

    public void add(byte[] ar, int len) {
        byte[] res = new byte[len];
        if (ar != null) {
            System.arraycopy(ar, 0, res, 0, Math.min(ar.length, len));
        }
        add(res);
    }


    // Additional helper methods for conversion or manipulation
    public byte[] toByteArray() {
        byte[] byteArray = new byte[list.size()];
        for (int i = 0; i < list.size(); i++) {
            byteArray[i] = list.get(i);
        }
        return byteArray;
    }

    public void clear() {
        list.clear();
    }

    public int size() {
        return list.size();
    }
}