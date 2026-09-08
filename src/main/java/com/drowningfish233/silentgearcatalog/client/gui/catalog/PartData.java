package com.drowningfish233.silentgearcatalog.client.gui.catalog;

import java.util.List;
import java.util.Map;

public record PartData(
        String partType,
        String partTypeDisplayName,
        Map<String, Double> attributeValues,
        Map<String, String> attributeTexts,
        List<TraitData> traits
) {
    public record TraitData(String id, String name, int level, List<String> descriptions) {}
}