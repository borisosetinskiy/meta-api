package com.ob.broker.common.client.rest;


import java.io.IOException;

public class BadHttpException extends IOException {
    public BadHttpException(String message) {
        super(message);
    }
}
