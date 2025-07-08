package com.ob.broker.calculation.model;

import lombok.Getter;

@Getter
public enum Currency {
    EUR("eur"),
    USD("usd");

    final String name;

    Currency(String name) {
        this.name = name;
    }
}
