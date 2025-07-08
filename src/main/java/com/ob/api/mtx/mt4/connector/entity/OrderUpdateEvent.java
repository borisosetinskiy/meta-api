package com.ob.api.mtx.mt4.connector.entity;

import com.ob.api.mtx.mt4.connector.entity.dto.Mt4Order;
import com.ob.api.mtx.mt4.connector.entity.trading.TradingEvent;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderUpdateEvent {
    private Mt4Order mt4Order;
    private TradingEvent action;
    private BigDecimal balance;
    private BigDecimal credit;
}
