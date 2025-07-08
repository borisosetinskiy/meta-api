package com.ob.api.mtx.mt4.connector.entity.dto;

import lombok.Data;

@Data
public class ConGroupDto {
    private String groupName;
    private String company;
    private String smtpPassword;
    private double defaultDeposit;
    private ConGroupSecDto[] secgroups;
    private ConGroupMarginDto[] conGroupMargin;
    private int secMarginsTotal;
    private String currency;
    private int marginCall;
    private int marginMode;
    private int marginStopout;
    private double interestRate;
    private int useSwap;
    private int news;
    private int rights;
    private int checkIePrices;
    private int maxPositions;
    private int closeReopen;
    private int hedgeProhibited;
    private int closeFifo;
    private int[] unusedRights;
    private byte[] securitiesHash;
    private int marginType;
}
