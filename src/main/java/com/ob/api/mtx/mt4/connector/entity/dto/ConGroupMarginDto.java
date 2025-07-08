package com.ob.api.mtx.mt4.connector.entity.dto;

import lombok.Data;

@Data
public class ConGroupMarginDto {
    private byte[] symbol;
    private double swapLong;
    private double swapShort;
    private double marginDivider;
}
