package com.drowningfish233.silentgearcatalog.client.gui.catalog;

import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public record AttributeFilter(
        String id,
        String displayName,
        String attributeKey,
        String translationKey
) {
    public static final String FILTER_HAS_TRAITS = "_has_traits";
    public static final String FILTER_HAS_FLAT = "_has_flat";
    public static final String FILTER_HAS_MULTIPLIER = "_has_multiplier";
    public static final String FILTER_HAS_BOTH = "_has_both";

    public static final Map<String, String> TRANSLATION_KEYS = new LinkedHashMap<>();

    static {
        TRANSLATION_KEYS.put("silentgear:durability", "property.silentgear.durability");
        TRANSLATION_KEYS.put("silentgear:armor", "property.silentgear.armor");
        TRANSLATION_KEYS.put("silentgear:armor_toughness", "property.silentgear.armor_toughness");
        TRANSLATION_KEYS.put("silentgear:knockback_resistance", "property.silentgear.knockback_resistance");
        TRANSLATION_KEYS.put("silentgear:attack_damage", "property.silentgear.attack_damage");
        TRANSLATION_KEYS.put("silentgear:attack_speed", "property.silentgear.attack_speed");
        TRANSLATION_KEYS.put("silentgear:attack_reach", "property.silentgear.attack_reach");
        TRANSLATION_KEYS.put("silentgear:harvest_speed", "property.silentgear.harvest_speed");
        TRANSLATION_KEYS.put("silentgear:harvest_tier", "property.silentgear.harvest_tier");
        TRANSLATION_KEYS.put("silentgear:block_reach", "property.silentgear.block_reach");
        TRANSLATION_KEYS.put("silentgear:ranged_damage", "property.silentgear.ranged_damage");
        TRANSLATION_KEYS.put("silentgear:draw_speed", "property.silentgear.draw_speed");
        TRANSLATION_KEYS.put("silentgear:projectile_speed", "property.silentgear.projectile_speed");
        TRANSLATION_KEYS.put("silentgear:projectile_accuracy", "property.silentgear.projectile_accuracy");
        TRANSLATION_KEYS.put("silentgear:magic_damage", "property.silentgear.magic_damage");
        TRANSLATION_KEYS.put("silentgear:magic_armor", "property.silentgear.magic_armor");
        TRANSLATION_KEYS.put("silentgear:repair_efficiency", "property.silentgear.repair_efficiency");
        TRANSLATION_KEYS.put("silentgear:repair_value", "property.silentgear.repair_value");
        TRANSLATION_KEYS.put("silentgear:enchantment_value", "property.silentgear.enchantment_value");

        TRANSLATION_KEYS.put("silentgear:forge_positive_chance", "property.silentgear.forge_positive_chance");
        TRANSLATION_KEYS.put("silentgear:forge_power", "property.silentgear.forge_power");
        TRANSLATION_KEYS.put("silentgear:geas_limit", "property.silentgear.geas_limit");
        TRANSLATION_KEYS.put("silentgear:healing_received", "property.silentgear.healing_received");
        TRANSLATION_KEYS.put("silentgear:mana_regen", "property.silentgear.mana_regen");
        TRANSLATION_KEYS.put("silentgear:max_mana", "property.silentgear.max_mana");
        TRANSLATION_KEYS.put("silentgear:socket_slots", "property.silentgear.socket_slots");
        TRANSLATION_KEYS.put("silentgear:spell_power", "property.silentgear.spell_power");
        TRANSLATION_KEYS.put("silentgear:spell_resist", "property.silentgear.spell_resist");
        TRANSLATION_KEYS.put("silentgear:spell_slots", "property.silentgear.spell_slots");

        TRANSLATION_KEYS.put(FILTER_HAS_TRAITS, "filter.silentgearcatalog.has_traits");
        TRANSLATION_KEYS.put(FILTER_HAS_FLAT, "filter.silentgearcatalog.has_flat");
        TRANSLATION_KEYS.put(FILTER_HAS_MULTIPLIER, "filter.silentgearcatalog.has_multiplier");
        TRANSLATION_KEYS.put(FILTER_HAS_BOTH, "filter.silentgearcatalog.has_both");
    }

    public static AttributeFilter create(String id, String attributeKey) {
        String keyForTranslation = attributeKey;
        if (keyForTranslation != null && keyForTranslation.startsWith("silentgear:")) {
            keyForTranslation = keyForTranslation.substring("silentgear:".length());
        }
        String translationKey = TRANSLATION_KEYS.getOrDefault(attributeKey, "property.silentgear." + keyForTranslation);
        String displayName = Component.translatable(translationKey).getString();
        return new AttributeFilter(id, displayName, attributeKey, translationKey);
    }

    public static AttributeFilter createSpecial(String id) {
        String translationKey = TRANSLATION_KEYS.getOrDefault(id, id);
        String displayName = Component.translatable(translationKey).getString();
        return new AttributeFilter(id, displayName, null, translationKey);
    }

    public static List<AttributeFilter> buildFromEntries(List<CatalogEntry> entries) {
        Map<String, AttributeFilter> filters = new LinkedHashMap<>();

        filters.put(FILTER_HAS_TRAITS, createSpecial(FILTER_HAS_TRAITS));
        filters.put(FILTER_HAS_FLAT, createSpecial(FILTER_HAS_FLAT));
        filters.put(FILTER_HAS_MULTIPLIER, createSpecial(FILTER_HAS_MULTIPLIER));
        filters.put(FILTER_HAS_BOTH, createSpecial(FILTER_HAS_BOTH));

        for (CatalogEntry entry : entries) {
            for (String key : entry.getAttributeValues().keySet()) {
                if (!filters.containsKey(key)) {
                    filters.put(key, create(key, key));
                }
            }
        }

        return new ArrayList<>(filters.values());
    }

    public boolean test(CatalogEntry entry, Set<String> activePartFilters) {
        if (FILTER_HAS_TRAITS.equals(id)) {
            return !entry.getTraitNames().isEmpty();
        }

        if (FILTER_HAS_FLAT.equals(id) || FILTER_HAS_MULTIPLIER.equals(id) || FILTER_HAS_BOTH.equals(id)) {
            return testTypeFilter(entry, activePartFilters);
        }

        if (attributeKey == null) {
            return true;
        }

        if (activePartFilters == null || activePartFilters.isEmpty()) {
            PartData.AttributeValue v = entry.getAttributeValues().get(attributeKey);
            return v != null && !v.isEmpty();
        }

        for (PartData partData : entry.getPartData()) {
            if (activePartFilters.contains(partData.partType())) {
                PartData.AttributeValue v = partData.attributeValues().get(attributeKey);
                if (v != null && !v.isEmpty()) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean testTypeFilter(CatalogEntry entry, Set<String> activePartFilters) {
        for (PartData partData : entry.getPartData()) {
            if (activePartFilters != null && !activePartFilters.isEmpty()
                    && !activePartFilters.contains(partData.partType())) {
                continue;
            }
            for (PartData.AttributeValue v : partData.attributeValues().values()) {
                boolean hasFlat = v.flat() != 0;
                boolean hasMul = v.multiplier() != 1.0;
                if (FILTER_HAS_FLAT.equals(id) && hasFlat && !hasMul) return true;
                if (FILTER_HAS_MULTIPLIER.equals(id) && hasMul && !hasFlat) return true;
                if (FILTER_HAS_BOTH.equals(id) && hasFlat && hasMul) return true;
            }
        }
        return false;
    }

    public Component getDisplayComponent() {
        return Component.translatable(this.translationKey);
    }
}