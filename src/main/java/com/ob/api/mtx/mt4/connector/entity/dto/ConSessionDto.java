package com.ob.api.mtx.mt4.connector.entity.dto;

import lombok.Data;

@Data
public class ConSessionDto {
    private short openHour, openMin;          // session open  time: hour & minute
    private short closeHour, closeMin;        // session close time: hour & minute
}
