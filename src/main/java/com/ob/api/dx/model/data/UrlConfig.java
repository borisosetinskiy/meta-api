package com.ob.api.dx.model.data;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UrlConfig {
    String messageWebSocketUrl;
    String restApiUrl;
    String authUrl;
}
