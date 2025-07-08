package com.ob.api.mtx.mt4.connector.parser;

import com.ob.api.mtx.mt4.connector.entity.dto.Instrument;
import com.ob.api.mtx.mt4.connector.util.ByteUtil;
import com.ob.broker.common.model.ContractType;
import com.ob.broker.common.model.ExecDataType;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

@Slf4j
public class InstrumentBufParser {
    public static final int INSTRUMENT_ENTITY_SIZE = 1936;

    private static final int OFFSET_SIZE_WITH_SYMBOL = 12;
    private static final int OFFSET_SIZE_WITH_DESCRIPTION = 76;
    private static final int OFFSET_SIZE_WITH_SOURCE = 88;
    private static final int OFFSET_SIZE_WITH_CURRENCY = 100;
    private static final int OFFSET_SIZE_WITH_SYMBOL_GROUP = 104;
    private static final int OFFSET_SIZE_WITH_SYMBOL_DIGITS = 108;
    private static final int OFFSET_SIZE_WITH_TRADE_MODE = 112;
    private static final int OFFSET_SIZE_WITH_MISSING_BACKGROUND_COLOR = 116;
    private static final int OFFSET_SIZE_WITH_SYMBOL_INDEX = 120;
    private static final int OFFSET_SIZE_WITH_SYMBOL_INDEX_ORIGINAL = 124;
    private static final int OFFSET_SIZE_WITH_MISSING_EXTERNAL_UNUSED = 152;
    private static final int OFFSET_SIZE_WITH_REALTIME = 156;
    private static final int OFFSET_SIZE_WITH_STARTING = 160;
    private static final int OFFSET_SIZE_WITH_EXPIRATION = 164;
    private static final int OFFSET_SIZE_WITH_MISSING_CONSESSIONS_ARRAY = 1620;
    private static final int OFFSET_SIZE_WITH_PROFIT_MODE = 1624;
    private static final int OFFSET_SIZE_WITH_PROFIT_RESERVED = 1628;
    private static final int OFFSET_SIZE_WITH_MISSING_CSHARP_VARIABLES = 1660;
    private static final int OFFSET_SIZE_WITH_SPREAD = 1664;
    private static final int OFFSET_SIZE_WITH_SPREAD_BALANCE = 1668;
    private static final int OFFSET_SIZE_WITH_EXE_MODE = 1672;
    private static final int OFFSET_SIZE_WITH_SWAP_ENABLE = 1676;
    private static final int OFFSET_SIZE_WITH_SWAP_TYPE = 1680;
    private static final int OFFSET_SIZE_WITH_SWAP_LONG = 1688;
    private static final int OFFSET_SIZE_WITH_SWAP_SHORT = 1696;
    private static final int OFFSET_SIZE_WITH_SWAP_ROLLOVER3DAY = 1700;
    private static final int OFFSET_SIZE_WITH_MISSING_EXTRA_BYTES_1 = 1704;
    private static final int OFFSET_SIZE_WITH_CONTRACT_SIZE = 1712;
    private static final int OFFSET_SIZE_WITH_TICK_VALUE = 1720;
    private static final int OFFSET_SIZE_WITH_TICK_SIZE = 1728;
    private static final int OFFSET_SIZE_WITH_STOPS_LEVEL = 1732;
    private static final int OFFSET_SIZE_WITH_GTC_PENDING = 1736;
    private static final int OFFSET_SIZE_WITH_MARGIN_MODE = 1740;
    private static final int OFFSET_SIZE_WITH_MISSING_EXTRA_BYTES_2 = 1744;
    private static final int OFFSET_SIZE_WITH_MARGIN_INITIAL = 1752;
    private static final int OFFSET_SIZE_WITH_MARGIN_MAINTENANCE = 1760;
    private static final int OFFSET_SIZE_WITH_MARGIN_HEDGED = 1768;
    private static final int OFFSET_SIZE_WITH_MARGIN_DIVIDER = 1776;
    private static final int OFFSET_SIZE_WITH_POINT = 1784;
    private static final int OFFSET_SIZE_WITH_MULTIPLY = 1792;
    private static final int OFFSET_SIZE_WITH_BID_TICK_VALUE = 1800;
    private static final int OFFSET_SIZE_WITH_ASK_TICK_VALUE = 1808;
    private static final int OFFSET_SIZE_WITH_LONG_ONLY = 1812;
    private static final int OFFSET_SIZE_WITH_INSTANT_MAX_VOLUME = 1816;
    private static final int OFFSET_SIZE_WITH_MARGIN_CURRENCY = 1828;
    private static final int OFFSET_SIZE_WITH_FREEZE_LEVEL = 1832;
    private static final int OFFSET_SIZE_WITH_MARGIN_HEDGED_STRONG = 1836;
    private static final int OFFSET_SIZE_WITH_VALUE_DATE = 1840;
    private static final int OFFSET_SIZE_WITH_QUOTES_DELAY = 1844;
    private static final int OFFSET_SIZE_WITH_SWAP_OPEN_PRICE = 1848;
    private static final int OFFSET_SIZE_WITH_MISSING_EXTRA_BYTES = 1936;

    private static final int STRING_DESCRIPTION_SIZE = 63;
    private static final int STRING_SOURCE_SIZE = 11;
    private static final int STRING_CURRENCY_SIZE = 11;
    private static final int STRING_MARGIN_CURRENCY_SIZE = 11;

    private static final int SYMBOL_SIZE = 12;

    public static Instrument parse(byte[] instrumentArray, int offset) {
        Instrument instrument = new Instrument();

//        int maxLength = data.length - offset;
//        byte[] instrumentArray = new byte[maxLength];
//        System.arraycopy(data, offset, instrumentArray, 0, maxLength);

//        int maxLength = data.length - offset;

//        int internalOffset = offset;

        //Symbol Field
        byte[] symbol = ByteUtil.getNotReversedBytes(instrumentArray, offset, SYMBOL_SIZE);
        instrument.setSymbol(ByteUtil.byteToString(symbol, 0));

        //Description Field
        byte[] description = ByteUtil.getNotReversedBytes(instrumentArray
                , OFFSET_SIZE_WITH_SYMBOL + offset
                , STRING_DESCRIPTION_SIZE);
        instrument.setDescription(ByteUtil.byteToString(description, 0));

        //Source Field
        byte[] source = ByteUtil.getNotReversedBytes(instrumentArray, OFFSET_SIZE_WITH_DESCRIPTION + offset, STRING_SOURCE_SIZE);
        instrument.setSource(ByteUtil.byteToString(source, 0));

        //Currency Field
        byte[] currency = ByteUtil.getNotReversedBytes(instrumentArray, OFFSET_SIZE_WITH_SOURCE + offset, STRING_CURRENCY_SIZE);
        instrument.setCurrency(ByteUtil.byteToString(currency, 0));

        //Symbol Group Field
        int symbolGroup = ByteUtil.getInt(instrumentArray, OFFSET_SIZE_WITH_CURRENCY + offset);
        instrument.setSymbolGroup(symbolGroup);

        //Digits Mode Field
        int digits = ByteUtil.getInt(instrumentArray, OFFSET_SIZE_WITH_SYMBOL_GROUP + offset);
        instrument.setDigits(digits);

        //Trade Mode Field
        int tradeMode = ByteUtil.getInt(instrumentArray, OFFSET_SIZE_WITH_SYMBOL_DIGITS + offset);
        instrument.setTradeMode(tradeMode);
        //missing 4 bytes for background color

        //Symbol Index Field
        int symbolIndex = ByteUtil.getInt(instrumentArray, OFFSET_SIZE_WITH_MISSING_BACKGROUND_COLOR + offset);
        instrument.setSymbolIndex((short) symbolIndex);

        //Symbol Index Original Field
        int symbolIndexOriginal = ByteUtil.getInt(instrumentArray, OFFSET_SIZE_WITH_SYMBOL_INDEX + offset);

        instrument.setSymbolIndexOriginal(symbolIndexOriginal);


        //Real Time Field
        int realTime = ByteUtil.getInt(instrumentArray, OFFSET_SIZE_WITH_MISSING_EXTERNAL_UNUSED + offset);
        instrument.setRealTime(realTime);

        //Starting Field
        int starting = ByteUtil.getInt(instrumentArray, OFFSET_SIZE_WITH_REALTIME + offset);
        instrument.setStarting(starting);

        //Expiration Field
        int expiration = ByteUtil.getInt(instrumentArray, OFFSET_SIZE_WITH_STARTING + offset);
        instrument.setExpiration(expiration);


        //Profit Mode Field
        int profitMode = ByteUtil.getInt(instrumentArray, OFFSET_SIZE_WITH_MISSING_CONSESSIONS_ARRAY + offset);
        instrument.setProfitMode(profitMode);

        //Profit Reserved
        int profitReserved = ByteUtil.getInt(instrumentArray, OFFSET_SIZE_WITH_PROFIT_MODE + offset);
        instrument.setProfitReserved(profitReserved);

        //28 + 4th extra bytes
//        internalOffset = OFFSET_SIZE_WITH_MISSING_CSHARP_VARIABLES;

        //Spread Reserved
        int spread = ByteUtil.getInt(instrumentArray, OFFSET_SIZE_WITH_MISSING_CSHARP_VARIABLES + offset);
        instrument.setSpread(spread);

        //Spread Balance
        int spreadBalance = ByteUtil.getInt(instrumentArray, OFFSET_SIZE_WITH_SPREAD + offset);
        instrument.setSpreadBalance(spreadBalance);

        int exeMode = ByteUtil.getInt(instrumentArray, OFFSET_SIZE_WITH_SPREAD_BALANCE + offset);
        instrument.setExeMode(exeMode);
        instrument.setExecDataType(ExecDataType.fromValue(exeMode)); // 0 - ExecutionType.REQUEST, 1 - ExecutionType.INSTANT, 2 -


        int swapEnable = ByteUtil.getInt(instrumentArray, OFFSET_SIZE_WITH_EXE_MODE + offset);
        instrument.setSwapEnable(swapEnable);

        int swapType = ByteUtil.getInt(instrumentArray, OFFSET_SIZE_WITH_SWAP_ENABLE + offset);
        instrument.setSwapType(swapType);

        double swapLong = ByteUtil.getDouble(instrumentArray, OFFSET_SIZE_WITH_SWAP_TYPE + offset);
        instrument.setSwapLong(BigDecimal.valueOf(swapLong));

        double swapShort = ByteUtil.getDouble(instrumentArray, OFFSET_SIZE_WITH_SWAP_LONG + offset);
        instrument.setSwapShort(BigDecimal.valueOf(swapShort));

        int swapRollover3day = ByteUtil.getInt(instrumentArray, OFFSET_SIZE_WITH_SWAP_SHORT + offset);
        instrument.setSwapRollover3day(swapRollover3day);

        //extra bytes
//        internalOffset = OFFSET_SIZE_WITH_MISSING_EXTRA_BYTES_1;

        double contractSize = ByteUtil.getDouble(instrumentArray, OFFSET_SIZE_WITH_MISSING_EXTRA_BYTES_1 + offset);
        instrument.setContractSize(contractSize > 0 ? BigDecimal.valueOf((int) contractSize) : BigDecimal.ONE);

        double tickValue = ByteUtil.getDouble(instrumentArray, OFFSET_SIZE_WITH_CONTRACT_SIZE + offset);
        instrument.setTickPrice(BigDecimal.valueOf(tickValue));

        double tickSize = ByteUtil.getDouble(instrumentArray, OFFSET_SIZE_WITH_TICK_VALUE + offset);
        instrument.setTickSize(BigDecimal.valueOf(tickSize));

        int stopsLevel = ByteUtil.getInt(instrumentArray, OFFSET_SIZE_WITH_TICK_SIZE + offset);
        instrument.setStopsLevel(stopsLevel);

        int gtcPending = ByteUtil.getInt(instrumentArray, OFFSET_SIZE_WITH_STOPS_LEVEL + offset);
        instrument.setGtcPending(gtcPending);

        int marginMode = ByteUtil.getInt(instrumentArray, OFFSET_SIZE_WITH_GTC_PENDING + offset);
        instrument.setMarginMode(marginMode);
        try {
            instrument.setContractType(ContractType.fromValue(marginMode));
        } catch (Exception e) {
            log.error("Failed to parse contract type", e);
        }

        double marginInitial = ByteUtil.getDouble(instrumentArray, OFFSET_SIZE_WITH_MISSING_EXTRA_BYTES_2 + offset);
        instrument.setMarginInitial(BigDecimal.valueOf(marginInitial));

        double marginMaintenance = ByteUtil.getDouble(instrumentArray, OFFSET_SIZE_WITH_MARGIN_INITIAL + offset);
        instrument.setMarginMaintenance(BigDecimal.valueOf(marginMaintenance));

        double marginHedged = ByteUtil.getDouble(instrumentArray, OFFSET_SIZE_WITH_MARGIN_MAINTENANCE + offset);
        instrument.setMarginHedged(BigDecimal.valueOf(marginHedged));

        double marginDivider = ByteUtil.getDouble(instrumentArray, OFFSET_SIZE_WITH_MARGIN_HEDGED + offset);
        instrument.setMarginDivider(BigDecimal.valueOf(marginDivider));

        double point = ByteUtil.getDouble(instrumentArray, OFFSET_SIZE_WITH_MARGIN_DIVIDER + offset);
        instrument.setPoint(point);
        instrument.setPointSize(BigDecimal.valueOf(1 / Math.pow(10, digits)));

        if (tickSize == 0) {
            instrument.setTickSize(BigDecimal.valueOf(point));
        }


        double multiply = ByteUtil.getDouble(instrumentArray, OFFSET_SIZE_WITH_POINT + offset);
        instrument.setMultiply(BigDecimal.valueOf(multiply));

        double bidTickValue = ByteUtil.getDouble(instrumentArray, OFFSET_SIZE_WITH_MULTIPLY + offset);
        instrument.setBidTickValue(BigDecimal.valueOf(bidTickValue));

        double askTickValue = ByteUtil.getDouble(instrumentArray, OFFSET_SIZE_WITH_BID_TICK_VALUE + offset);
        instrument.setAskTickValue(BigDecimal.valueOf(askTickValue));

        int longOnly = ByteUtil.getInt(instrumentArray, OFFSET_SIZE_WITH_ASK_TICK_VALUE + offset);
        instrument.setLongOnly(longOnly == 1);

        int instantMaxVolume = ByteUtil.getInt(instrumentArray, OFFSET_SIZE_WITH_LONG_ONLY + offset);
        instrument.setInstantMaxVolume(instantMaxVolume);

        byte[] marginCurrency = ByteUtil.getNotReversedBytes(instrumentArray, OFFSET_SIZE_WITH_INSTANT_MAX_VOLUME + offset, STRING_MARGIN_CURRENCY_SIZE);

        instrument.setMarginCurrency(ByteUtil.byteToString(marginCurrency, 0));

        int freezeLevel = ByteUtil.getInt(instrumentArray, OFFSET_SIZE_WITH_MARGIN_CURRENCY + offset);
        instrument.setFreezeLevel(freezeLevel);

        int marginHedgedStrong = ByteUtil.getInt(instrumentArray, OFFSET_SIZE_WITH_FREEZE_LEVEL + offset);
        instrument.setMarginHedgedStrong(marginHedgedStrong);

        int valueDate = ByteUtil.getInt(instrumentArray, OFFSET_SIZE_WITH_MARGIN_HEDGED_STRONG + offset);
        instrument.setValueDate(valueDate);

        int quotesDelay = ByteUtil.getInt(instrumentArray, OFFSET_SIZE_WITH_VALUE_DATE + offset);
        instrument.setQuotesDelay(quotesDelay);

        int swapOpenPrice = ByteUtil.getInt(instrumentArray, OFFSET_SIZE_WITH_QUOTES_DELAY + offset);
        instrument.setSwapOpenPrice(swapOpenPrice);

        return instrument;
    }

    public static List<Instrument> parseInstruments(byte[] buf) throws Exception {
        if (buf == null)
            throw new IllegalArgumentException("Instruments buffer is null");
        int count = buf.length / 0x790;
        List<Instrument> instruments = new LinkedList<>();
        for (int i = 0; i < count; i++) {
            try {
                Instrument instrument = InstrumentBufParser.parse(buf, i * 0x790);
                instruments.add(instrument);
            } catch (Exception e) {
                log.error("Failed to parse instrument", e);
            }
        }
        return instruments;
    }

    public static List<Instrument> parseInstruments4Update(byte[] buf) throws Exception {
        int count = buf.length / 0x7A0;
        List<Instrument> instruments = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            int bufInd = ByteUtil.getInt(buf, i * 0x7A0 + 4);
            if (bufInd != 2)
                continue;
            Instrument instrument = InstrumentBufParser.parse(buf, i * 0x7A0 + 16);
            instruments.add(instrument);
        }
        return instruments;
    }

}
