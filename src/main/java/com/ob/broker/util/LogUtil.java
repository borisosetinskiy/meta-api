package com.ob.broker.util;

import java.util.function.Consumer;


public abstract class LogUtil {
    static final String ENV = System.getProperty("ENV") == null ? "DEFAULT" : System.getProperty("ENV");
    static final String APP_ID = System.getProperty("APP_ID") == null ? "DEFAULT" : System.getProperty("APP_ID");

    public static void log(
            final String logType
            , final String app
            , final Object accountId
            , final Object brokerId
            , Consumer<String> call) {
        call.accept(String.format("\"env\":\"%s\", \"app\":\"%s\", \"appId\":\"%s\", \"logType\":\"%s\", \"accountId\":%s, \"brokerId\":%s",
                ENV,
                app,
                APP_ID,
                logType,
                accountId,
                brokerId));
    }


}
