package com.drowningfish233.silentgearcatalog.client.gui.catalog;

import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Map;
import java.util.Set;

public final class CatalogApi {
    private CatalogApi() {}

    public interface EntryView {
        String getId();
        String getName();
        int getMaterialLevel();

        List<String> getTraitNames();
        default Map<String, Integer> getTraitLevels() { return Map.of(); }
        default Set<String> getTraitIds() { return getTraitLevels().keySet(); }
        List<String> getTraitDescriptions();

        Map<String, Double> getAttributeValues();
        Map<String, String> getAttributeTexts();
    }

    public interface MaterialView extends EntryView {
        default Set<String> getCategories() { return Set.of("unknown"); }
        int getDefaultSortOrder();
        int getAvailablePartCount();
        ItemStack getMaterialDisplayItem();
        int getColor();
        String getParentId();
        boolean isSimple();
        default Set<String> getSupportedPartTypes() { return Set.of(); }
        default List<PartData> getPartData() { return List.of(); }
    }

    public interface TraitView extends EntryView {
        int getMaxLevel();
        default List<CatalogEntry> getSourceMaterials() { return List.of(); }
        default List<ItemStack> getSourceItems() { return List.of(); }
    }

    @FunctionalInterface
    public interface EntryFilter {
        boolean test(EntryView view);
    }

    @FunctionalInterface
    public interface EntrySortValue {
        Object getValue(EntryView view);
    }
}