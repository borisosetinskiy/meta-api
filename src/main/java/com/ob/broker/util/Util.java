package com.ob.broker.util;

import java.math.BigDecimal;
import java.util.concurrent.TimeUnit;

public class Util {
    public static final BigDecimal MINUS = BigDecimal.valueOf(-1);
    public static final long PRICE_TIMESTAMP_TIMEOUT_SEC = TimeUnit.MINUTES.toMillis(1);
    public static final long REQUEST_PRICE_TIMEOUT_SEC = 15;
    public static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    public static long toLong(BigDecimal v) {
        if (v == null) {
            return 0L;
        }
        return v.longValue();
    }

    public static double toDouble(BigDecimal v) {
        if (v == null) {
            return 0.0;
        }
        return v.doubleValue();
    }

    public static BigDecimal toBigDecimal(Integer v, int digit) {
        if (v == null)
            return null;
        else if (v == 0)
            return BigDecimal.ZERO;
        return BigDecimal.valueOf(v).movePointLeft(digit);
    }

    public static BigDecimal toBigDecimal(Long v, int digit) {
        if (v == null)
            return null;
        else if (v == 0)
            return BigDecimal.ZERO;
        return BigDecimal.valueOf(v).movePointLeft(digit);
    }

    public static Long toAmount(BigDecimal lot) {
        return lot.multiply(HUNDRED).longValue();
    }

    public static BigDecimal toLot(long amount) {
        return BigDecimal.valueOf(amount).divide(HUNDRED);
    }

    public static Integer toInt(BigDecimal v, int digit) {
        if (v == null || BigDecimal.ZERO.equals(v))
            return 0;
        return v.movePointRight(digit).intValue();
    }


    public static Long toLong(Object v) {
        switch (v) {
            case null -> {
                return 0L;
            }
            case Long l -> {
                return l;
            }
            case Integer i -> {
                return i.longValue();
            }
            case BigDecimal bigDecimal -> {
                return bigDecimal.longValue();
            }
            default -> {
                try {
                    return Long.parseLong(v.toString());
                } catch (Exception e) {
                    return 0L;
                }
            }
        }
    }

    public static Integer toInt(Object v) {
        switch (v) {
            case null -> {
                return 0;
            }
            case Integer i -> {
                return i;
            }
            case Long l -> {
                return l.intValue();
            }
            case BigDecimal bigDecimal -> {
                return bigDecimal.intValue();
            }
            default -> {
                try {
                    return Integer.parseInt(v.toString());
                } catch (Exception e) {
                    return 0;
                }
            }
        }

    }

    public static  boolean isMoreThanZero(BigDecimal v) {
        return v != null && v.compareTo(BigDecimal.ZERO) > 0;
    }

    public static  boolean isMoreThanZero(Long v) {
        return v != null && v > 0;
    }

    public static String toString(Object o){
        return String.valueOf(o);
    }

    public static int countDecimalDigits(BigDecimal number) {

        String[] parts = number.stripTrailingZeros().toPlainString().split("\\.");

        if (parts.length == 2) {
            return parts[1].length();
        } else {
            return 0; // No decimal part
        }
    }
}
