package com.drowningfish233.silentgearcatalog.client.gui.catalog;

import com.drowningfish233.silentgearcatalog.client.gui.catalog.CatalogApi;
import com.drowningfish233.silentgearcatalog.client.gui.catalog.CatalogEntry;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

public record PartTypeFilter(
        String id,
        String displayName,
        String translationKey,
        Predicate<CatalogEntry> predicate
) {
    public static final Map<String, String> TRANSLATION_KEYS = new LinkedHashMap<>();

    static {
        TRANSLATION_KEYS.put("main", "part.silentgear.type.main");
        TRANSLATION_KEYS.put("rod", "part.silentgear.type.rod");
        TRANSLATION_KEYS.put("tip", "part.silentgear.type.tip");
        TRANSLATION_KEYS.put("cord", "part.silentgear.type.cord");
        TRANSLATION_KEYS.put("fletching", "part.silentgear.type.fletching");
        TRANSLATION_KEYS.put("binding", "part.silentgear.type.binding");
        TRANSLATION_KEYS.put("coating", "part.silentgear.type.coating");
        TRANSLATION_KEYS.put("grip", "part.silentgear.type.grip");
        TRANSLATION_KEYS.put("lining", "part.silentgear.type.lining");
        TRANSLATION_KEYS.put("setting", "part.silentgear.type.setting");
        TRANSLATION_KEYS.put("misc_upgrade", "part.silentgear.type.misc_upgrade");
        TRANSLATION_KEYS.put("none", "part.silentgear.type.none");
    }

    public static PartTypeFilter create(String id) {
        String translationKey = TRANSLATION_KEYS.getOrDefault(id, "part.silentgear.type." + id);
        String displayName = Component.translatable(translationKey).getString();
        return new PartTypeFilter(
                id,
                displayName,
                translationKey,
                e -> e instanceof CatalogApi.MaterialView &&
                        ((CatalogApi.MaterialView) e).getSupportedPartTypes().contains(id)
        );
    }

    public static List<PartTypeFilter> buildFromEntries(List<CatalogEntry> entries) {
        Map<String, PartTypeFilter> filters = new LinkedHashMap<>();

        for (CatalogEntry entry : entries) {
            if (entry instanceof CatalogApi.MaterialView materialView) {
                for (String partType : materialView.getSupportedPartTypes()) {
                    if (!filters.containsKey(partType) && TRANSLATION_KEYS.containsKey(partType)) {
                        filters.put(partType, create(partType));
                    }
                }
            }
        }

        return new ArrayList<>(filters.values());
    }

    public Component getDisplayComponent() {
        return Component.translatable(this.translationKey);
    }
}