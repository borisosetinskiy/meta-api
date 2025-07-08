package com.ob.api.mtx.mt5;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.Random;

public class ApplicationInfo {

    private static String generateRandomString(int length) {
        String candidateChars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder sb = new StringBuilder(length);
        Random random = new Random();
        for (int i = 0; i < length; i++) {
            sb.append(candidateChars.charAt(random.nextInt(candidateChars.length())));
        }
        return sb.toString();
    }

    private static String generateRandomNumericString(int length) {
        String candidateChars = "0123456789";
        StringBuilder sb = new StringBuilder(length);
        Random random = new Random();
        for (int i = 0; i < length; i++) {
            sb.append(candidateChars.charAt(random.nextInt(candidateChars.length())));
        }
        return sb.toString();
    }

    public static byte[] getApplicationInfo() {
        String info = "file=terminal64.exe\tversion=" +
                      Connection.CLIENT_BUILD +
                      "\tcert_company=MetaQuotes Ltd\tcert_issuer=DigiCert Trusted G4 Code Signing RSA4096 SHA384 2021 CA1\tcert_serial=04390a4c5f8906a1d7052c1768d45047\tos_ver=Windows 11 build 22" +
                      generateRandomNumericString(3) + "\tos_id="
                      + generateRandomNumericString(4) + "-"
                      + generateRandomNumericString(4) + "-"
                      + generateRandomNumericString(4)
                      + "-AAOEM\tcomputer=" + generateRandomString(4) + "\t\0";
        byte[] infoBytes;
        try {
            infoBytes = info.getBytes("UNICODE");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
        byte[] tmp = new byte[infoBytes.length - 3 + 1];
        if (tmp.length - 1 >= 0)
            System.arraycopy(infoBytes, 3, tmp, 0, tmp.length - 1);
        return tmp;
    }

    public static void main(String[] args) {
        byte[] appInfo = getApplicationInfo();
        System.out.println(new String(appInfo, StandardCharsets.UTF_16LE));
        // Print or use the byte array as needed
    }

}
