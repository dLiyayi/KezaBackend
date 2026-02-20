package com.keza.common.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.Locale;

public final class MoneyUtil {

    private static final Locale KE_LOCALE = new Locale("en", "KE");

    private MoneyUtil() {}

    public static String formatKES(BigDecimal amount) {
        if (amount == null) return "KES 0.00";
        NumberFormat format = NumberFormat.getCurrencyInstance(KE_LOCALE);
        format.setCurrency(java.util.Currency.getInstance("KES"));
        return format.format(amount);
    }

    public static BigDecimal round(BigDecimal amount) {
        if (amount == null) return BigDecimal.ZERO;
        return amount.setScale(2, RoundingMode.HALF_UP);
    }

    public static BigDecimal percentage(BigDecimal amount, BigDecimal percent) {
        return round(amount.multiply(percent).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP));
    }
}
