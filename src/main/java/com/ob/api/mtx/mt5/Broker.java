package com.ob.api.mtx.mt5;

import lombok.Data;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

public class Broker {

    private static final char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();

    public static String Search(String company) throws IOException, NoSuchAlgorithmException, KeyManagementException {
        String ver = "mt5";
        String req = "company=" + company + "&code=" + ver;
        byte[] sign = GetSignature(req.getBytes(StandardCharsets.US_ASCII));
        req += "&signature=";
        for (byte item : sign) {
            String b = String.format("%X", item).toLowerCase();
            if (b.length() == 1) {
                b = "0" + b;
            }
            req += b;
        }
        req += "&ver=2";
        URL obj = new URL("https://updates.metaquotes.net/public/" + ver + "/network");
        HttpsURLConnection con = (HttpsURLConnection) obj.openConnection();
        SSLContext sc = SSLContext.getInstance("TLSv1.2"); //$NON-NLS-1$
        sc.init(null, null, new java.security.SecureRandom());
        con.setSSLSocketFactory(sc.getSocketFactory());
        con.setRequestMethod("POST");
        con.setRequestProperty("Accept", "*/*");
        con.setRequestProperty("Accept-Encoding", "gzip, deflate");
        con.setRequestProperty("Accept-Language", "en");
        con.setRequestProperty("UserAgent", "MetaTrader 4 Terminal/4.1380 (Windows NT 6.2.9200; x64");
        con.setRequestProperty("Cookie", GetCookies());
        con.setRequestProperty("ContentType", "application/x-www-form-urlencoded");
        con.setDoOutput(true);
        return readCompanyData(company, con, req);
    }

    private static String readCompanyData(String company, HttpsURLConnection con, String req) throws IOException {
        try (DataOutputStream wr = new DataOutputStream(con.getOutputStream())) {
            wr.writeBytes(req);
            wr.flush();
        }
        int responseCode = con.getResponseCode();
        if (responseCode != 200)
            throw new IOException("Broker.Search( " + company + ")" + responseCode);
        StringBuilder response = new StringBuilder();
        try (BufferedReader in = new BufferedReader(
                new InputStreamReader(con.getInputStream()))) {
            String inputLine;

            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
        }
        String r = response.toString();
        r = r.substring(r.indexOf("{"));
        return r;
    }

    static String GetCookies() {
        RuntimeMXBean runtimeMX = ManagementFactory.getRuntimeMXBean();
        Date start = new Date(runtimeMX.getStartTime());
        long ticks = (new Date()).getTime() - start.getTime();
        long time = (long) Duration.between(LocalDateTime.of(1970, 1, 1, 0, 0, 0),
                LocalDateTime.now()).toMillis() / 1000 - 16436 * 24 * 3600;
        long softid = time | ((ticks & 0x1FFFFFF) << 32) | 0x4200000000000000L;
        byte[] key = CreateHardId();
        String commonKey = bytesToHex(key);
        commonKey = commonKey.substring(0, 16);
        long age = (long) Duration.between(LocalDateTime.of(1970, 1, 1, 0, 0, 0),
                LocalDateTime.now()).toMillis() / 1000 - 24 * 3600;
        return "_fz_uniq=" + softid + ";uniq=" + softid + ";age=" + age + ";tid=" + commonKey + ";";
    }

    public static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = HEX_ARRAY[v >>> 4];
            hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
        }
        return new String(hexChars);
    }

    static byte[] CreateHardId() {
        long seed = (new Date()).getTime();
        byte[] data = new byte[256];
        for (int i = 0; i < 256; i++) {
            seed = seed * 214013 + 2531011;
            data[i] = (byte) ((seed >> 16) & 0xFF);
        }
        byte[] _HardId = MD5.computeMD5(data);
        _HardId[0] = 0;
        for (int i = 1; i < 16; i++)
            _HardId[0] += _HardId[i];
        return _HardId;
    }

    private static byte[] GetSignature(byte[] data) {

        byte[] key = MD5.computeMD5(data);
        byte[] sign = {0x3D, 0x7B, 0x15, 0x16, (byte) 0xD6, (byte) 0xEA, (byte) 0xBB, 0x34, (byte) 0xD9, (byte) 0xD6, 0x63, (byte) 0xE3, 0x62, 0x3E, 0x1B, (byte) 0xD7, (byte) 0xFB, (byte) 0xDC, (byte) 0xAE, (byte) 0xF4, 0x57, 0x3B, (byte) 0xDF, 0x35, 0x7F, (byte) 0xA8, (byte) 0xCF, 0x0B, (byte) 0xBE, (byte) 0xAD, (byte) 0x92, 0x7F};
        byte[] d = new byte[48];
        System.arraycopy(key, 0, d, 0, 16);
        System.arraycopy(sign, 0, d, 16, 32);
        return MD5.computeMD5(d);
    }

    @Data
    public static class Result {
        private String name;
        private List<String> access;
    }

    @Data
    public static class Company {
        private String company;
        private List<Result> results;
    }

    @Data
    public static class Companies {
        private List<Company> result;
    }
}
