package com.ob.api.mtx.mt4.connector.connection.codec;

import org.apache.commons.lang3.ArrayUtils;

import java.math.BigInteger;
import java.nio.ByteBuffer;

public class RSAJava {
    private static final BigInteger TWO_COMPL_REF = BigInteger.ONE.shiftLeft(64);
    private static final BigInteger NUMBER_16_RADIX = new BigInteger("FFFFFFFFFFFFFFFF", 16);
    private static final BigInteger NUMBER_FOR_SIGN = new BigInteger(String.valueOf(0xFFFFFFFF00000000L));

    private BigInteger M, N, D, Y;

    public RSAJava(BigInteger p) {
//        P = p.xor(new BigInteger(String.valueOf(0x151D8255)));
//        Q = p.xor(new BigInteger(String.valueOf(0x274ECC00)));
        M = new BigInteger(String.valueOf(0x67789ED4559AF79L));
        N = new BigInteger("CCCCCCCCCCCCCCCC", 16);
        D = new BigInteger(String.valueOf(0x1DE7FED38081L));
        //E = 0x5D405B5;
        Y = new BigInteger(String.valueOf(0x53290744C4D541L));
    }

    private static byte[] parseBigIntegerPositive(BigInteger b) {
        if (b.compareTo(BigInteger.ZERO) < 0)
            b = b.add(TWO_COMPL_REF);
        return b.toByteArray();
    }

    public long computePacketKey(byte[] data) throws Exception {
        return expMod64(prepareKey(data).mod(N), N, M).longValue();
    }

    public long computeFileKey(byte[] data) throws Exception {
        return expMod64(prepareKey(data).mod(M), D, M).longValue();
    }

    public boolean checkKey(byte[] data) throws Exception {
        ByteBuffer wrappedData = ByteBuffer.wrap(data);
        if (data.length < 8)
            return false;
        int szData = data.length - 8;
        byte[] buf = new byte[szData];
        System.arraycopy(buf, 0, data, 0, szData);
        BigInteger dataKey = prepareKey(buf);
        BigInteger origKey = getReversedIntFromBytes(wrappedData, szData);
        return expMod64(origKey, Y, M).longValue() == (dataKey.longValue() % M.longValue());
    }

    public BigInteger prepareKey(byte[] data) throws Exception {
        if (data.length < 1)
            return BigInteger.ZERO;
        BigInteger h = BigInteger.ZERO;
        BigInteger pm = new BigInteger(String.valueOf(0x123456789L));
        ByteBuffer wrappedData = ByteBuffer.wrap(data);
        for (int i = 0; i < data.length / 8; i++) {
            BigInteger w = getReversedIntFromBytes(wrappedData, i * 8);
            h = h.xor(w);
            Long lw = w.longValue() & 0xFFFFFFFFL;
            Long hw = w.shiftRight(32).longValue();
            BigInteger sign = (!(BigInteger.valueOf(hw).and(new BigInteger(String.valueOf(0x80000000L)))).equals(BigInteger.ZERO))
                    ? NUMBER_FOR_SIGN : BigInteger.ZERO;
            BigInteger t = ((BigInteger.valueOf(hw).xor(BigInteger.valueOf(lw))).shiftLeft(32)).or((BigInteger.valueOf(lw).xor(BigInteger.valueOf(hw))));
            t = t.add(sign);
            t = (t.add(((BigInteger.valueOf(lw).shiftLeft(32)).or(BigInteger.valueOf(hw))).or(sign))).and(NUMBER_16_RADIX);
            t = t.add(pm);
            t = t.add(w);
            pm = pm.xor(t);
        }
        int rem = data.length & 7;
        if (rem != 0) {
            long ls = 0;
            long rs = 0;
            long mdc = 0x11F71FB04CBL;
            int last = 0;
            if (rem >= 2) {
                int it = (rem - 2) / 2 + 1;
                last = it * 2;
                int off = data.length - rem;
                int sh = 0;
                for (int i = 0; i < it; i++, sh += 16) {
                    ls += (long) ((byte) data[i * 2 + off]) << sh;
                    rs += (long) ((byte) data[i * 2 + 1 + off]) << (sh + 8);
                }
            }
            if (last < rem)
                mdc += (long) ((byte) data[data.length - 1]) << (last << 8);
            BigInteger w = (new BigInteger(String.valueOf(ls))
                    .add(new BigInteger(String.valueOf(rs)))
                    .add(new BigInteger(String.valueOf(mdc))));
            w = new BigInteger(parseBigIntegerPositive(w));
            h = h.xor(w);
            Long lw = w.longValue() & 0xFFFFFFFFL;
            Long hw = w.shiftRight(32).longValue();
            BigInteger sign = (!(BigInteger.valueOf(hw).and(new BigInteger(String.valueOf(0x80000000L)))).equals(BigInteger.ZERO))
                    ? NUMBER_FOR_SIGN : BigInteger.ZERO;
            sign = new BigInteger(parseBigIntegerPositive(sign));
            BigInteger t = ((BigInteger) (BigInteger.valueOf(hw)
                    .xor(BigInteger.valueOf(lw))).shiftLeft(32))
                    .or((BigInteger) (BigInteger.valueOf(lw)
                            .xor(BigInteger.valueOf(hw))));
            t = (t.add(sign)).and(NUMBER_16_RADIX);
            t = t.add((((BigInteger.valueOf(lw).shiftLeft(32)).or(BigInteger.valueOf(hw))).or(sign))).and(NUMBER_16_RADIX);
            t = t.add(pm);
            t = (t.add(w)).and(NUMBER_16_RADIX);
            ;
            pm = pm.xor(t);
        }

        return ((new BigInteger(String.valueOf(data.length)).multiply(new BigInteger(String.valueOf(0x100000001L)))).xor(pm).xor(h)).and(new BigInteger(String.valueOf(0xFFFFFFFFFFFL)));
    }

    public BigInteger expMod64(BigInteger rem, BigInteger n, BigInteger m) {
        BigInteger key = BigInteger.ONE;
        BigInteger prv = rem;
        for (int i = 0; i < 64; i++) {
            if (!((n.shiftRight(i)).and(BigInteger.ONE)).equals(BigInteger.ZERO))
                key = mulMod64(key, prv, m);
            prv = mulMod64(prv, prv, m);
        }
        return key;
    }

    public BigInteger mulMod64(BigInteger k, BigInteger n, BigInteger m) {
        BigInteger key = BigInteger.ZERO;
        BigInteger prv = k;
        for (int i = 0; i < 64; i++) {
            if (!((n.shiftRight(i)).and(BigInteger.ONE)).equals(BigInteger.ZERO))
                key = (key.add(prv)).mod(m);
            prv = prv.multiply(BigInteger.valueOf(2)).mod(m);
        }
        return key;
    }

    private BigInteger getReversedIntFromBytes(ByteBuffer byteBuffer, int index) {
        byte[] intByteArr = new byte[8];
        byteBuffer.position(index);
        byteBuffer.get(intByteArr, 0, 8);
        ArrayUtils.reverse(intByteArr);
        return new BigInteger(1, intByteArr);
    }
}
