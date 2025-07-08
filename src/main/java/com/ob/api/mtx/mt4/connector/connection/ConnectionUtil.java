package com.ob.api.mtx.mt4.connector.connection;

import com.ob.api.mtx.mt4.connector.connection.codec.MT4Crypt;
import com.ob.api.mtx.mt4.connector.connection.codec.vAESJava;
import com.ob.api.mtx.mt4.connector.connection.codec.vSHA1Java;
import com.ob.api.mtx.mt4.connector.util.ByteArrayUtil;
import com.ob.api.mtx.mt4.connector.util.ByteUtil;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Random;

import static com.ob.api.mtx.mt4.connector.util.BitConverter.updateRandom;

public class ConnectionUtil {
    public static final long MAX_INT_VALUE = 0xFFFFFFFFL;
    final static byte[] MT_COMPLEX = "MTComplex".getBytes(StandardCharsets.US_ASCII);
    final static byte[] MT_LOGIN = "MTLogin".getBytes(StandardCharsets.US_ASCII);
    final static byte[] MT_TYPE = "MTType".getBytes(StandardCharsets.US_ASCII);
    final static byte[] MT_BUILD = "MTBuild".getBytes(StandardCharsets.US_ASCII);
    final static byte[] MT_RANDOM = "MTRandom".getBytes(StandardCharsets.US_ASCII);
    final static byte[] MT_SESSION = "MTSession".getBytes(StandardCharsets.US_ASCII);

    public static byte[] modifyKeyEasyKey(byte[] hashKey) {
        byte last = 0;
        byte[] key = new byte[16];
        for (int i = 0; i < 16; i++) {
            key[i] = (byte) (hashKey[i] ^ (Byte.toUnsignedInt(MT4Crypt.CryptKey[i]) + Byte.toUnsignedInt(last)));
            last = key[i];
        }
        return key;
    }

    public static byte[] modifyKeySwapAllBytes(byte[] hashKey) {
        byte[] key = new byte[16];
        for (int i = 0; i < 16; i++)
            key[i] = hashKey[15 - i];
        return key;
    }

    public static byte[] modifyKeyMD5MTComplex(byte[] rcvData, byte[] hashKey, Session cp) throws NoSuchAlgorithmException {
        byte[] data = new byte[97];
        data[0] = 0;
        byte[] bytes = MT_COMPLEX;
        System.arraycopy(bytes, 0, data, 1, bytes.length);
        ByteArrayUtil.copyShortToByteArray(ConnectionData.CURRENT_BUILD, data, 11);
        System.arraycopy(hashKey, 0, data, 13, hashKey.length);
        System.arraycopy(rcvData, 0, data, 29, rcvData.length);
        ByteArrayUtil.copyIntToByteArray(cp.account, data, 93);
        MessageDigest m = MessageDigest.getInstance("MD5");
        byte[] res = m.digest(data);
        return res;
    }

    public static byte[] modifyKeyMD5MTRandom(byte[] rcvData, byte[] hashKey) throws NoSuchAlgorithmException {
        byte[] data = new byte[89];
        System.arraycopy(hashKey, 0, data, 0, hashKey.length);
        System.arraycopy(rcvData, 0, data, 16, rcvData.length);
        byte[] bytes = MT_RANDOM;
        System.arraycopy(bytes, 0, data, 80, bytes.length);
        MessageDigest m = MessageDigest.getInstance("MD5");
        byte[] res = m.digest(data);
        return res;
    }

    public static byte[] modifyKeySwapPair(byte[] hashKey) {
        byte[] key = new byte[16];
        for (int i = 0; i < 16; i += 2) {
            key[i] = hashKey[i + 1];
            key[i + 1] = hashKey[i];
        }
        return key;
    }

    public static byte[] modifyKeyNotBytes(byte[] hashKey) {
        byte[] key = new byte[16];
        for (int i = 0; i < 16; i++)
            key[i] = (byte) (~hashKey[i]);
        return key;
    }

    public static byte[] modifyKeySHA1MTComplex(byte[] rcvData, byte[] hashKey, Session cp) {
        byte[] data = new byte[97];
        data[0] = 0;                                            //type (0 - client, etc)
        ByteArrayUtil.copyShortToByteArray(ConnectionData.CURRENT_BUILD, data, 1);
        System.arraycopy(rcvData, 0, data, 3, rcvData.length);
        System.arraycopy(hashKey, 0, data, 67, hashKey.length);
        ByteArrayUtil.copyIntToByteArray(cp.account, data, 83);
        byte[] bytes = MT_COMPLEX;
        System.arraycopy(bytes, 0, data, 87, bytes.length);
        return getKeySHA1(data);
    }

    public static byte[] modifyKeySHA1MTType(byte[] hashKey) {
        byte[] data = new byte[24];
        byte[] bytes = MT_TYPE;
        System.arraycopy(bytes, 0, data, 0, bytes.length);
        System.arraycopy(hashKey, 0, data, 7, hashKey.length);
        data[23] = 0;
        return getKeySHA1(data);
    }

    public static byte[] modifyKeyNotEven(byte[] hashKey) {
        byte[] key = new byte[16];
        for (int i = 0; i < 16; i += 2) {
            key[i] = (byte) (~hashKey[i]);
            key[i + 1] = hashKey[i + 1];
        }
        return key;
    }

    public static byte[] modifyKeyNotOdd(byte[] hashKey) {
        byte[] key = new byte[16];
        for (int i = 0; i < 16; i += 2) {
            key[i] = hashKey[i];
            key[i + 1] = (byte) (~hashKey[i + 1]);
        }
        return key;
    }

    public static byte[] modifyKeySHA256MTComplex(byte[] rcvData, byte[] hashKey, Session cp) throws NoSuchAlgorithmException {
        byte[] data = new byte[97];
        data[0] = 0;
        byte[] bytes = MT_COMPLEX;
        System.arraycopy(bytes, 0, data, 1, bytes.length);
        System.arraycopy(rcvData, 0, data, 11, rcvData.length);
        ByteArrayUtil.copyShortToByteArray(ConnectionData.CURRENT_BUILD, data, 75);
        System.arraycopy(hashKey, 0, data, 77, hashKey.length);
        ByteArrayUtil.copyIntToByteArray(cp.account, data, 93);
        return getKeySHA256(data);
    }


    public static byte[] modifyKeySHA256MTLogin(byte[] hashKey, Session cp) throws NoSuchAlgorithmException {
        byte[] data = new byte[28];
        ByteArrayUtil.copyIntToByteArray(cp.account, data, 0);
        System.arraycopy(hashKey, 0, data, 4, hashKey.length);
        byte[] bytes = MT_LOGIN;
        System.arraycopy(bytes, 0, data, 20, bytes.length);
        return getKeySHA256(data);
    }


    public static byte[] modifyKeySHA256MTType(byte[] hashKey) throws NoSuchAlgorithmException {
        byte[] data = new byte[24];
        data[0] = 0;
        System.arraycopy(hashKey, 0, data, 1, hashKey.length);
        byte[] bytes = MT_TYPE;
        System.arraycopy(bytes, 0, data, 17, bytes.length);
        return getKeySHA256(data);
    }


    public static byte[] modifyKeySHA256MTBuild(byte[] hashKey) throws NoSuchAlgorithmException {
        byte[] data = new byte[26];
        ByteArrayUtil.copyShortToByteArray(ConnectionData.CURRENT_BUILD, data, 0);
        System.arraycopy(hashKey, 0, data, 2, hashKey.length);
        byte[] bytes = MT_BUILD;
        System.arraycopy(bytes, 0, data, 18, bytes.length);
        return getKeySHA256(data);
    }


    public static byte[] modifyKeySHA256MTRandom(byte[] rcvData, byte[] hashKey) throws NoSuchAlgorithmException {
        byte[] data = new byte[89];
        byte[] bytes = MT_RANDOM;
        System.arraycopy(bytes, 0, data, 0, bytes.length);
        System.arraycopy(hashKey, 0, data, 9, hashKey.length);
        System.arraycopy(rcvData, 0, data, 25, rcvData.length);
        return getKeySHA256(data);
    }


    public static byte[] modifyKeySHA256MTSession(byte[] hashKey, Session cp) throws NoSuchAlgorithmException {
        byte[] data = new byte[30];
        ByteArrayUtil.copyIntToByteArray(cp.session, data, 0);
        System.arraycopy(hashKey, 0, data, 4, hashKey.length);
        byte[] bytes = MT_SESSION;
        System.arraycopy(bytes, 0, data, 20, bytes.length);
        return getKeySHA256(data);
    }

    public static byte[] getKeySHA1(byte[] data) {
        vSHA1Java sha = new vSHA1Java();
        byte[] shaKey = sha.computeHash(data);
        byte[] key = new byte[16];
        System.arraycopy(shaKey, 0, key, 0, 16);
        return key;
    }

    public static byte[] getKeySHA256(byte[] data) throws NoSuchAlgorithmException {
        MessageDigest m = MessageDigest.getInstance("SHA-256");
        byte[] shaKey = m.digest(data);
        byte[] key = new byte[16];
        System.arraycopy(shaKey, 0, key, 0, 16);
        return key;
    }

    public static byte[] cryptPacket(byte[] hdr, byte[] data, byte[] hashKey) {
        byte[] endata = new byte[data.length];
        byte[] enhdr = new byte[hdr.length];
        int length = Short.toUnsignedInt(ByteUtil.getShort(hdr, 0));
        boolean bData = (length >= 1) && (data.length != 0);
        if (bData) {
            short typeData = ByteUtil.getShort(hdr, 4);
            if (typeData == 0)
                endata = MT4Crypt.encode(data, hashKey);
            else if (typeData == 1) {
                vAESJava aes = new vAESJava();
                int szData = (length & ~0xF);
                byte[] idata = new byte[szData];
                System.arraycopy(data, 0, idata, 0, szData);
                byte[] iendata = aes.encryptData(idata, hashKey);
                byte[] rdata = new byte[length - szData];
                System.arraycopy(data, szData, rdata, 0, length - szData);
                byte[] rendata = MT4Crypt.encode(rdata, hashKey);
                endata = new byte[iendata.length + rendata.length];
                System.arraycopy(iendata, 0, endata, 0, iendata.length);
                System.arraycopy(rendata, 0, endata, iendata.length, rendata.length);
            }
        }
        int typeHdr = ByteUtil.getShort(hdr, 2);
        if (typeHdr == 2)
            System.arraycopy(hdr, 0, enhdr, 0, hdr.length);
        else
            enhdr = MT4Crypt.encode(hdr, hashKey);
        if (!bData)
            return enhdr;
        byte[] encode = new byte[enhdr.length + endata.length];
        System.arraycopy(enhdr, 0, encode, 0, enhdr.length);
        System.arraycopy(endata, 0, encode, enhdr.length, endata.length);

        return encode;
    }

    public static byte[] createLoginRequest(byte[] key, ConnectionData cp) {
        final byte[] login = new byte[28];
        final byte[] encodedKey = MT4Crypt.decode(key, key);
        login[0] = (byte) 0;               //login request (0 - trade, 0xB - data center)
        login[1] = (byte) (Byte.toUnsignedInt(login[0]) + Integer.toUnsignedLong(cp.user)
                           + Byte.toUnsignedInt(login[27])
                           + Short.toUnsignedInt(ConnectionData.CURRENT_BUILD));    //check code
        System.arraycopy(encodedKey, 0, login, 3, encodedKey.length);
        ByteArrayUtil.copyIntToByteArray(cp.user, login, 19);
        ByteArrayUtil.copyShortToByteArray((short) 400, login, 23);
        ByteArrayUtil.copyShortToByteArray(ConnectionData.CURRENT_BUILD, login, 25);
        return login;
    }

    public static byte[] createAccountRequest(int session) {
        OutBuf buf = new OutBuf();
        buf.longLongToBuffer(338067);// Equivalent to buf.LongLongToBuffer(0);
        buf.intToBuffer(2); // Equivalent to buf.IntToBuffer(2);

        Random rnd = new Random();
        byte[] ur = updateRandom(session, 48); // Assuming 'session' is defined elsewhere and updateRandom method is implemented
        buf.add(ur);

        for (int i = 0; i < 12; i++) {
            buf.add((byte) rnd.nextInt(256)); // Note: rnd.Next(0, 255) in C# is equivalent to rnd.nextInt(256) in Java because upper bound is exclusive in Java
        }

        byte[] ap = getApplicationInfo();
        buf.add((short) ap.length);
        buf.add((short) 2); // Equivalent to buf.Add((ushort)2);
        buf.add((short) 0); // Equivalent to buf.Add((ushort)0);
        buf.add((short) 0); // Equivalent to buf.Add((ushort)0);

        // Assuming this method returns application-specific information in byte[] form
        buf.add(ap);

        return buf.toByteArray(); // Using toByteArray() to return a byte[] directly
    }


    public static void printJavaLongAsCSharpULong(long value) {
        // Convert the long value to a binary string
        String binaryString = Long.toBinaryString(value);
        // Convert the binary string to a BigInteger
        BigInteger ulongEquivalent = new BigInteger(binaryString, 2);
        // Print the BigInteger, which represents the ulong value
//        System.out.println(ulongEquivalent);
    }

    public static byte[] getApplicationInfo() {
        OutBuf buf = new OutBuf();
        // Adding header information
        buf.add((short) 508); // dataHdr.m_szData
        buf.add((short) 13);  // dataHdr.m_nPackType
        buf.add((short) 0);   // dataHdr.m_nDataType
        buf.add((short) 0);   // dataHdr.m_nRandom

        // Initialize byte array with predefined values
        byte[] d = new byte[508];
        d[0x00000000] = 0x66;
        d[0x00000001] = 0x00;
        d[0x00000002] = 0x69;
        d[0x00000003] = 0x00;
        d[0x00000004] = 0x6c;
        d[0x00000005] = 0x00;
        d[0x00000006] = 0x65;
        d[0x00000007] = 0x00;
        d[0x00000008] = 0x3d;
        d[0x00000009] = 0x00;
        d[0x0000000a] = 0x74;
        d[0x0000000b] = 0x00;
        d[0x0000000c] = 0x65;
        d[0x0000000d] = 0x00;
        d[0x0000000e] = 0x72;
        d[0x0000000f] = 0x00;
        d[0x00000010] = 0x6d;
        d[0x00000011] = 0x00;
        d[0x00000012] = 0x69;
        d[0x00000013] = 0x00;
        d[0x00000014] = 0x6e;
        d[0x00000015] = 0x00;
        d[0x00000016] = 0x61;
        d[0x00000017] = 0x00;
        d[0x00000018] = 0x6c;
        d[0x00000019] = 0x00;
        d[0x0000001a] = 0x2e;
        d[0x0000001b] = 0x00;
        d[0x0000001c] = 0x65;
        d[0x0000001d] = 0x00;
        d[0x0000001e] = 0x78;
        d[0x0000001f] = 0x00;
        d[0x00000020] = 0x65;
        d[0x00000021] = 0x00;
        d[0x00000022] = 0x09;
        d[0x00000023] = 0x00;
        d[0x00000024] = 0x76;
        d[0x00000025] = 0x00;
        d[0x00000026] = 0x65;
        d[0x00000027] = 0x00;
        d[0x00000028] = 0x72;
        d[0x00000029] = 0x00;
        d[0x0000002a] = 0x73;
        d[0x0000002b] = 0x00;
        d[0x0000002c] = 0x69;
        d[0x0000002d] = 0x00;
        d[0x0000002e] = 0x6f;
        d[0x0000002f] = 0x00;
        d[0x00000030] = 0x6e;
        d[0x00000031] = 0x00;
        d[0x00000032] = 0x3d;
        d[0x00000033] = 0x00;
        d[0x00000034] = 0x31;
        d[0x00000035] = 0x00;
        d[0x00000036] = 0x34;
        d[0x00000037] = 0x00;
        d[0x00000038] = 0x31;
        d[0x00000039] = 0x00;
        d[0x0000003a] = 0x35;
        d[0x0000003b] = 0x00;
        d[0x0000003c] = 0x09;
        d[0x0000003d] = 0x00;
        d[0x0000003e] = 0x63;
        d[0x0000003f] = 0x00;
        d[0x00000040] = 0x65;
        d[0x00000041] = 0x00;
        d[0x00000042] = 0x72;
        d[0x00000043] = 0x00;
        d[0x00000044] = 0x74;
        d[0x00000045] = 0x00;
        d[0x00000046] = 0x5f;
        d[0x00000047] = 0x00;
        d[0x00000048] = 0x63;
        d[0x00000049] = 0x00;
        d[0x0000004a] = 0x6f;
        d[0x0000004b] = 0x00;
        d[0x0000004c] = 0x6d;
        d[0x0000004d] = 0x00;
        d[0x0000004e] = 0x70;
        d[0x0000004f] = 0x00;
        d[0x00000050] = 0x61;
        d[0x00000051] = 0x00;
        d[0x00000052] = 0x6e;
        d[0x00000053] = 0x00;
        d[0x00000054] = 0x79;
        d[0x00000055] = 0x00;
        d[0x00000056] = 0x3d;
        d[0x00000057] = 0x00;
        d[0x00000058] = 0x4d;
        d[0x00000059] = 0x00;
        d[0x0000005a] = 0x65;
        d[0x0000005b] = 0x00;
        d[0x0000005c] = 0x74;
        d[0x0000005d] = 0x00;
        d[0x0000005e] = 0x61;
        d[0x0000005f] = 0x00;
        d[0x00000060] = 0x51;
        d[0x00000061] = 0x00;
        d[0x00000062] = 0x75;
        d[0x00000063] = 0x00;
        d[0x00000064] = 0x6f;
        d[0x00000065] = 0x00;
        d[0x00000066] = 0x74;
        d[0x00000067] = 0x00;
        d[0x00000068] = 0x65;
        d[0x00000069] = 0x00;
        d[0x0000006a] = 0x73;
        d[0x0000006b] = 0x00;
        d[0x0000006c] = 0x20;
        d[0x0000006d] = 0x00;
        d[0x0000006e] = 0x4c;
        d[0x0000006f] = 0x00;
        d[0x00000070] = 0x74;
        d[0x00000071] = 0x00;
        d[0x00000072] = 0x64;
        d[0x00000073] = 0x00;
        d[0x00000074] = 0x09;
        d[0x00000075] = 0x00;
        d[0x00000076] = 0x63;
        d[0x00000077] = 0x00;
        d[0x00000078] = 0x65;
        d[0x00000079] = 0x00;
        d[0x0000007a] = 0x72;
        d[0x0000007b] = 0x00;
        d[0x0000007c] = 0x74;
        d[0x0000007d] = 0x00;
        d[0x0000007e] = 0x5f;
        d[0x0000007f] = 0x00;
        d[0x00000080] = 0x69;
        d[0x00000081] = 0x00;
        d[0x00000082] = 0x73;
        d[0x00000083] = 0x00;
        d[0x00000084] = 0x73;
        d[0x00000085] = 0x00;
        d[0x00000086] = 0x75;
        d[0x00000087] = 0x00;
        d[0x00000088] = 0x65;
        d[0x00000089] = 0x00;
        d[0x0000008a] = 0x72;
        d[0x0000008b] = 0x00;
        d[0x0000008c] = 0x3d;
        d[0x0000008d] = 0x00;
        d[0x0000008e] = 0x44;
        d[0x0000008f] = 0x00;
        d[0x00000090] = 0x69;
        d[0x00000091] = 0x00;
        d[0x00000092] = 0x67;
        d[0x00000093] = 0x00;
        d[0x00000094] = 0x69;
        d[0x00000095] = 0x00;
        d[0x00000096] = 0x43;
        d[0x00000097] = 0x00;
        d[0x00000098] = 0x65;
        d[0x00000099] = 0x00;
        d[0x0000009a] = 0x72;
        d[0x0000009b] = 0x00;
        d[0x0000009c] = 0x74;
        d[0x0000009d] = 0x00;
        d[0x0000009e] = 0x20;
        d[0x0000009f] = 0x00;
        d[0x000000a0] = 0x54;
        d[0x000000a1] = 0x00;
        d[0x000000a2] = 0x72;
        d[0x000000a3] = 0x00;
        d[0x000000a4] = 0x75;
        d[0x000000a5] = 0x00;
        d[0x000000a6] = 0x73;
        d[0x000000a7] = 0x00;
        d[0x000000a8] = 0x74;
        d[0x000000a9] = 0x00;
        d[0x000000aa] = 0x65;
        d[0x000000ab] = 0x00;
        d[0x000000ac] = 0x64;
        d[0x000000ad] = 0x00;
        d[0x000000ae] = 0x20;
        d[0x000000af] = 0x00;
        d[0x000000b0] = 0x47;
        d[0x000000b1] = 0x00;
        d[0x000000b2] = 0x34;
        d[0x000000b3] = 0x00;
        d[0x000000b4] = 0x20;
        d[0x000000b5] = 0x00;
        d[0x000000b6] = 0x43;
        d[0x000000b7] = 0x00;
        d[0x000000b8] = 0x6f;
        d[0x000000b9] = 0x00;
        d[0x000000ba] = 0x64;
        d[0x000000bb] = 0x00;
        d[0x000000bc] = 0x65;
        d[0x000000bd] = 0x00;
        d[0x000000be] = 0x20;
        d[0x000000bf] = 0x00;
        d[0x000000c0] = 0x53;
        d[0x000000c1] = 0x00;
        d[0x000000c2] = 0x69;
        d[0x000000c3] = 0x00;
        d[0x000000c4] = 0x67;
        d[0x000000c5] = 0x00;
        d[0x000000c6] = 0x6e;
        d[0x000000c7] = 0x00;
        d[0x000000c8] = 0x69;
        d[0x000000c9] = 0x00;
        d[0x000000ca] = 0x6e;
        d[0x000000cb] = 0x00;
        d[0x000000cc] = 0x67;
        d[0x000000cd] = 0x00;
        d[0x000000ce] = 0x20;
        d[0x000000cf] = 0x00;
        d[0x000000d0] = 0x52;
        d[0x000000d1] = 0x00;
        d[0x000000d2] = 0x53;
        d[0x000000d3] = 0x00;
        d[0x000000d4] = 0x41;
        d[0x000000d5] = 0x00;
        d[0x000000d6] = 0x34;
        d[0x000000d7] = 0x00;
        d[0x000000d8] = 0x30;
        d[0x000000d9] = 0x00;
        d[0x000000da] = 0x39;
        d[0x000000db] = 0x00;
        d[0x000000dc] = 0x36;
        d[0x000000dd] = 0x00;
        d[0x000000de] = 0x20;
        d[0x000000df] = 0x00;
        d[0x000000e0] = 0x53;
        d[0x000000e1] = 0x00;
        d[0x000000e2] = 0x48;
        d[0x000000e3] = 0x00;
        d[0x000000e4] = 0x41;
        d[0x000000e5] = 0x00;
        d[0x000000e6] = 0x33;
        d[0x000000e7] = 0x00;
        d[0x000000e8] = 0x38;
        d[0x000000e9] = 0x00;
        d[0x000000ea] = 0x34;
        d[0x000000eb] = 0x00;
        d[0x000000ec] = 0x20;
        d[0x000000ed] = 0x00;
        d[0x000000ee] = 0x32;
        d[0x000000ef] = 0x00;
        d[0x000000f0] = 0x30;
        d[0x000000f1] = 0x00;
        d[0x000000f2] = 0x32;
        d[0x000000f3] = 0x00;
        d[0x000000f4] = 0x31;
        d[0x000000f5] = 0x00;
        d[0x000000f6] = 0x20;
        d[0x000000f7] = 0x00;
        d[0x000000f8] = 0x43;
        d[0x000000f9] = 0x00;
        d[0x000000fa] = 0x41;
        d[0x000000fb] = 0x00;
        d[0x000000fc] = 0x31;
        d[0x000000fd] = 0x00;
        d[0x000000fe] = 0x09;
        d[0x000000ff] = 0x00;
        d[0x00000100] = 0x63;
        d[0x00000101] = 0x00;
        d[0x00000102] = 0x65;
        d[0x00000103] = 0x00;
        d[0x00000104] = 0x72;
        d[0x00000105] = 0x00;
        d[0x00000106] = 0x74;
        d[0x00000107] = 0x00;
        d[0x00000108] = 0x5f;
        d[0x00000109] = 0x00;
        d[0x0000010a] = 0x73;
        d[0x0000010b] = 0x00;
        d[0x0000010c] = 0x65;
        d[0x0000010d] = 0x00;
        d[0x0000010e] = 0x72;
        d[0x0000010f] = 0x00;
        d[0x00000110] = 0x69;
        d[0x00000111] = 0x00;
        d[0x00000112] = 0x61;
        d[0x00000113] = 0x00;
        d[0x00000114] = 0x6c;
        d[0x00000115] = 0x00;
        d[0x00000116] = 0x3d;
        d[0x00000117] = 0x00;
        d[0x00000118] = 0x34;
        d[0x00000119] = 0x00;
        d[0x0000011a] = 0x37;
        d[0x0000011b] = 0x00;
        d[0x0000011c] = 0x35;
        d[0x0000011d] = 0x00;
        d[0x0000011e] = 0x30;
        d[0x0000011f] = 0x00;
        d[0x00000120] = 0x64;
        d[0x00000121] = 0x00;
        d[0x00000122] = 0x34;
        d[0x00000123] = 0x00;
        d[0x00000124] = 0x36;
        d[0x00000125] = 0x00;
        d[0x00000126] = 0x38;
        d[0x00000127] = 0x00;
        d[0x00000128] = 0x31;
        d[0x00000129] = 0x00;
        d[0x0000012a] = 0x37;
        d[0x0000012b] = 0x00;
        d[0x0000012c] = 0x32;
        d[0x0000012d] = 0x00;
        d[0x0000012e] = 0x63;
        d[0x0000012f] = 0x00;
        d[0x00000130] = 0x30;
        d[0x00000131] = 0x00;
        d[0x00000132] = 0x35;
        d[0x00000133] = 0x00;
        d[0x00000134] = 0x64;
        d[0x00000135] = 0x00;
        d[0x00000136] = 0x37;
        d[0x00000137] = 0x00;
        d[0x00000138] = 0x61;
        d[0x00000139] = 0x00;
        d[0x0000013a] = 0x31;
        d[0x0000013b] = 0x00;
        d[0x0000013c] = 0x30;
        d[0x0000013d] = 0x00;
        d[0x0000013e] = 0x36;
        d[0x0000013f] = 0x00;
        d[0x00000140] = 0x38;
        d[0x00000141] = 0x00;
        d[0x00000142] = 0x39;
        d[0x00000143] = 0x00;
        d[0x00000144] = 0x35;
        d[0x00000145] = 0x00;
        d[0x00000146] = 0x66;
        d[0x00000147] = 0x00;
        d[0x00000148] = 0x34;
        d[0x00000149] = 0x00;
        d[0x0000014a] = 0x63;
        d[0x0000014b] = 0x00;
        d[0x0000014c] = 0x30;
        d[0x0000014d] = 0x00;
        d[0x0000014e] = 0x61;
        d[0x0000014f] = 0x00;
        d[0x00000150] = 0x33;
        d[0x00000151] = 0x00;
        d[0x00000152] = 0x39;
        d[0x00000153] = 0x00;
        d[0x00000154] = 0x30;
        d[0x00000155] = 0x00;
        d[0x00000156] = 0x34;
        d[0x00000157] = 0x00;
        d[0x00000158] = 0x09;
        d[0x00000159] = 0x00;
        d[0x0000015a] = 0x6f;
        d[0x0000015b] = 0x00;
        d[0x0000015c] = 0x73;
        d[0x0000015d] = 0x00;
        d[0x0000015e] = 0x5f;
        d[0x0000015f] = 0x00;
        d[0x00000160] = 0x76;
        d[0x00000161] = 0x00;
        d[0x00000162] = 0x65;
        d[0x00000163] = 0x00;
        d[0x00000164] = 0x72;
        d[0x00000165] = 0x00;
        d[0x00000166] = 0x3d;
        d[0x00000167] = 0x00;
        d[0x00000168] = 0x57;
        d[0x00000169] = 0x00;
        d[0x0000016a] = 0x69;
        d[0x0000016b] = 0x00;
        d[0x0000016c] = 0x6e;
        d[0x0000016d] = 0x00;
        d[0x0000016e] = 0x64;
        d[0x0000016f] = 0x00;
        d[0x00000170] = 0x6f;
        d[0x00000171] = 0x00;
        d[0x00000172] = 0x77;
        d[0x00000173] = 0x00;
        d[0x00000174] = 0x73;
        d[0x00000175] = 0x00;
        d[0x00000176] = 0x20;
        d[0x00000177] = 0x00;
        d[0x00000178] = 0x38;
        d[0x00000179] = 0x00;
        d[0x0000017a] = 0x20;
        d[0x0000017b] = 0x00;
        d[0x0000017c] = 0x62;
        d[0x0000017d] = 0x00;
        d[0x0000017e] = 0x75;
        d[0x0000017f] = 0x00;
        d[0x00000180] = 0x69;
        d[0x00000181] = 0x00;
        d[0x00000182] = 0x6c;
        d[0x00000183] = 0x00;
        d[0x00000184] = 0x64;
        d[0x00000185] = 0x00;
        d[0x00000186] = 0x20;
        d[0x00000187] = 0x00;
        d[0x00000188] = 0x39;
        d[0x00000189] = 0x00;
        d[0x0000018a] = 0x32;
        d[0x0000018b] = 0x00;
        d[0x0000018c] = 0x30;
        d[0x0000018d] = 0x00;
        d[0x0000018e] = 0x30;
        d[0x0000018f] = 0x00;
        d[0x00000190] = 0x09;
        d[0x00000191] = 0x00;
        d[0x00000192] = 0x6f;
        d[0x00000193] = 0x00;
        d[0x00000194] = 0x73;
        d[0x00000195] = 0x00;
        d[0x00000196] = 0x5f;
        d[0x00000197] = 0x00;
        d[0x00000198] = 0x69;
        d[0x00000199] = 0x00;
        d[0x0000019a] = 0x64;
        d[0x0000019b] = 0x00;
        d[0x0000019c] = 0x3d;
        d[0x0000019d] = 0x00;
        d[0x0000019e] = 0x30;
        d[0x0000019f] = 0x00;
        d[0x000001a0] = 0x30;
        d[0x000001a1] = 0x00;
        d[0x000001a2] = 0x33;
        d[0x000001a3] = 0x00;
        d[0x000001a4] = 0x34;
        d[0x000001a5] = 0x00;
        d[0x000001a6] = 0x32;
        d[0x000001a7] = 0x00;
        d[0x000001a8] = 0x2d;
        d[0x000001a9] = 0x00;
        d[0x000001aa] = 0x34;
        d[0x000001ab] = 0x00;
        d[0x000001ac] = 0x33;
        d[0x000001ad] = 0x00;
        d[0x000001ae] = 0x32;
        d[0x000001af] = 0x00;
        d[0x000001b0] = 0x35;
        d[0x000001b1] = 0x00;
        d[0x000001b2] = 0x32;
        d[0x000001b3] = 0x00;
        d[0x000001b4] = 0x2d;
        d[0x000001b5] = 0x00;
        d[0x000001b6] = 0x31;
        d[0x000001b7] = 0x00;
        d[0x000001b8] = 0x31;
        d[0x000001b9] = 0x00;
        d[0x000001ba] = 0x30;
        d[0x000001bb] = 0x00;
        d[0x000001bc] = 0x30;
        d[0x000001bd] = 0x00;
        d[0x000001be] = 0x30;
        d[0x000001bf] = 0x00;
        d[0x000001c0] = 0x2d;
        d[0x000001c1] = 0x00;
        d[0x000001c2] = 0x41;
        d[0x000001c3] = 0x00;
        d[0x000001c4] = 0x41;
        d[0x000001c5] = 0x00;
        d[0x000001c6] = 0x4f;
        d[0x000001c7] = 0x00;
        d[0x000001c8] = 0x45;
        d[0x000001c9] = 0x00;
        d[0x000001ca] = 0x4d;
        d[0x000001cb] = 0x00;
        d[0x000001cc] = 0x09;
        d[0x000001cd] = 0x00;
        d[0x000001ce] = 0x64;
        d[0x000001cf] = 0x00;
        d[0x000001d0] = 0x6f;
        d[0x000001d1] = 0x00;
        d[0x000001d2] = 0x6d;
        d[0x000001d3] = 0x00;
        d[0x000001d4] = 0x61;
        d[0x000001d5] = 0x00;
        d[0x000001d6] = 0x69;
        d[0x000001d7] = 0x00;
        d[0x000001d8] = 0x6e;
        d[0x000001d9] = 0x00;
        d[0x000001da] = 0x3d;
        d[0x000001db] = 0x00;
        d[0x000001dc] = 0x09;
        d[0x000001dd] = 0x00;
        d[0x000001de] = 0x63;
        d[0x000001df] = 0x00;
        d[0x000001e0] = 0x6f;
        d[0x000001e1] = 0x00;
        d[0x000001e2] = 0x6d;
        d[0x000001e3] = 0x00;
        d[0x000001e4] = 0x70;
        d[0x000001e5] = 0x00;
        d[0x000001e6] = 0x75;
        d[0x000001e7] = 0x00;
        d[0x000001e8] = 0x74;
        d[0x000001e9] = 0x00;
        d[0x000001ea] = 0x65;
        d[0x000001eb] = 0x00;
        d[0x000001ec] = 0x72;
        d[0x000001ed] = 0x00;
        d[0x000001ee] = 0x3d;
        d[0x000001ef] = 0x00;
        d[0x000001f0] = 0x4f;
        d[0x000001f1] = 0x00;
        d[0x000001f2] = 0x4d;
        d[0x000001f3] = 0x00;
        d[0x000001f4] = 0x45;
        d[0x000001f5] = 0x00;
        d[0x000001f6] = 0x4e;
        d[0x000001f7] = 0x00;
        d[0x000001f8] = 0x09;
        d[0x000001f9] = 0x00;
        d[0x000001fa] = 0x00;
        d[0x000001fb] = 0x00;

        buf.add(d);

        //        ArrayUtils.reverse(bytes);
        return buf.toByteArray();
    }

    //516
//{252,1,13,0,0,0,0,0,102,0,105,0,108,0,101,0,61,0,116,0,101,0,114,0,109,0,105,0,110,0,97,0,108,0,46,0,101,0,120,0,101,0,9,0,118,0,101,0,114,0,115,0,105,0,111,0,110,0,61,0,49,0,52,0,49,0,53,0,9,0,99,0,101,0,114,0,116,0,95,0,99,0,111,0,109,0,112,0,97,0,110,0,121,0,61,0,77,0,101,0,116,0,97,0,81,0,117,0,111,0,116,0,101,0,115,0,32,0,76,0,116,0,100,0,9,0,99,0,101,0,114,0,116,0,95,0,105,0,115,0,115,0,117,0,101,0,114,0,61,0,68,0,105,0,103,0,105,0,67,0,101,0,114,0,116,0,32,0,84,0,114,0,117,0,115,0,116,0,101,0,100,0,32,0,71,0,52,0,32,0,67,0,111,0,100,0,101,0,32,0,83,0,105,0,103,0,110,0,105,0,110,0,103,0,32,0,82,0,83,0,65,0,52,0,48,0,57,0,54,0,32,0,83,0,72,0,65,0,51,0,56,0,52,0,32,0,50,0,48,0,50,0,49,0,32,0,67,0,65,0,49,0,9,0,99,0,101,0,114,0,116,0,95,0,115,0,101,0,114,0,105,0,97,0,108,0,61,0,52,0,55,0,53,0,48,0,100,0,52,0,54,0,56,0,49,0,55,0,50,0,99,0,48,0,53,0,100,0,55,0,97,0,49,0,48,0,54,0,56,0,57,0,53,0,102,0,52,0,99,0,48,0,97,0,51,0,57,0,48,0,52,0,9,0,111,0,115,0,95,0,118,0,101,0,114,0,61,0,87,0,105,0,110,0,100,0,111,0,119,0,115,0,32,0,56,0,32,0,98,0,117,0,105,0,108,0,100,0,32,0,57,0,50,0,48,0,48,0,9,0,111,0,115,0,95,0,105,0,100,0,61,0,48,0,48,0,51,0,52,0,50,0,45,0,52,0,51,0,50,0,53,0,50,0,45,0,49,0,49,0,48,0,48,0,48,0,45,0,65,0,65,0,79,0,69,0,77,0,9,0,100,0,111,0,109,0,97,0,105,0,110,0,61,0,9,0,99,0,111,0,109,0,112,0,117,0,116,0,101,0,114,0,61,0,79,0,77,0,69,0,78,0,9,0,0,0,}
    public static void createAndFillTransactionKey2(Session session) throws Exception {
        byte[] hardId = session.getHardIdByte();
        final byte[] rnd = new byte[16];
        long seed = (Integer.toUnsignedLong(session.account) + Integer.toUnsignedLong(session.session));
        for (int i = 0; i < 16; i++) {
            seed = seed * 214013 + 2531011;
            seed &= MAX_INT_VALUE;
            rnd[i] = (byte) ((seed >>> 16) & 0xFFL);
        }
        final byte[] data = new byte[36];
        System.arraycopy(rnd, 0, data, 0, rnd.length);
        System.arraycopy(hardId, 0, data, 16, hardId.length);
        ByteArrayUtil.copyIntToByteArray(ConnectionData.CLIENT_EXE_SIZE, data, 32);
        MessageDigest m = MessageDigest.getInstance("MD5");
        final byte[] hash = m.digest(data);
        session.transactionKey2 = MT4Crypt.encode(rnd, hash);
        session.transactionKey2 = MT4Crypt.encode(session.transactionKey2, hardId);
    }
}
