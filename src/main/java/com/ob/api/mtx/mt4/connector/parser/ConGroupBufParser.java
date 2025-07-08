package com.ob.api.mtx.mt4.connector.parser;

import com.ob.api.mtx.mt4.connector.entity.dto.ConGroupDto;
import com.ob.api.mtx.mt4.connector.entity.dto.ConGroupMarginDto;
import com.ob.api.mtx.mt4.connector.entity.dto.ConGroupSecDto;
import com.ob.api.mtx.mt4.connector.util.ByteUtil;

public class ConGroupBufParser {
    private static final int OFFSET_SIZE_WITH_GROUP_NAME = 16;
    private static final int OFFSET_SIZE_WITH_ENABLE_GROUP_FIELD = 20;
    private static final int OFFSET_SIZE_WITH_TRADE_CONFIRMATION_TIMEOUT = 24;
    private static final int OFFSET_SIZE_WITH_TRADE_ADVANCED_SECURITY = 28;
    private static final int OFFSET_SIZE_WITH_COMPANY_NAME = 156;
    private static final int OFFSET_SIZE_WITH_SIGNATURE = 412;
    private static final int OFFSET_SIZE_WITH_SMTP_SERVER = 476;
    private static final int OFFSET_SIZE_WITH_SMTP_LOGIN = 508;
    private static final int OFFSET_SIZE_WITH_SMTP_PASSWORD = 540;
    private static final int OFFSET_SIZE_WITH_SUPPORT_EMAIL = 604;
    private static final int OFFSET_SIZE_WITH_TEMPLATES = 636;
    private static final int OFFSET_SIZE_WITH_COPIES = 640;
    private static final int OFFSET_SIZE_WITH_STATEMENTS = 644;
    private static final int OFFSET_SIZE_WITH_LEVERAGE = 648;
    private static final int OFFSET_SIZE_WITH_DEFAULT_DEPOSIT = 656;
    private static final int OFFSET_SIZE_WITH_MAXSECURITIES = 660;
    private static final int OFFSET_SIZE_WITH_SPACE_1 = 4248;
    private static final int OFFSET_SIZE_WITH_SECMARGINS_TOTAL = 13468;
    private static final int OFFSET_SIZE_WITH_CURRENCY = 13480;
    private static final int OFFSET_SIZE_WITH_VIRTUAL_CREDIT = 13488;
    private static final int OFFSET_SIZE_WITH_MARGIN_CALL = 13492;
    private static final int OFFSET_SIZE_WITH_MARGIN_MODE = 13496;
    private static final int OFFSET_SIZE_WITH_MARGIN_STOPOUT = 13500;
    private static final int OFFSET_SIZE_WITH_SPACE_2 = 13504;
    private static final int OFFSET_SIZE_WITH_INTERSTATE = 13512;
    private static final int OFFSET_SIZE_WITH_USE_SWAP = 13516;
    private static final int OFFSET_SIZE_WITH_USE_NEWS = 13520;
    private static final int OFFSET_SIZE_WITH_RIGHTS = 13524;
    private static final int OFFSET_SIZE_WITH_CHECK_IE_PRICES = 13528;
    private static final int OFFSET_SIZE_WITH_MAX_POSITIONS = 13532;
    private static final int OFFSET_SIZE_WITH_CLOSE_REOPEN = 13536;
    private static final int OFFSET_SIZE_WITH_HEDGE_PROHIBITED = 13540;
    private static final int OFFSET_SIZE_WITH_CLOSE_INFO = 13544;
    private static final int OFFSET_SIZE_WITH_SECURITIES_HASH = 13572;
    private static final int OFFSET_SIZE_WITH_MARGIN_TYPE = 13576;
    private static final int OFFSET_SIZE_WITH_ARCHIVE_PERIOD = 13580;
    private static final int OFFSET_SIZE_WITH_ARCHIVE_MAX_BALANCE = 13584;
    private static final int OFFSET_SIZE_WITH_ARCHIVE_STOP_OUT_MODE = 13588;
    private static final int OFFSET_SIZE_WITH_PENDINGS_CLEAN_PERIOD = 13592;
    private static final int OFFSET_SIZE_WITH_COM_GROUP_RESERVED = 13696;

    private static final int STRING_GROUP_NAME_SIZE = 15;
    private static final int STRING_COMPANY_NAME_SIZE = 127;
    private static final int STRING_SMTP_PASSWORD_SIZE = 31;
    private static final int STRING_CURRENCY_SIZE = 11;

    private static final int SPACE = 4;
    private static final int INT_BYTE_SIZE = 4;
    private static final int DOUBLE_BYTE_SIZE = 8;
    private static final int MAX_SEC_GROUPS = 32;
    private static final int GROUPS_RESERVED_SIZE = 3;
    private static final int MAX_SEC_GROUPS_MARGIN = 128;
    private static final int GROUPS_MARGIN_SYMBOL_SIZE = 12;
    private static final int GROUPS_MARGIN_RESERVED_SIZE = 7;
    private static final int UNUSED_RIGHTS_SIZE = 3;
    private static final int SECURITIES_HASH_SIZE = 16;

    public static ConGroupDto parse(byte[] conGroupArray, int offset) {
        ConGroupDto conGroupDto = new ConGroupDto();

//        int maxLength = data.length - offset;
//        byte[] conGroupArray = new byte[maxLength];
//        System.arraycopy(data, offset, conGroupArray, 0, maxLength);

        //   int maxLength = data.length - offset;
        // System.arraycopy(data, offset, conGroupArray, 0, maxLength);


//        int internalOffset = offset;

        byte[] groupNameBytes = ByteUtil.getNotReversedBytes(conGroupArray, offset, STRING_GROUP_NAME_SIZE);
        String groupNameStr = ByteUtil.byteToString(groupNameBytes, 0);
        conGroupDto.setGroupName(groupNameStr);

//        // enable group field
//        internalOffset = OFFSET_SIZE_WITH_ENABLE_GROUP_FIELD;
//
//        // trade confirmation timeout (seconds) field
//        internalOffset = OFFSET_SIZE_WITH_TRADE_CONFIRMATION_TIMEOUT;
//
//        // enable advanced security field
//        internalOffset = OFFSET_SIZE_WITH_TRADE_ADVANCED_SECURITY;

        // company name field
        byte[] companyNameBytes = ByteUtil.getNotReversedBytes(conGroupArray
                , OFFSET_SIZE_WITH_TRADE_ADVANCED_SECURITY + offset, STRING_COMPANY_NAME_SIZE);
        String companyNameStr = ByteUtil.byteToString(companyNameBytes, 0);
        conGroupDto.setCompany(companyNameStr);

//        // statements signature field
//        internalOffset = OFFSET_SIZE_WITH_SIGNATURE;
//
//        // statements SMTP server
//        internalOffset = OFFSET_SIZE_WITH_SMTP_SERVER;
//
//        // statements SMTP login field
//        internalOffset = OFFSET_SIZE_WITH_SMTP_LOGIN;

        // statements SMTP password field
        byte[] smtpPassBytes = ByteUtil.getNotReversedBytes(conGroupArray
                , OFFSET_SIZE_WITH_SMTP_LOGIN + offset, STRING_SMTP_PASSWORD_SIZE);
        String smtpPassStr = ByteUtil.byteToString(smtpPassBytes, 0);
        conGroupDto.setSmtpPassword(smtpPassStr);
//
//        // support email field
//        internalOffset = OFFSET_SIZE_WITH_SUPPORT_EMAIL;
//
//        // path to directory with custom templates field
//        internalOffset = OFFSET_SIZE_WITH_TEMPLATES;
//
//        // copyAndReset statements on support email field
//        internalOffset = OFFSET_SIZE_WITH_COPIES;
//
//        // enable statements field
//        internalOffset = OFFSET_SIZE_WITH_STATEMENTS;
//
//        // default leverage field
//        internalOffset = OFFSET_SIZE_WITH_LEVERAGE;

        // default deposit field
        double defaultDeposit = ByteUtil.getDouble(conGroupArray, OFFSET_SIZE_WITH_LEVERAGE + offset);
        conGroupDto.setDefaultDeposit(defaultDeposit);

//        // maximum simultaneous securities field
//        internalOffset = OFFSET_SIZE_WITH_MAXSECURITIES;

        int offsetForGroup = OFFSET_SIZE_WITH_MAXSECURITIES;
        ConGroupSecDto[] conGroupSecDtoArray = new ConGroupSecDto[MAX_SEC_GROUPS];
        // security group settings
        for (int i = 0; i < MAX_SEC_GROUPS; i++) {
            offsetForGroup += SPACE;

            ConGroupSecDto conGroupSec = new ConGroupSecDto();

            int show = ByteUtil.getInt(conGroupArray, offsetForGroup);
            offsetForGroup += INT_BYTE_SIZE;
            conGroupSec.setShow(show);

            int trade = ByteUtil.getInt(conGroupArray, offsetForGroup);
            offsetForGroup += INT_BYTE_SIZE;
            conGroupSec.setTrade(trade);

            int execution = ByteUtil.getInt(conGroupArray, offsetForGroup);
            offsetForGroup += INT_BYTE_SIZE;
            conGroupSec.setExecution(execution);


            offsetForGroup += SPACE;

            double comm_base = ByteUtil.getDouble(conGroupArray, offsetForGroup);
            offsetForGroup += DOUBLE_BYTE_SIZE;
            conGroupSec.setCommBase(comm_base);

            int comm_type = ByteUtil.getInt(conGroupArray, offsetForGroup);
            offsetForGroup += INT_BYTE_SIZE;
            conGroupSec.setCommType(comm_type);

            int comm_lots = ByteUtil.getInt(conGroupArray, offsetForGroup);
            offsetForGroup += INT_BYTE_SIZE;
            conGroupSec.setCommLots(comm_lots);

            double comm_agent = ByteUtil.getDouble(conGroupArray, offsetForGroup);
            offsetForGroup += DOUBLE_BYTE_SIZE;
            conGroupSec.setCommAgent(comm_agent);

            int comm_agent_type = ByteUtil.getInt(conGroupArray, offsetForGroup);
            offsetForGroup += INT_BYTE_SIZE;
            conGroupSec.setCommAgentType(comm_agent_type);

            int spread_diff = ByteUtil.getInt(conGroupArray, offsetForGroup);
            offsetForGroup += INT_BYTE_SIZE;
            conGroupSec.setSpreadDiff(spread_diff);

            int lot_min = ByteUtil.getInt(conGroupArray, offsetForGroup);
            offsetForGroup += INT_BYTE_SIZE;
            conGroupSec.setLotMin(lot_min);

            int lot_max = ByteUtil.getInt(conGroupArray, offsetForGroup);
            offsetForGroup += INT_BYTE_SIZE;
            conGroupSec.setLotMax(lot_max);


            int lot_step = ByteUtil.getInt(conGroupArray, offsetForGroup);
            offsetForGroup += INT_BYTE_SIZE;
            conGroupSec.setLotStep(lot_step);

            // maximum price deviation in Instant Execution mode field
            int maxPriceDeviation = ByteUtil.getInt(conGroupArray, offsetForGroup);
            offsetForGroup += INT_BYTE_SIZE;
            conGroupSec.setMaxPriceDeviation(maxPriceDeviation);


            // use confirmation in Request mode field
            offsetForGroup += INT_BYTE_SIZE;

            // clients trade rights-bit mask see TRADE_DENY_NONE,TRADE_DENY_CLOSEBY,TRADE_DENY_MUCLOSEBY field
            offsetForGroup += INT_BYTE_SIZE;

            int ie_quick_mode = ByteUtil.getInt(conGroupArray, offsetForGroup);
            offsetForGroup += INT_BYTE_SIZE;
            conGroupSec.setIeQuickMode(ie_quick_mode);

            // auto close-out modes see CLOSE_OUT_NONE,CLOSE_OUT_HIHI, CLOSE_OUT_LOLO, CLOSE_OUT_HILO, CLOSE_OUT_LOHI field
            offsetForGroup += INT_BYTE_SIZE;

            double comm_tax = ByteUtil.getDouble(conGroupArray, offsetForGroup);
            offsetForGroup += DOUBLE_BYTE_SIZE;
            conGroupSec.setCommTax(comm_tax);

            int comm_agent_lots = ByteUtil.getInt(conGroupArray, offsetForGroup);
            offsetForGroup += INT_BYTE_SIZE;
            conGroupSec.setCommAgentLots(comm_agent_lots);

            // free margin check mode field
            offsetForGroup += INT_BYTE_SIZE;

            for (int j = 0; j < GROUPS_RESERVED_SIZE; j++)
                // reserved field
                offsetForGroup += INT_BYTE_SIZE;

            conGroupSecDtoArray[i] = conGroupSec;
        }
        conGroupDto.setSecgroups(conGroupSecDtoArray);
        {
            int offsetForMargin = OFFSET_SIZE_WITH_SPACE_1 + offset;
            ConGroupMarginDto[] conGroupMarginDtoArray = new ConGroupMarginDto[MAX_SEC_GROUPS_MARGIN];
            // special securities settings
            for (int i = 0; i < MAX_SEC_GROUPS_MARGIN; i++) {
                ConGroupMarginDto conGroupMargin = new ConGroupMarginDto();

                // security
                byte[] security = ByteUtil.getNotReversedBytes(conGroupArray, offsetForMargin, GROUPS_MARGIN_SYMBOL_SIZE);
                offsetForMargin += GROUPS_MARGIN_SYMBOL_SIZE;
                conGroupMargin.setSymbol(security);

                offsetForMargin += SPACE;

                double swap_long = ByteUtil.getDouble(conGroupArray, offsetForMargin);
                offsetForMargin += DOUBLE_BYTE_SIZE;
                conGroupMargin.setSwapLong(swap_long);

                double swap_short = ByteUtil.getDouble(conGroupArray, offsetForMargin);
                offsetForMargin += DOUBLE_BYTE_SIZE;
                conGroupMargin.setSwapShort(swap_short);

                double margin_divider = ByteUtil.getDouble(conGroupArray, offsetForMargin);
                offsetForMargin += DOUBLE_BYTE_SIZE;
                conGroupMargin.setMarginDivider(margin_divider);

                for (int j = 0; j < GROUPS_MARGIN_RESERVED_SIZE; j++)
                    // reserved field
                    offsetForMargin += INT_BYTE_SIZE;

                conGroupMarginDtoArray[i] = conGroupMargin;

                offsetForMargin += SPACE;
            }
            conGroupDto.setConGroupMargin(conGroupMarginDtoArray);
        }


        byte[] currencyBytes = ByteUtil.getNotReversedBytes(conGroupArray
                , OFFSET_SIZE_WITH_SECMARGINS_TOTAL + offset, STRING_CURRENCY_SIZE);
        String currencyStr = ByteUtil.byteToString(currencyBytes, 0);
        conGroupDto.setCurrency(currencyStr);


        int marginCall = ByteUtil.getInt(conGroupArray, OFFSET_SIZE_WITH_VIRTUAL_CREDIT + offset);
        conGroupDto.setMarginCall(marginCall);

        int marginMode = ByteUtil.getInt(conGroupArray, OFFSET_SIZE_WITH_MARGIN_CALL + offset);
        conGroupDto.setMarginMode(marginMode);


        int marginStopout = ByteUtil.getInt(conGroupArray, OFFSET_SIZE_WITH_MARGIN_MODE + offset);
        conGroupDto.setMarginStopout(marginStopout);

        double interstate = ByteUtil.getDouble(conGroupArray, OFFSET_SIZE_WITH_SPACE_2 + offset);
        conGroupDto.setInterestRate(interstate);

        int use_swap = ByteUtil.getInt(conGroupArray, OFFSET_SIZE_WITH_INTERSTATE + offset);
        conGroupDto.setUseSwap(use_swap);

        int news = ByteUtil.getInt(conGroupArray, OFFSET_SIZE_WITH_USE_SWAP + offset);
        conGroupDto.setNews(news);

        int rights = ByteUtil.getInt(conGroupArray, OFFSET_SIZE_WITH_USE_NEWS + offset);
        conGroupDto.setRights(rights);

        int check_ie_prices = ByteUtil.getInt(conGroupArray, OFFSET_SIZE_WITH_RIGHTS + offset);
        conGroupDto.setCheckIePrices(check_ie_prices);


        int maxpositions = ByteUtil.getInt(conGroupArray, OFFSET_SIZE_WITH_CHECK_IE_PRICES + offset);
        conGroupDto.setMaxPositions(maxpositions);

        int close_reopen = ByteUtil.getInt(conGroupArray, OFFSET_SIZE_WITH_MAX_POSITIONS + offset);
        conGroupDto.setCloseReopen(close_reopen);

        int hedgeProhibited = ByteUtil.getInt(conGroupArray, OFFSET_SIZE_WITH_CLOSE_REOPEN + offset);
        conGroupDto.setHedgeProhibited(hedgeProhibited);

        int closeFifo = ByteUtil.getInt(conGroupArray, OFFSET_SIZE_WITH_HEDGE_PROHIBITED + offset);
        conGroupDto.setCloseFifo(closeFifo);


        int marginType = ByteUtil.getInt(conGroupArray, OFFSET_SIZE_WITH_SECURITIES_HASH + offset);
        conGroupDto.setMarginType(marginType);

        return conGroupDto;
    }

    public static ConGroupDto parseConGroup(byte[] buf, String name) {
        ConGroupDto result = null;
        int count = buf.length / 0x3590;
        for (int i = 0; i < count; i++) {
            int type = ByteUtil.getInt(buf, i * 0x3590 + 4);
            if (type != 2)
                continue;
            ConGroupDto conGroupDto = ConGroupBufParser.parse(buf, i * 0x3590 + 16);
            if (name.equals(conGroupDto.getGroupName()))
                result = conGroupDto;
        }
        return result;
    }
}
