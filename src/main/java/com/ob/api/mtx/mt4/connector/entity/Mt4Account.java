package com.ob.api.mtx.mt4.connector.entity;


import com.ob.api.mtx.mt4.connector.entity.dto.ConGroupDto;
import com.ob.api.mtx.mt4.connector.entity.dto.ConSymbolGroupDto;
import lombok.Data;

import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicReference;

@Data
public class Mt4Account {
    public Long brokerId;
    public Long accountId;
    private AtomicReference<ConGroupDto> conGroupDto = new AtomicReference<>();
    private String name;
    private int leverage;
    private AtomicReference<BigDecimal> balance = new AtomicReference<>();
    private AtomicReference<BigDecimal> credit = new AtomicReference<>();
    private int accountMode;
    private ConSymbolGroupDto[] groups;

}
