package com.drowningfish233.silentgearcatalog.client.gui.catalog;

import net.minecraft.network.chat.Component;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public final class CatalogAttributes {
    private CatalogAttributes() {}

    public static Component title(String id) {
        return Component.translatable("sort.silentgearcatalog." + id);
    }

    public static String format(String id, double value) {
        if (id.endsWith("_durability") || id.endsWith("_level")) {
            return Integer.toString((int) Math.round(value));
        }
        if (value == (long) value) {
            return Long.toString((long) value);
        }
        return String.format(Locale.ROOT, "%.2f", value);
    }

    public static Map<String, Double> collect(Map<String, Double> source) {
        return new LinkedHashMap<>(source);
    }

    public static Map<String, String> collectTexts(Map<String, Double> source) {
        Map<String, String> texts = new LinkedHashMap<>();
        for (Map.Entry<String, Double> entry : source.entrySet()) {
            texts.put(entry.getKey(), format(entry.getKey(), entry.getValue()));
        }
        return texts;
    }
}