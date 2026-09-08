package com.drowningfish233.silentgearcatalog.client.gui.catalog;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record AttributeSort(
        String id,
        String displayName,
        String attributeKey,
        String translationKey
) {
    // 预定义的翻译key
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

        // 自定义属性（使用 silentgear 命名空间，因为 RFGearProperties 注册在 silentgear 下）
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
    }

    public static AttributeSort create(String id, String attributeKey) {
        String keyForTranslation = attributeKey;
        if (keyForTranslation.startsWith("silentgear:")) {
            keyForTranslation = keyForTranslation.substring("silentgear:".length());
        }
        String translationKey = TRANSLATION_KEYS.getOrDefault(attributeKey, "property.silentgear." + keyForTranslation);
        return new AttributeSort(id, Component.translatable(translationKey).getString(), attributeKey, translationKey);
    }

    public static AttributeSort createNameSort() {
        return new AttributeSort("_name", Component.translatable("sort.silentgearcatalog.name").getString(), null, "sort.silentgearcatalog.name");
    }

    public static Map<String, AttributeSort> buildFromEntries(List<CatalogEntry> entries) {
        Map<String, AttributeSort> sorts = new LinkedHashMap<>();

        // 名称排序
        sorts.put("_name", createNameSort());

        // 收集所有材料中出现的属性键
        for (CatalogEntry entry : entries) {
            for (String key : entry.getAttributeValues().keySet()) {
                if (!sorts.containsKey(key)) {
                    sorts.put(key, create(key, key));
                }
            }
        }

        return sorts;
    }

    public Component getDisplayComponent() {
        return Component.translatable(this.translationKey);
    }

    public Comparator<CatalogEntry> getComparator(boolean descending) {
        if ("_name".equals(id) || attributeKey == null) {
            Comparator<CatalogEntry> comparator = Comparator.comparing(
                    CatalogEntry::getName, String.CASE_INSENSITIVE_ORDER
            );
            return descending ? comparator.reversed() : comparator;
        }

        Comparator<CatalogEntry> comparator = (a, b) -> {
            double valA = a.getAttributeValues().getOrDefault(attributeKey, Double.NEGATIVE_INFINITY);
            double valB = b.getAttributeValues().getOrDefault(attributeKey, Double.NEGATIVE_INFINITY);
            return Double.compare(valA, valB);
        };

        return descending ? comparator.reversed() : comparator;
    }
}