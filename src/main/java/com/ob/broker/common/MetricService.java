package com.ob.broker.common;

public interface MetricService {
    default void increment(String name, String... tags) {
    }

    ;

    default void decrement(String name, String... tags) {
    }

    ;

    default void count(String name, long count, String... tags) {
    }

    ;

    default void record(String name, long time, String... tags) {
    }

    ;
}
