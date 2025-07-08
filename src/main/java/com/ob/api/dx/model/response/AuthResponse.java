package com.ob.api.dx.model.response;

import lombok.Data;

@Data
public class AuthResponse {
    String sessionToken;
    String timeout;
}
//{
//  "sessionToken": "5nju8has7osfr03kv66r5m8io7",
//  "timeout": "00:30:00"
//}