package com.drowningfish233.silentgearcatalog.client.gui.catalog;

import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

public record AttributeFilter(
        String id,
        String displayName,
        String attributeKey,
        String translationKey,
        Predicate<CatalogEntry> predicate
) {
    public static final Map<String, String> TRANSLATION_KEYS = new LinkedHashMap<>();

    static {
        // 基础属性
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

        // 自定义属性
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

        // 特性特殊
        TRANSLATION_KEYS.put("_has_traits", "filter.silentgearcatalog.has_traits");
    }

    public static AttributeFilter create(String id, String attributeKey, Predicate<CatalogEntry> predicate) {
        String keyForTranslation = attributeKey;
        if (keyForTranslation != null && keyForTranslation.startsWith("silentgear:")) {
            keyForTranslation = keyForTranslation.substring("silentgear:".length());
        }
        String translationKey = TRANSLATION_KEYS.getOrDefault(attributeKey, "property.silentgear." + keyForTranslation);
        String displayName = Component.translatable(translationKey).getString();
        return new AttributeFilter(id, displayName, attributeKey, translationKey, predicate);
    }

    public static AttributeFilter createTraitFilter() {
        String key = "_has_traits";
        String translationKey = TRANSLATION_KEYS.getOrDefault(key, "filter.silentgearcatalog.has_traits");
        String displayName = Component.translatable(translationKey).getString();
        return new AttributeFilter(key, displayName, null, translationKey, e -> !e.getTraitNames().isEmpty());
    }

    public static List<AttributeFilter> buildFromEntries(List<CatalogEntry> entries) {
        Map<String, AttributeFilter> filters = new LinkedHashMap<>();

        // 特性过滤器
        filters.put("_has_traits", createTraitFilter());

        // 从数据中收集属性
        for (CatalogEntry entry : entries) {
            for (String key : entry.getAttributeValues().keySet()) {
                if (!filters.containsKey(key)) {
                    Predicate<CatalogEntry> predicate = e -> e.getAttributeValues().getOrDefault(key, 0.0) > 0.0001;
                    filters.put(key, create(key, key, predicate));
                }
            }
        }

        return new ArrayList<>(filters.values());
    }

    public Component getDisplayComponent() {
        return Component.translatable(this.translationKey);
    }
}