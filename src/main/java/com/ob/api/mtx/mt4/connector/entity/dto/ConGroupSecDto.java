package com.ob.api.mtx.mt4.connector.entity.dto;

import lombok.Data;

@Data
public class ConGroupSecDto {
    private int trade;
    private int show;
    private int execution;
    private double commBase;
    private int commType;
    private int commLots;
    private double commAgent;
    private int commAgentType;
    private int spreadDiff;
    private int lotMin;
    private int lotMax;
    private int lotStep;
    private int ieQuickMode;
    private double commTax;
    private int commAgentLots;
    // maximum price deviation in Instant Execution mode field
    private int maxPriceDeviation;
}
