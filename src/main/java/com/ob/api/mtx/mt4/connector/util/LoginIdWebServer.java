package com.ob.api.mtx.mt4.connector.util;


import com.ob.broker.common.error.CodeException;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static com.ob.api.mtx.mt4.connector.entity.StatusCode.LOGIN_ID_FAIL;

public class LoginIdWebServer {

    public static Long decode(String loginIdServerUrl, byte[] pack, String command) {
        String url = loginIdServerUrl + command + "?guid=d555a78d-57a9-4781-bc41-efb7b7027eed";
        try {
            HttpURLConnection httpURLConnection = (HttpURLConnection) new URL(url).openConnection();
            httpURLConnection.setRequestMethod("POST");
            httpURLConnection.setRequestProperty("Content-Type", "application/text");
            httpURLConnection.setConnectTimeout(10000);
            httpURLConnection.setDoOutput(true);
            httpURLConnection.setDoInput(true);
            try (DataOutputStream writer
                         = new DataOutputStream(httpURLConnection.getOutputStream())) {
                String req = new String(Base64.getEncoder().encode(pack));
                writer.write(req.getBytes(StandardCharsets.US_ASCII));
                writer.flush();
            }
            try (InputStream reader = httpURLConnection.getInputStream();
                 ByteArrayOutputStream result = new ByteArrayOutputStream()) {
                byte[] buffer = new byte[256];
                int length;
                while ((length = reader.read(buffer)) != -1) {
                    result.write(buffer, 0, length);
                }
                String response = result.toString(StandardCharsets.UTF_8);
                return new BigDecimal(response).longValue();
            }
        } catch (Exception e) {
            throw new CodeException("LOGIN_EX_ID:" + e.getMessage(), LOGIN_ID_FAIL.getId());
        }
    }

}
