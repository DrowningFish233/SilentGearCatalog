package com.drowningfish233.silentgearcatalog.client.gui.catalog;

import java.util.List;
import java.util.Map;

public record PartData(
        String partType,
        String partTypeDisplayName,
        Map<String, AttributeValue> attributeValues,
        List<TraitData> traits
) {
    public record TraitData(String id, String name, int level, List<String> descriptions) {}

    public record AttributeValue(
            double average,
            double add,
            double multiplyBase,
            double multiplyTotal,
            double max,
            String displayText
    ) {
        public static final AttributeValue EMPTY =
                new AttributeValue(0, 0, 0, 0, 0, "");

        public boolean isEmpty() {
            return average == 0 && add == 0 && multiplyBase == 0
                    && multiplyTotal == 0 && max == 0;
        }

        public double flat() {
            return average + add + max;
        }

        public double multiplier() {
            return (1 + multiplyBase) * (1 + multiplyTotal);
        }

        public double effective() {
            return flat() * multiplier();
        }
    }
}