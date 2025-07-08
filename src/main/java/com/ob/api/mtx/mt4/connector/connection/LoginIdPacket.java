package com.ob.api.mtx.mt4.connector.connection;

import com.ob.api.mtx.mt4.connector.util.ByteArrayUtil;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Random;

public class LoginIdPacket implements Delegate {
    @Override
    public byte[] invoke(byte[] hardId, Session cp, Connection connection) {
        return loginIdPacketCreate(cp);
    }

    byte[] invokeIt1321(byte[] hardId, Session cp, Connection connection) {
        byte[] hdr = new byte[8];
        hdr[0] = 8;
        hdr[2] = 10;
        cp.seed = (cp.seed * 214013) + 2531011;
        cp.seed &= ConnectionUtil.MAX_INT_VALUE;
        short sShort = (short) ((cp.seed >>> 16) & 0x7FFFL);
        ByteArrayUtil.copyShortToByteArray(sShort, hdr, 6);
        byte[] data = new byte[8];

        ByteArrayUtil.copyLongToByteArray(cp.loginId ^ 0x05286AED3286692AL, data, 0);
        byte[] result = ConnectionUtil.cryptPacket(hdr, data, cp.hashKey);

        if (cp.serverBuild > 1321) {
            hdr = new byte[8];
            hdr[0] = 8;
            hdr[2] = 12;
            cp.seed = (cp.seed * 214013) + 2531011;
            cp.seed &= ConnectionUtil.MAX_INT_VALUE;
            sShort = (short) ((cp.seed >>> 16) & 0x7FFFL);
            ByteArrayUtil.copyShortToByteArray(sShort, hdr, 6);
            data = new byte[8];
            ByteArrayUtil.copyLongToByteArray(cp.dataLoginId ^ 0x05286AED3286692AL, data, 0);
            byte[] newResult = ConnectionUtil.cryptPacket(hdr, data, cp.hashKey);
            int oldResultSize = result.length;
            result = Arrays.copyOf(result, newResult.length + oldResultSize);
            System.arraycopy(newResult, 0, result, oldResultSize, newResult.length);
        }
        if (cp.serverBuild > 1393) {
            hdr = new byte[8];
            hdr[0] = 8;
            hdr[2] = 16;
            cp.seed = (cp.seed * 214013) + 2531011;
            cp.seed &= ConnectionUtil.MAX_INT_VALUE;
            sShort = (short) ((cp.seed >>> 16) & 0x7FFFL);
            ByteArrayUtil.copyShortToByteArray(sShort, hdr, 6);
            data = new byte[8];
            ByteArrayUtil.copyLongToByteArray(cp.loginIdEx ^ 0x05286AED3286692AL, data, 0);
            byte[] newResult = ConnectionUtil.cryptPacket(hdr, data, cp.hashKey);
            int oldResultSize = result.length;
            result = Arrays.copyOf(result, newResult.length + oldResultSize);
            System.arraycopy(newResult, 0, result, oldResultSize, newResult.length);
        }
        return result;
    }

    byte[] loginIdPacketCreate(Session cp) {

        Random random = new Random();

        byte[] hdr = new byte[8];
        hdr[0] = 8;
        hdr[2] = 10;
        ByteBuffer buffer = ByteBuffer.allocate(2);
        buffer.putShort((short) random.nextInt(Short.MAX_VALUE + 1));
        System.arraycopy(buffer.array(), 0, hdr, 6, 2);

        byte[] data = new byte[8];

        ByteArrayUtil.copyLongToByteArray(cp.loginId ^ 0x05286AED3286692AL, data, 0);
        byte[] result = ConnectionUtil.cryptPacket(hdr, data, cp.hashKey);

        if (cp.serverBuild > 1321) {
            hdr = new byte[8];
            hdr[0] = 8;
            hdr[2] = 12;
            buffer = ByteBuffer.allocate(2);
            buffer.putShort((short) random.nextInt(Short.MAX_VALUE + 1));
            System.arraycopy(buffer.array(), 0, hdr, 6, 2);
            data = new byte[8];
            ByteArrayUtil.copyLongToByteArray(cp.dataLoginId ^ 0x05286AED3286692AL, data, 0);
            byte[] newResult = ConnectionUtil.cryptPacket(hdr, data, cp.hashKey);
            int oldResultSize = result.length;
            result = Arrays.copyOf(result, newResult.length + oldResultSize);
            System.arraycopy(newResult, 0, result, oldResultSize, newResult.length);
        }
        if (cp.serverBuild > 1393) {
            hdr = new byte[8];
            hdr[0] = 8;
            hdr[2] = 16;
            buffer = ByteBuffer.allocate(2);
            buffer.putShort((short) random.nextInt(Short.MAX_VALUE + 1));
            System.arraycopy(buffer.array(), 0, hdr, 6, 2);
            data = new byte[8];
            ByteArrayUtil.copyLongToByteArray(cp.loginIdEx ^ 0x05286AED3286692AL, data, 0);
            byte[] newResult = ConnectionUtil.cryptPacket(hdr, data, cp.hashKey);
            int oldResultSize = result.length;
            result = Arrays.copyOf(result, newResult.length + oldResultSize);
            System.arraycopy(newResult, 0, result, oldResultSize, newResult.length);
        }
        return result;
    }

/**
 * private byte[] LoginIdPacket()
 *                {
 * 			byte[] hdr = new byte[8];
 * 			hdr[0] = 8;
 * 			hdr[2] = 10;
 * 			BitConverter.GetBytes((ushort)new Random().Next()).CopyTo(hdr, 6);
 *
 * 			byte[] data = new byte[8];
 * 			BitConverter.GetBytes(_LoginId ^ 0x05286AED3286692A).CopyTo(data, 0);
 * 			var res = EncryptPacket(hdr, data, _HashKey);
 * 			if (_CurrentBuild > 1321)
 *            {
 * 				hdr = new byte[8];
 * 				hdr[0] = 8;
 * 				hdr[2] = 12;
 * 				BitConverter.GetBytes((ushort)new Random().Next()).CopyTo(hdr, 6);
 * 				data = new byte[8];
 * 				BitConverter.GetBytes(_DataLoginId ^ 0x05286AED3286692A).CopyTo(data, 0);
 * 				var r = EncryptPacket(hdr, data, _HashKey);
 * 				var resOldLen = res.Length;
 * 				Array.Resize(ref res, res.Length + r.Length);
 * 				r.CopyTo(res, resOldLen);
 *            }
 * 			if (_CurrentBuild > 1393)
 *            {
 * 				hdr = new byte[8];
 * 				hdr[0] = 8;
 * 				hdr[2] = 16;
 * 				BitConverter.GetBytes((ushort)new Random().Next()).CopyTo(hdr, 6);
 * 				data = new byte[8];
 * 				BitConverter.GetBytes(_LoginIdEx ^ 0x05286AED3286692A).CopyTo(data, 0);
 * 				var r = EncryptPacket(hdr, data, _HashKey);
 * 				var resOldLen = res.Length;
 * 				Array.Resize(ref res, res.Length + r.Length);
 * 				r.CopyTo(res, resOldLen);
 *            }
 * 			return res;
 *        }
 */


}
