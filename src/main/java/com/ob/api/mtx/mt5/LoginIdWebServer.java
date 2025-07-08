package com.ob.api.mtx.mt5;


import com.ob.broker.common.error.CodeException;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Base64;

public class LoginIdWebServer {


    private static HttpURLConnection getHttpURLConnection(String Url) throws IOException {
        URL obj = new URL(Url);
        HttpURLConnection con = (HttpURLConnection) obj.openConnection();
        con.setRequestMethod("POST");
        con.setConnectTimeout(5000);
        con.setReadTimeout(5000);
        con.setRequestProperty("Content-Type", "text/plain;charset=UTF-8");
        // Send post request
        con.setDoOutput(true);
        return con;
    }

    public long Decode(String url, String guid, byte[] data) {

        String Url = url + "?guid=" + guid;
        try {
            HttpURLConnection con = getHttpURLConnection(Url);
            try (DataOutputStream wr = new DataOutputStream(con.getOutputStream())) {
                String str = Base64.getEncoder().encodeToString(data);

                wr.writeBytes(str);
                wr.flush();
            }
            int responseCode = con.getResponseCode();
            try (BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()))) {
                if (responseCode != 200)
                    throw new CodeException("LoginIdWebServer: " + responseCode, 404);
                String inputLine;
                StringBuilder response = new StringBuilder();
                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                String resp = response.toString();
                return new BigInteger(resp, 10).longValue();
            }
        } catch (Exception e) {
            throw new CodeException("LoginIdWebServer: " + e.getMessage(), -1);
        }
    }
}