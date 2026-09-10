package com.drowningfish233.silentgearcatalog.client.gui.catalog;

import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Set;

public interface CatalogEntry extends CatalogApi.EntryView {
    ItemStack getDisplayStack();
    String getSearchText();
    List<TraitTooltip> getTraitTooltips();

    default Set<String> getSupportedPartTypes() {
        return Set.of();
    }

    default List<PartData> getPartData() {
        return List.of();
    }

    default List<TraitData> getAllTraits() {
        return List.of();
    }

    record TraitTooltip(String name, List<String> descriptions) {
        public TraitTooltip(String name, List<String> descriptions) {
            this.name = name;
            this.descriptions = List.copyOf(descriptions);
        }
    }

    record TraitData(String id, String name, int level, List<String> descriptions) {}
}