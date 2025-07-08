package com.ob.api.mtx.mt5;

import com.ob.broker.common.error.Code;

import java.io.IOException;
import java.util.ArrayList;

public class QuoteHistory {
    private MT5API QuoteClient;

    public QuoteHistory(MT5API qc) {
        QuoteClient = qc;
    }

    public static void ReqProcess(Connection connection, String symbol) throws IOException {
        OutBuf buf = new OutBuf();
//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: buf.Add((byte)0xE);
        buf.Add((byte) (0xE & 0xFF));
//C# TO JAVA CONVERTER TODO TASK: There is no equivalent to implicit typing in Java:
        byte[] bytes = symbol.getBytes(java.nio.charset.StandardCharsets.UTF_16LE);
        buf.Add(bytes);
//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: buf.Add(new byte[32 * 2 - bytes.Length]);
        buf.Add(new byte[32 * 2 - bytes.length]);
//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: buf.Add((ushort)1);
        buf.Add((short) (1 & 0xFFFF)); //size
//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: buf.Add((byte)0);
        buf.Add((byte) (0 & 0xFF)); //year
        buf.Add((int) 0); //time
//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: buf.Add((ushort)0);
        buf.Add((short) (0 & 0xFFFF)); //CheckDate
        connection.SendPacket((byte) 0x66, buf);
    }

    public static void ReqStart(Connection connection, String symbol) throws IOException {
        OutBuf buf = new OutBuf();
//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: buf.Add((byte)0xE);
        buf.Add((byte) (0xE & 0xFF));
//C# TO JAVA CONVERTER TODO TASK: There is no equivalent to implicit typing in Java:
        byte[] bytes = symbol.getBytes(java.nio.charset.StandardCharsets.UTF_16LE);
        buf.Add(bytes);
//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: buf.Add(new byte[32 * 2 - bytes.Length]);
        buf.Add(new byte[32 * 2 - bytes.length]);
//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: buf.Add((ushort)1);
        buf.Add((short) (1 & 0xFFFF)); //size
//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: buf.Add((byte)0);
        buf.Add((byte) (0 & 0xFF)); //year
        buf.Add((int) 0); //time
//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: buf.Add((ushort)0);
        buf.Add((short) (0 & 0xFFFF)); //CheckDate
        connection.SendPacket((byte) 0x66, buf);
    }

    public static void ReqSelect(Connection connection, String symbol) throws IOException {
        OutBuf buf = new OutBuf();
//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: buf.Add((byte)9);
        buf.Add((byte) (9 & 0xFF));
//C# TO JAVA CONVERTER TODO TASK: There is no equivalent to implicit typing in Java:
        byte[] bytes = symbol.getBytes(java.nio.charset.StandardCharsets.UTF_16LE);
        buf.Add(bytes);
//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: buf.Add(new byte[32 * 2 - bytes.Length]);
        buf.Add(new byte[32 * 2 - bytes.length]);
//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: buf.Add((ushort)1);
        buf.Add((short) (1 & 0xFFFF)); //begin
//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: buf.Add((ushort)1);
        buf.Add((short) (1 & 0xFFFF)); //firstDate
        connection.SendPacket((byte) 0x66, buf);
    }

    public final void Parse(InBuf buf) {
//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: var cmd = buf.Byte();
        byte cmd = buf.Byte();
        String symbol = ConvertBytes.ToUnicode(buf.Bytes(32));
        switch ((cmd & 0xFF)) {
            case 0x0E:
                ParseStart(buf, symbol);
                break;
            case 9:
                ParseSelect(buf, symbol);
                break;
            default:
                throw new UnsupportedOperationException("Parse quote hist cmd = " + (cmd & 0xFF));
        }
    }

    public final void ParseSelect(InBuf buf, String symbol) {
        int status = buf.Int();
        if (status != 0) {
            throw new RuntimeException((Code.getById(status)).toString());
        }
//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: var num = buf.UShort();
        short num = buf.UShort();
        //if (num == 0)
        //    throw new Exception("num == 0");
        int numContainer = 0;
        for (int i = 0; i < (num & 0xFFFF); i++) {
            ParseContainer(buf, symbol);
            numContainer++;
        }

    }

    public final void ParseStart(InBuf buf, String symbol) {
        int status = buf.Int();
        if (status != 0) {
            throw new RuntimeException((Code.getById(status)).toString());
        }
//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: var day = buf.UShort();
        short day = buf.UShort();
//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: var num = buf.UShort();
        short num = buf.UShort();
        if ((num & 0xFFFF) == 0) {
            ParseContainer(buf, symbol);
        } else {
            throw new UnsupportedOperationException("num != 0");
        }
    }

    private void ParseContainer(InBuf buf, String symbol) {
        HistHeader hdr = UDT.ReadStruct(buf.Bytes(32 + 0x81), 32, 0x81, new HistHeader());
        if ((hdr.Flags & 1) != 0) {
            throw new RuntimeException("Compressed");
        }
//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: byte[] data = buf.Bytes(hdr.DataSize);
        byte[] data = buf.Bytes(hdr.DataSize);
        ReadBarRecords(new BitReader(data, hdr), hdr, symbol);
    }

    private void ReadBarRecords(BitReader btr, HistHeader hdr, String symbol) {
//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: ulong flags = 0;
        long flags = 0;
        boolean bSpread = false;
        boolean bVolume = false;
        int numBars = 0;
        ArrayList<Bar> bars = new ArrayList<Bar>();
        BarRecord rec = new BarRecord();
        while ((btr.BitPos <= btr.BitSize) && (numBars < hdr.NumberBars)) {
            long type = BitConverter.ToInt64(btr.GetRecord(8), 0);
            if (type == 0) {
                flags = BitConverter.ToUInt64(btr.GetRecord(8), 0);
                rec.Time = BitConverter.ToInt64(btr.GetRecord(8), 0);
                rec.OpenPrice = BitConverter.ToInt32(btr.GetSignRecord(8), 0);
                rec.High = BitConverter.ToInt32(btr.GetRecord(4), 0);
                rec.Low = BitConverter.ToInt32(btr.GetRecord(4), 0);
                rec.Close = BitConverter.ToInt32(btr.GetSignRecord(4), 0);
                rec.TickVolume = BitConverter.ToUInt64(btr.GetRecord(8), 0);
                if ((flags & 1) != 0) {
                    bSpread = true;
                    rec.Spread = BitConverter.ToInt32(btr.GetSignRecord(4), 0);
                }
                if ((flags & 2) != 0) {
                    bVolume = true;
                    rec.Volume = BitConverter.ToUInt64(btr.GetRecord(8), 0);
                }
                btr.SkipRecords(flags, 4);
                bars.add(RecordToBar(rec.clone(), hdr.Digits).clone());
                numBars++;
            } else if (type == 1) {
                long num = btr.GetLong();
                for (long i = 0; i < num; i++) {
                    rec.Time += 60;
                    long value = btr.GetSignLong();
                    rec.OpenPrice += (hdr.LimitPoints & 0xFFFFFFFFL) * value + rec.Close;
                    int data = btr.GetInt();
                    rec.High = (int) ((hdr.LimitPoints & 0xFFFFFFFFL) * data);
                    data = btr.GetInt();
                    rec.Low = (int) ((hdr.LimitPoints & 0xFFFFFFFFL) * data);
                    value = btr.GetSignLong();
                    rec.Close = (int) ((hdr.LimitPoints & 0xFFFFFFFFL) * (int) value);
                    rec.TickVolume = btr.GetULong();
                    if (bSpread) {
                        rec.Spread = btr.GetSignInt();
                    }
                    if (bVolume) {
                        rec.Volume = btr.GetULong();
                    }
                    btr.SkipRecords(flags, 4);
                    bars.add(RecordToBar(rec.clone(), hdr.Digits).clone());
                    numBars++;
                }
            } else if (type == 2) {
                long value = btr.GetLong();
                rec.Time += value * 60;
            }
        }
        QuoteClient.OnQuoteHistory(symbol, bars.toArray(new Bar[0]));
    }

    private Bar RecordToBar(BarRecord rec, int digits) {
        Bar bar = new Bar();
        bar.Time = ConvertTo.DateTimeMs(rec.Time);
        bar.OpenPrice = ConvertTo.LongLongToDouble(digits, rec.OpenPrice);
        bar.HighPrice = ConvertTo.LongLongToDouble(digits, rec.OpenPrice + rec.High);
        bar.LowPrice = ConvertTo.LongLongToDouble(digits, rec.OpenPrice - rec.Low);
        bar.ClosePrice = ConvertTo.LongLongToDouble(digits, rec.OpenPrice + rec.Close);
        bar.Volume = rec.Volume;
        bar.TickVolume = rec.TickVolume;
        bar.Spread = rec.Spread;
        return bar;
    }
}

