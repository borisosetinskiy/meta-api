package com.ob.api.mtx.mt5;

import com.google.common.collect.Sets;
import lombok.Getter;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Getter
class Subscriber {
    public final Map<String, Quote> quotes = new ConcurrentHashMap<>();
    public final Set<String> subscribe = Sets.newConcurrentHashSet();
    private MT5API api;

    public Subscriber(MT5API api) {
        this.api = api;
    }

    public final Quote getQuote(String symbol) throws IOException {
        subscribe(symbol);
        return quotes.get(symbol);
    }

    public final boolean isSubscribed(String symbol) {
        return subscribe.contains(symbol);
    }

    public final void subscribe(String symbol) throws IOException {
        try {
            if (!subscribe.contains(symbol)) {
                subscribe.add(symbol);
                Request();
            }
        } catch (IOException e) {
            subscribe.remove(symbol);
            throw e;
        }
    }

    public final void resubscribe() throws IOException {
        if (!subscribe.isEmpty()) {
            Request();
        }
    }

    public final void unsubscribe(String symbol) throws IOException {
        if (subscribe.remove(symbol)) {
            Request();
        }
    }

    private void Request() throws IOException {
        OutBuf buf = new OutBuf();
        buf.ByteToBuffer((byte) 9);
        buf.IntToBuffer(subscribe.size());
        for (String item : subscribe) {
            SymbolInfo si = api.symbols.GetInfo(item);
            buf.IntToBuffer(si.Id);
        }
        api.Connection.SendPacket((byte) 0x69, buf);
    }

    public final void Parse(InBuf buf) {
        byte[] bytes = buf.ToBytes();
        BitReaderQuotes br = new BitReaderQuotes(bytes, bytes.length * 8);
        br.Initialize((byte) 2, (byte) 3);
        while (true) {
            TickRec rec = new TickRec();
            RefObject<Integer> tempRef_Id = new RefObject<Integer>(rec.Id);
            if (!br.GetInt(tempRef_Id)) {
                rec.Id = tempRef_Id.argValue;
                break;
            } else {
                rec.Id = tempRef_Id.argValue;
            }
            RefObject<Long> tempRef_Time = new RefObject<Long>(rec.Time);
            if (!br.GetLong(tempRef_Time)) {
                rec.Time = tempRef_Time.argValue;
                break;
            } else {
                rec.Time = tempRef_Time.argValue;
            }
            RefObject<Long> tempRef_UpdateMask = new RefObject<Long>(rec.UpdateMask);
            if (!br.GetULong(tempRef_UpdateMask)) {
                rec.UpdateMask = tempRef_UpdateMask.argValue;
                break;
            } else {
                rec.UpdateMask = tempRef_UpdateMask.argValue;
            }
            if ((rec.UpdateMask & 1) != 0) {
                RefObject<Long> tempRef_Bid = new RefObject<Long>(rec.Bid);
                if (!br.GetLong(tempRef_Bid)) {
                    rec.Bid = tempRef_Bid.argValue;
                    break;
                } else {
                    rec.Bid = tempRef_Bid.argValue;
                }
            }
            if ((rec.UpdateMask & 2) != 0) {
                RefObject<Long> tempRef_Ask = new RefObject<Long>(rec.Ask);
                if (!br.GetLong(tempRef_Ask)) {
                    rec.Ask = tempRef_Ask.argValue;
                    break;
                } else {
                    rec.Ask = tempRef_Ask.argValue;
                }
            }
            if ((rec.UpdateMask & 4) != 0) {
                RefObject<Long> tempRef_Last = new RefObject<Long>(rec.Last);
                if (!br.GetLong(tempRef_Last)) {
                    rec.Last = tempRef_Last.argValue;
                    break;
                } else {
                    rec.Last = tempRef_Last.argValue;
                }
            }
            long volume = 0;
            if ((rec.UpdateMask & 8) != 0) {
                RefObject<Long> tempRef_volume = new RefObject<Long>(volume);
                if (!br.GetULong(tempRef_volume)) {
                    volume = tempRef_volume.argValue;
                    break;
                } else {
                    volume = tempRef_volume.argValue;
                }
            }
            if ((rec.UpdateMask & 0x10) != 0) {
                RefObject<Long> tempRef_s3C = new RefObject<Long>(rec.s3C);
                if (!br.GetLong(tempRef_s3C)) {
                    rec.s3C = tempRef_s3C.argValue;
                    break;
                } else {
                    rec.s3C = tempRef_s3C.argValue;
                }
            }
            if ((rec.UpdateMask & 0x20) != 0) {
                RefObject<Short> tempRef_BankId = new RefObject<Short>(rec.BankId);
                if (!br.GetShort(tempRef_BankId)) {
                    rec.BankId = tempRef_BankId.argValue;
                    break;
                } else {
                    rec.BankId = -1;
                }
            }
            if ((rec.UpdateMask & 0x40) != 0) {
                RefObject<Long> tempRef_TimeMs = new RefObject<Long>(rec.TimeMs);
                if (!br.GetLong(tempRef_TimeMs)) {
                    rec.TimeMs = tempRef_TimeMs.argValue;
                    break;
                } else {
                    rec.TimeMs = tempRef_TimeMs.argValue;
                }
                rec.TimeMs += rec.Time * 1000;
            } else {
                rec.TimeMs = rec.Time * 1000;
            }
            if ((rec.UpdateMask & 0x80) != 0) {
                RefObject<Long> tempRef_s44 = new RefObject<Long>(rec.s44);
                if (!br.GetLong(tempRef_s44)) {
                    rec.s44 = tempRef_s44.argValue;
                    break;
                } else {
                    rec.s44 = tempRef_s44.argValue;
                }
            }
            long volumeEx = 0;
            if ((rec.UpdateMask & 0x100) != 0) {
                RefObject<Long> tempRef_volumeEx = new RefObject<Long>(volumeEx);
                if (!br.GetULong(tempRef_volumeEx)) {
                    volumeEx = tempRef_volumeEx.argValue;
                    break;
                } else {
                    volumeEx = tempRef_volumeEx.argValue;
                }
            }
            if ((rec.UpdateMask & 0x108) != 0) {
                rec.Volume = volume * 100000000 + volumeEx;
            }
            br.SkipRecords(rec.UpdateMask, 0x100);
            br.AlignBitPosition(1);
            SymbolInfo info = api.symbols.GetInfo(rec.Id);
            Quote quote;
            if (!subscribe.contains(info.Name)) {
                continue;
            }
            quote = quotes.computeIfAbsent(info.Name, s -> {
                Quote quote1 = new Quote();
                quote1.id = rec.Id;
                quote1.Symbol = info.Name;
                return quote1;
            });

            RecToQuote(rec, quote);
            if (quote.Bid != 0) {
                if (quote.Ask != 0) {
                    if (api.ServerDetails.getKey().ServerName.startsWith("LandFX")) {
                        double dif = 2 * Math.pow(10, -api.symbols.GetInfo(quote.Symbol).Digits);
                        quote.Bid += dif;
                        quote.Ask -= dif;
                    }
                    try {
                        api.OnQuoteCall(quote);
                    } catch (Exception e) {
                    }
                }
            }
        }

    }

    private void RecToQuote(TickRec rec, Quote quote) {
        SymbolInfo info = api.symbols.GetInfo(rec.Id);
        quote.Time = ConvertTo.toLocal(ConvertTo.DateTimeMs(rec.TimeMs), this.api.zoneId);

        quote.BankId = rec.BankId;
        quote.UpdateMask = rec.UpdateMask;
        //if ((rec.UpdateMask & 1)!=0)
        if (rec.Bid != 0) {
            quote.Bid = ConvertTo.LongLongToDouble(info.Digits, rec.Bid);
        }
        //if ((rec.UpdateMask & 2)!=0)
        if (rec.Ask != 0) {
            quote.Ask = ConvertTo.LongLongToDouble(info.Digits, rec.Ask);
        }
        //if ((rec.UpdateMask & 4) != 0)
        if (rec.Last != 0) {
            quote.Last = ConvertTo.LongLongToDouble(info.Digits, rec.Last);
        }
        //if ((rec.UpdateMask & 8) != 0)
        if (rec.Volume != 0) {
            quote.Volume = rec.Volume;
        }

    }
}