package com.ob.api.mtx.mt4.connector.util;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

public class UDT {


    public static String readString(byte[] buf, int of, int len) {
        ArrayList<Byte> res = new ArrayList<Byte>();
        for (int i = 0; i < len; i += 2) {
            if (buf[of + i] == 0 && buf[of + i + 1] == 0)
                break;
            res.add(buf[of + i]);
            res.add(buf[of + i + 1]);
        }
        byte[] array = new byte[res.size()];
        for (int i = 0; i < res.size(); i++) array[i] = res.get(i);
        try {
            return new String(array, StandardCharsets.UTF_16LE.toString());
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    public static String readStringASCII(byte[] buf, int of, int len) {
        ArrayList<Byte> res = new ArrayList<Byte>();
        for (int i = 0; i < len; i += 1) {
            if (buf[of + i] == 0) // && buf[of + i + 1] == 0
                if (i > 0)
                    break;
            res.add(buf[of + i]);
        }
        if (res.size() > 0)
            if (res.get(0) == 0)
                res.remove(0);
        byte[] array = new byte[res.size()];
        for (int i = 0; i < res.size(); i++) array[i] = res.get(i);
        String r = null;
        try {
            r = new String(array, StandardCharsets.US_ASCII);
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
        return r;
    }

}