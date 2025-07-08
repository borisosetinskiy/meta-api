package com.ob.api.mtx.mt4.connector.entity.dto;

import com.ob.broker.common.model.ContractData;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.math.BigDecimal;


@Data
@ToString(callSuper = true)
@EqualsAndHashCode(of = "symbolIndex", callSuper = true)
public class Instrument extends ContractData {
    private String description;
    private String source;
    private int symbolGroup;    // security group (see ConSymbolGroup)
    private int tradeMode; // trade mode
    private short symbolIndex;
    private int symbolIndexOriginal;    // symbols index in market watch
    private int realTime;                    // allow real time quotes
    private int starting; // trades starting date (UNIX time)
    private int expiration; // trades end date      (UNIX time)
    private int profitMode; // profit calculation mode
    private int profitReserved;
    private int spread;
    private int spreadBalance;
    private int exeMode;
    private int swapEnable;
    private int swapType;
    private BigDecimal swapLong;
    private BigDecimal swapShort;
    private int swapRollover3day;// triple rollover day: 0-Monday, 1-Tuesday ... 4-Friday
    private int stopsLevel;
    private int gtcPending;// GTC mode ORDERS_DAILY, ORDERS_GTC, ORDERS_DAILY_NO_STOPS
    private int marginMode;// margin calculation mode
    private BigDecimal marginInitial;
    private BigDecimal marginMaintenance;
    private BigDecimal marginHedged;
    //    private BigDecimal marginDivider;
    private double point;/// point size - (1/(10^digits)
    private BigDecimal multiply;/// multiply 10^digits
    private BigDecimal bidTickValue;
    private BigDecimal askTickValue;
    private int instantMaxVolume;
    private int freezeLevel;/// modification freeze level (from market price)
    private int marginHedgedStrong;/// lock open checking mode
    private int valueDate;/// value date for this security
    private int quotesDelay;
    private int swapOpenPrice;  /// use open price at swaps calculation in SWAP_BY_INTEREST mode
    private double pointHalf;


    public void setPoint(double point) {
        this.point = point;
        pointHalf = point / 2;
    }


}
