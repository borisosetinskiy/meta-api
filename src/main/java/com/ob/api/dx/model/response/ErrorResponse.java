package com.ob.api.dx.model.response;

import lombok.Data;

@Data
public class ErrorResponse {
    Integer errorCode;
    String description;
}
