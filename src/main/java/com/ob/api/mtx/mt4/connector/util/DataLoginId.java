package com.ob.api.mtx.mt4.connector.util;

import com.ob.api.mtx.mt4.connector.connection.Session;
import com.ob.broker.common.error.CodeException;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Base64;

import static com.ob.api.mtx.mt4.connector.entity.StatusCode.LOGIN_ID_FAIL;

public class DataLoginId {


    public long decode(byte[] data) {
        long[] d = new long[data.length / 8];
        for (int i = 0; i < d.length; i++) {
            d[i] = BitConverter.ToUInt64(data, i * 8);
        }
        return decode(d, d.length);
    }

    private long decode(long[] pData, int szData) {
        long id = 0;
        for (int i = 0; i < szData; i += 3) {
            byte depth = (byte) (pData[i] >> 14);
            long value = pData[i + 2];
            if (((pData[i + 1] & 0x3FC000) ^ 0x14C000) != 0) {
                id = pData[i + 1];
            }
            switch (depth) {
                case 0x12:
                    id |= value;
                    break;
                case 0x3A:
                    id -= value;
                    break;
                case 0x40:
                    id >>= (int) (value % 24);
                    break;
                case 0x79:
                    id ^= value;
                    break;
                case (byte) 0x8B:
                    id += value;
                    break;
                case (byte) 0xB5:
                    id = (((id & 0xffffffffL) | (((value >> 32) & 0xffffffffL) << 32)));
                    break;
                case (byte) 0xC2:
                    id <<= (int) (value % 24);
                    break;
                case (byte) 0xE1:
                    id &= value;
                    break;
                default:
                    return 0;
            }
        }

        return id;
    }


    public Long decode(Session session, byte[] pack) {
        try {
            String url = session.dataLoginIdServerUrl();
            HttpURLConnection httpURLConnection = (HttpURLConnection) new URL(url).openConnection();
            httpURLConnection.setRequestMethod("POST");
            httpURLConnection.setRequestProperty("Content-Type", "text/plain");
            httpURLConnection.setRequestProperty("charset", "utf-8");
            httpURLConnection.setDoOutput(true);
            httpURLConnection.setDoInput(true);
            httpURLConnection.setConnectTimeout(10000);
            try (DataOutputStream writer
                         = new DataOutputStream(httpURLConnection.getOutputStream())) {
                String req = "loginiddata" + new String(Base64.getEncoder().encode(pack));
                writer.write(req.getBytes());
            }
            try (InputStream reader = httpURLConnection.getInputStream();
                 ByteArrayOutputStream result = new ByteArrayOutputStream();) {
                byte[] buffer = new byte[256];
                int length;
                while ((length = reader.read(buffer)) != -1) {
                    result.write(buffer, 0, length);
                }
                String response = result.toString("UTF-8");
                return new BigDecimal(response).longValue();
            }
        } catch (IOException e) {
            throw new CodeException("DATA_LOGIN_ID:" + e.getMessage(), LOGIN_ID_FAIL.getId());
        }
    }

}


/**
 * public ulong Decode(ulong[] pData, uint szData)
 * {
 * ulong id = 0;
 * for (uint i = 0; i < szData; i += 3)
 * {
 * byte depth = (byte)(pData[i] >> 14);
 * ulong value = pData[i + 2];
 * if (((pData[i + 1] & 0x3FC000) ^ 0x14C000) != 0)
 * {
 * id = pData[i + 1];
 * }
 * switch (depth)
 * {
 * case 0x12:
 * id |= value;
 * break;
 * case 0x3A:
 * id -= value;
 * break;
 * case 0x40:
 * id >>= (int)(value % 24);
 * break;
 * case 0x79:
 * id ^= value;
 * break;
 * case 0x8B:
 * id += value;
 * break;
 * case 0xB5:
 * id = (ulong)((long)((uint)((((uint)(((long)(id)) & 0xffffffff)))) | (((long)((((uint)((((long)(value)) >> 32) & 0xffffffff))))) << 32)));
 * break;
 * case 0xC2:
 * id <<= (int)(value % 24);
 * break;
 * case 0xE1:
 * id &= value;
 * break;
 * default:
 * return 0;
 * }
 * }
 * return id;
 * }
 */