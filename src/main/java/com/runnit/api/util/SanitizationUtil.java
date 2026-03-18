package com.runnit.api.util;

import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;

public class SanitizationUtil {

    private static final Safelist NONE = Safelist.none();

    public static String sanitize(String input) {
        if (input == null) return null;
        return Jsoup.clean(input.trim(), NONE);
    }

    public static String sanitizeAndLimit(String input, int maxLength) {
        String sanitized = sanitize(input);
        if (sanitized == null) return null;
        return sanitized.length() > maxLength ? sanitized.substring(0, maxLength) : sanitized;
    }
}
