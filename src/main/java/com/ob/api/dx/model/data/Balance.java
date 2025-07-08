package com.ob.api.dx.model.data;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.math.BigDecimal;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class Balance {
    String account;
    int version;
    BigDecimal value;
    String currency;
}
/*
{
          "account": "default:m_1056663",
          "version": 28,
          "value": 100003.87,
          "currency": "USD"
        }
 */