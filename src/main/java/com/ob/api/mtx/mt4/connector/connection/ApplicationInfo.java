package com.ob.api.mtx.mt4.connector.connection;

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
        OutBuf buf = new OutBuf();
        String info = String.format("file=terminal.exe\tversion=%s\tcert_company=MetaQuotes Ltd\tcert_issuer=DigiCert Trusted G4 Code Signing RSA4096 SHA384 2021 CA1\tcert_serial=4750d468172c05d7a106895f4c0a3904\tos_ver=Windows 11 build 22%s\tos_id=%s-%s-%s-AAOEM\tdomain=\tcomputer=%s\t",
                ConnectionData.CURRENT_BUILD,
                generateRandomNumericString(3),
                generateRandomNumericString(4),
                generateRandomNumericString(4),
                generateRandomNumericString(4),
                generateRandomString(4)
        );

        byte[] b = info.getBytes(StandardCharsets.UTF_16LE);
        byte[] resized = new byte[b.length + 2];
        System.arraycopy(b, 0, resized, 0, b.length);

        buf.add((short) resized.length); // dataHdr.m_szData = (USHORT)data.GetSize();
        buf.add((short) 13); // dataHdr.m_nPackType = 13;
        buf.add((short) 0); // dataHdr.m_nDataType = 0;
        buf.add((short) 0); // dataHdr.m_nRandom = 0;
        buf.add(resized);

        return buf.toArray();
    }

    public static void main(String[] args) {
        byte[] appInfo = getApplicationInfo();
        System.out.println(new String(appInfo, StandardCharsets.UTF_16LE));
        // Print or use the byte array as needed
    }

}
