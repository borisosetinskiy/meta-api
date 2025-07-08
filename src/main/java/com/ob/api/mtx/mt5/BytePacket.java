package com.ob.api.mtx.mt5;

import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class BytePacket {
    final boolean isByteBuffer = true;
    private final Map<Byte, ArrayList<Byte>> Packets = new ConcurrentHashMap<>();
    private final Map<Byte, ByteBuff> PacketsByteBuff = new ConcurrentHashMap<>();

    public boolean has(byte cmd) {
        if (isByteBuffer) {
            return PacketsByteBuff.containsKey(cmd);
        } else {
            return Packets.containsKey(cmd);
        }
    }

    public byte[] toBytes(byte cmd) {
        if (isByteBuffer) {
            ByteBuff buff = PacketsByteBuff.get(cmd);
            try {
                return buff.toBytes();
            } finally {
                PacketsByteBuff.remove(cmd);
            }
        } else {
            try {
                return ByteLists.toArray(Packets.get(cmd));
            } finally {
                Packets.remove(cmd);
            }
        }
    }

    public void add(byte cmd, byte[] bytes) {
        if (isByteBuffer) {
            ByteBuff buff = PacketsByteBuff.get(cmd);
            if (buff == null) {
                PacketsByteBuff.put(cmd, new ByteBuff(bytes));
            } else {
                buff.add(bytes);
            }
        } else {
            if (!Packets.containsKey(cmd))
                Packets.put(cmd, new ArrayList<>());
            ByteLists.addPrimitiveArrayToList(bytes, Packets.get(cmd));
        }
    }
}
