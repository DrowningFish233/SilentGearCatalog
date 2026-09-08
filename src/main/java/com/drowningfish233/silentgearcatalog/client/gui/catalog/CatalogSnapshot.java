package com.drowningfish233.silentgearcatalog.client.gui.catalog;

import java.util.List;

public record CatalogSnapshot(
        boolean fullyLoaded,
        List<CatalogEntry> materials,
        List<CatalogEntry> traits
) {
    public static CatalogSnapshot loading() {
        return new CatalogSnapshot(false, List.of(), List.of());
    }

    public boolean isEmpty() {
        return materials.isEmpty() && traits.isEmpty();
    }
}