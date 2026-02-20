package com.keza.common.util;

import java.util.regex.Pattern;

public final class PhoneUtil {

    private static final Pattern KENYA_PHONE_PATTERN = Pattern.compile("^\\+254[17]\\d{8}$");
    private static final Pattern LOCAL_PHONE_PATTERN = Pattern.compile("^0[17]\\d{8}$");

    private PhoneUtil() {}

    public static boolean isValidKenyanPhone(String phone) {
        if (phone == null || phone.isBlank()) return false;
        return KENYA_PHONE_PATTERN.matcher(phone).matches()
                || LOCAL_PHONE_PATTERN.matcher(phone).matches();
    }

    public static String normalizeToInternational(String phone) {
        if (phone == null || phone.isBlank()) return null;
        phone = phone.trim().replaceAll("\\s+", "");
        if (phone.startsWith("0")) {
            return "+254" + phone.substring(1);
        }
        if (phone.startsWith("254") && !phone.startsWith("+")) {
            return "+" + phone;
        }
        return phone;
    }

    public static String formatForMpesa(String phone) {
        String normalized = normalizeToInternational(phone);
        if (normalized == null) return null;
        return normalized.replace("+", "");
    }
}
