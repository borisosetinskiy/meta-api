package com.ob.api.dx.model.request;

import lombok.Data;

@Data
public class LoginRequest {
    final String username;
    final String domain;
    final String password;
}
