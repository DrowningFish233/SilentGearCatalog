package com.drowningfish233.silentgearcatalog.Utils;

import me.towdium.jecharacters.utils.Match;

public final class SearchUtils {
    private static final boolean HAS_JECH;

    static {
        boolean has = false;
        try {
            Class.forName("me.towdium.jecharacters.utils.Match");
            has = true;
        } catch (ClassNotFoundException ignored) {}
        HAS_JECH = has;
    }

    private SearchUtils() {}

    public static boolean matches(String text, String query) {
        if (query == null || query.isEmpty()) return true;
        if (text == null) return false;

        if (HAS_JECH) {
            return Match.contains(text, query, false);
        }
        return text.toLowerCase().contains(query.toLowerCase());
    }

    public static boolean hasPinyinSupport() {
        return HAS_JECH;
    }
}