package com.ob.broker.common.model;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ContractData {
    String symbol;
    BigDecimal contractSize;
    ExecDataType execDataType;
    MarketMode md;
    BigDecimal stopLevel;
    ContractType contractType;
    boolean longOnly;
    Integer digits;
    BigDecimal marginDivider;
    String marginCurrency;
    BigDecimal incrementAmount;
    BigDecimal tickPrice;
    BigDecimal tickSize;
    BigDecimal spreadScale;
    String currency;
    BigDecimal pointSize;
    Map<WorkSession.DayOfWeek, WorkSession> WorkSessions;
}