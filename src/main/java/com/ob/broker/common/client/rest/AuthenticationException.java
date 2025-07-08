package com.ob.broker.common.client.rest;

import java.io.IOException;

public class AuthenticationException extends IOException {
    public AuthenticationException(String message) {
        super(message);
    }
}
