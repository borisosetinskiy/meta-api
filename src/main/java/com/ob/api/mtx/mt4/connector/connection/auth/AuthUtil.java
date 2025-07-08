package com.ob.api.mtx.mt4.connector.connection.auth;

import com.ob.api.mtx.mt4.connector.connection.*;
import com.ob.api.mtx.mt4.connector.connection.codec.MT4Crypt;
import com.ob.api.mtx.mt4.connector.connection.codec.vAESJava;
import com.ob.api.mtx.mt4.connector.entity.StatusCode;
import com.ob.api.mtx.mt4.connector.error.NotConnectedException;
import com.ob.api.mtx.mt4.connector.error.OldServerException;
import com.ob.api.mtx.mt4.connector.util.*;
import com.ob.broker.common.error.Code;
import com.ob.broker.common.error.CodeException;
import com.ob.broker.common.model.HostPort;

import java.security.NoSuchAlgorithmException;

import static com.ob.api.mtx.mt4.connector.connection.ConnectionUtil.modifyKeySHA1MTComplex;

public class AuthUtil {
    public static void connectAndLogin(HostPort hostPort, ConnectionWrapper connectionWrapper) throws Exception {
        Connection connection = connectionWrapper.getConnection();
        ConnectionData cp = connectionWrapper.getConnectionData();
        try {
            connection.connect(hostPort.getHost(), hostPort.getPort());
            Session session = connectionWrapper.getSession();
            byte[] key = cp.key;
            connection.setNewKey(key);
            byte[] send = ConnectionUtil.createLoginRequest(key, cp);
            connection.sendEasyCrypt(send);
            byte[] buf = connection.receive(1);
            if (buf[0] != 0) {
                var statusCode = StatusCode.getById(buf[0]);
                throw new CodeException(StatusCode.codeByNumber(statusCode.getId()));
            }
            connection.resetEncoder();
            connection.receiveDecode(1);
            buf = connection.receiveDecode(2);
            session.serverBuild = ByteUtil.getShort(buf, 0);
            buf = connection.receiveDecode(4);
            session.session = ByteUtil.getInt(buf, 0);
            session.account = cp.user;
            if (session.serverBuild < 1010) {
                if (session.serverBuild < 0) {
                    throw new CodeException(Code.SERVER_BUILD);
                }
                throw new OldServerException("Old version of server " + session.serverBuild);
            }
            if (session.session == 0)
                throw new NotConnectedException("No session");
            buf = connection.receiveDecode(64);
            long seed = (key[3] + key[2] + key[1] + key[0] +
                         ((session.session >>> 8) & 0xFF) + session.account + ConnectionData.CURRENT_BUILD_INT) & ConnectionUtil.MAX_INT_VALUE;
            byte[] hashKey = key;
            hashKey = modifyHashKey(seed, buf, hashKey, session);
            loginIdentification(connection, session, hashKey);
            connection.setNewKey(hashKey);
            byte[] packet = terminalIdentification(connection, session, hashKey);
            connection.sendNoCrypt(packet);
        } catch (Exception e) {
            connection.close();
            throw e;
        }
    }


    private static byte[] modifyHashKey(long seed, byte[] data, byte[] hashKey, Session cp) throws NoSuchAlgorithmException {
        for (int i = 0; i < 8; i++) {
            seed = seed * 214013 + 2531011;
            seed &= ConnectionUtil.MAX_INT_VALUE;
            int action = (int) ((seed >>> 16) & 0xF);
            switch (action) {
                case 9:         // xor EasyCriptKey
                    hashKey = ConnectionUtil.modifyKeyEasyKey(hashKey);
                    break;
                case 10:        // swap all bytes
                    hashKey = ConnectionUtil.modifyKeySwapAllBytes(hashKey);
                    break;
                case 11:        // crypt MD5 MTComplex
                    hashKey = ConnectionUtil.modifyKeyMD5MTComplex(data, hashKey, cp);
                    break;
                case 3:         // crypt MD5 MTRandom
                    hashKey = ConnectionUtil.modifyKeyMD5MTRandom(data, hashKey);
                    break;
                case 4:         // swap pair bytes
                    hashKey = ConnectionUtil.modifyKeySwapPair(hashKey);
                    break;
                case 13:        // not all
                    hashKey = ConnectionUtil.modifyKeyNotBytes(hashKey);
                    break;
                case 14:        // crypt SHA-1 MTComplex
                    hashKey = modifyKeySHA1MTComplex(data, hashKey, cp);
                    break;
                case 5:         // crypt SHA-1 MTType
                    hashKey = ConnectionUtil.modifyKeySHA1MTType(hashKey);
                    break;
                case 6:         // not even bytes
                    hashKey = ConnectionUtil.modifyKeyNotEven(hashKey);
                    break;
                case 7:         // not odd bytes
                    hashKey = ConnectionUtil.modifyKeyNotOdd(hashKey);
                    break;
                case 0:        // crypt SHA-256 MTComplex
                    hashKey = ConnectionUtil.modifyKeySHA256MTComplex(data, hashKey, cp);
                    break;
                case 1:        // crypt SHA-256 MTLogin
                    hashKey = ConnectionUtil.modifyKeySHA256MTLogin(hashKey, cp);
                    break;
                case 2:        // crypt SHA-256 MTType
                    hashKey = ConnectionUtil.modifyKeySHA256MTType(hashKey);
                    break;
                case 8:        // crypt SHA-256 MTBuild
                    hashKey = ConnectionUtil.modifyKeySHA256MTBuild(hashKey);
                    break;
                case 12:        // crypt SHA-256 MTRandom
                    hashKey = ConnectionUtil.modifyKeySHA256MTRandom(data, hashKey);
                    break;
                case 15:        // crypt SHA-256 MTSession
                    hashKey = ConnectionUtil.modifyKeySHA256MTSession(hashKey, cp);
                    break;
            }
        }
        return hashKey;
    }

    private static byte[] terminalIdentification(Connection connection, Session cp, byte[] hashKey) throws NoSuchAlgorithmException {
        cp.seed = (Integer.toUnsignedLong(cp.account) + Integer.toUnsignedLong(cp.session));
        cp.seed &= ConnectionUtil.MAX_INT_VALUE;
        byte[] rnd = new byte[64];
        for (int i = 0; i < 64; i++) {
            cp.seed = cp.seed * 214013 + 2531011;
            cp.seed &= ConnectionUtil.MAX_INT_VALUE;
            rnd[i] = (byte) ((cp.seed >>> 16) & 0xFFL);
        }

        cp.hashKey = modifyKeySHA1MTComplex(rnd, hashKey, cp);
        byte[] packKey = new byte[cp.hashKey.length];
        System.arraycopy(cp.hashKey, 0, packKey, 0, cp.hashKey.length);
        boolean bOldBuild = cp.serverBuild <= 1101;
        Delegate[] pfnCodes = new Delegate[9];
        pfnCodes[0] = new DataPacket();
        pfnCodes[1] = new HashPacket();
        int ind = 2;
        if (!bOldBuild)
            pfnCodes[ind++] = new LoginIdPacket();
        pfnCodes[ind++] = new ModifyKey();
        pfnCodes[ind++] = new RandomPacket();
        pfnCodes[ind++] = new ModifyKey();
        pfnCodes[ind++] = new RandomPacket();
        pfnCodes[ind++] = new ModifyKey();
        pfnCodes[ind++] = new RandomPacket();


        cp.seed = cp.seed * 214013 + 2531011;
        cp.seed &= ConnectionUtil.MAX_INT_VALUE;
        int step = (int) (((cp.seed >>> 16) & 3) + (bOldBuild ? 4 : 5));
        for (int i = 0; i < 128; i++) {
            cp.seed = cp.seed * 214013 + 2531011;
            cp.seed &= ConnectionUtil.MAX_INT_VALUE;
            int ind1 = (int) (((cp.seed >>> 16) & 0x7FFFL) % step);
            cp.seed = cp.seed * 214013 + 2531011;
            cp.seed &= ConnectionUtil.MAX_INT_VALUE;
            int ind2 = (int) (((cp.seed >>> 16) & 0x7FFFL) % step);
            Delegate pfnTmp = pfnCodes[ind1];
            pfnCodes[ind1] = pfnCodes[ind2];
            pfnCodes[ind2] = pfnTmp;
        }
        byte[] resizedData = new byte[0];
        for (int i = 0; i < step; i++) {
            byte[] pack = pfnCodes[i].invoke(cp.getHardIdByte(), cp, connection);
            if (pack != null) {
                if (resizedData.length == 0)
                    resizedData = pack;
                else {
                    int oldSize = resizedData.length;
                    byte[] tempData = new byte[oldSize + pack.length];
                    System.arraycopy(resizedData, 0, tempData, 0, resizedData.length);
                    System.arraycopy(pack, 0, tempData, oldSize, pack.length);
                    resizedData = tempData;
                }
            }
        }
        byte[] hdr = new byte[8];
        byte[] res = ByteArrayUtil.shortToByteArray(((short) resizedData.length));
        System.arraycopy(res, 0, hdr, 0, res.length);
        hdr[2] = 2;
        hdr[4] = 1;
        cp.seed = cp.seed * 214013 + 2531011;
        cp.seed &= ConnectionUtil.MAX_INT_VALUE;
        res = ByteArrayUtil.shortToByteArray(((short) ((cp.seed >>> 16) & 0x7FFFL)));
        System.arraycopy(res, 0, hdr, 6, res.length);
        res = ConnectionUtil.cryptPacket(hdr, resizedData, packKey);
        byte[] packet = new byte[res.length + 1];
        packet[0] = 0x18;
        System.arraycopy(res, 0, packet, 1, res.length);
        return packet;
    }

    private static void loginIdentification(Connection connection, Session session, byte[] hashKey) throws Exception {

        session.loginId = 0;
        if (session.serverBuild <= 1101)
            return;
        PacketHdr hdr = new PacketHdr();
        byte[] buf = connection.receiveDecode(8);
        hdr.sizeData = ByteUtil.getShort(buf, 0);
        hdr.packType = ByteUtil.getShort(buf, 2);
        hdr.dataType = ByteUtil.getShort(buf, 4);
        hdr.random = ByteUtil.getShort(buf, 6);

        byte[] data = connection.receiveDecode(hdr.sizeData);
        byte[] multipack = decryptPacket(data, hashKey, hdr);
        int start = 0;
        while ((multipack.length - start >= 8)) {
            byte[] hdrBytes = new byte[8];
            System.arraycopy(multipack, start, hdrBytes, 0, 8);
            start += 8;
            hdr = decryptPacketHdr(hdrBytes, hashKey);
            if (multipack.length - start < hdr.sizeData)
                break;
            byte[] packBytes = new byte[hdr.sizeData];
            System.arraycopy(multipack, start, packBytes, 0, hdr.sizeData);
            start += hdr.sizeData;
            byte[] pack = decryptPacket(packBytes, hashKey, hdr);
            if (hdr.packType == 9) {
                if (session.serverBuild >= 1435) {
                    session.loginId = LoginIdWebServer.decode(session.loginIdExServerUrl(), pack, "/DecodeNew");
                } else {
                    session.loginId = new LoginId().Decode(pack);
                }
                session.loginId ^= (Integer.toUnsignedLong(session.account) << 32) + session.session;
                session.loginId ^= 0x05286AED3286692AL;
            } else if (hdr.packType == 11) {
                session.dataLoginId = new DataLoginId().decode(session, pack);
                session.dataLoginId ^= (Integer.toUnsignedLong(session.account) << 32) + session.session;
                session.dataLoginId ^= 0x05286AED3286692AL;
            } else if (hdr.packType == 15) {
                session.loginIdEx = LoginIdWebServer.decode(session.loginIdExServerUrl(), pack, "/DecodeEx");
                session.loginIdEx ^= (Integer.toUnsignedLong(session.account) << 32) + session.session;
                if (session.serverBuild >= 1435) {
                    session.loginIdEx ^= 0x4367468243443L;
                }
                session.loginIdEx ^= 0x05286AED3286692AL;
            }

        }
    }


    public static PacketHdr decryptPacketHdr(byte[] data, byte[] key) {
        byte[] res = MT4Crypt.decode(data, key);
        PacketHdr hdr = new PacketHdr();
        hdr.sizeData = ByteUtil.getShort(res, 0);
        hdr.packType = ByteUtil.getShort(res, 2);
        hdr.dataType = ByteUtil.getShort(res, 4);
        hdr.random = ByteUtil.getShort(res, 6);
        return hdr;
    }

    public static byte[] decryptPacket(byte[] data, byte[] key, PacketHdr hdr) {
        if (hdr.dataType == 0)
            return MT4Crypt.decode(data, key);
        else if (hdr.dataType == 1) {
            vAESJava aes = new vAESJava();
            int szData = hdr.sizeData & ~0xF;
            int szTail = hdr.sizeData - szData;
            byte[] dat = new byte[szData];
            System.arraycopy(data, 0, dat, 0, szData);
            byte[] res = aes.decryptData(dat, key);
            byte[] tail = new byte[szTail];
            System.arraycopy(data, szData, tail, 0, szTail);
            byte[] tailDecrypt = MT4Crypt.decode(tail, key);
            int len = res.length;


            byte[] resizedResult = new byte[len + tailDecrypt.length];
            System.arraycopy(res, 0, resizedResult, 0, len);
            System.arraycopy(tailDecrypt, 0, resizedResult, len, tailDecrypt.length);

            return resizedResult;
        }
        return data;
    }
}
