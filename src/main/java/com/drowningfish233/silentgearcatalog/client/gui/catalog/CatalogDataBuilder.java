package com.drowningfish233.silentgearcatalog.client.gui.catalog;

import com.drowningfish233.silentgearcatalog.Utils.SilentGearUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.silentchaos512.gear.api.item.GearType;
import net.silentchaos512.gear.api.material.IMaterialCategory;
import net.silentchaos512.gear.api.material.Material;
import net.silentchaos512.gear.api.part.PartType;
import net.silentchaos512.gear.api.property.GearProperty;
import net.silentchaos512.gear.api.traits.TraitInstance;
import net.silentchaos512.gear.api.util.PropertyKey;
import net.silentchaos512.gear.gear.material.MaterialInstance;
import net.silentchaos512.gear.gear.trait.Trait;
import net.silentchaos512.gear.setup.SgRegistries;
import net.silentchaos512.gear.setup.gear.GearProperties;
import net.silentchaos512.gear.setup.gear.GearTypes;
import net.silentchaos512.gear.setup.gear.PartTypes;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public final class CatalogDataBuilder {
    private static final PartType DEFAULT_PART = PartTypes.MAIN.get();
    private static final GearType DEFAULT_GEAR = GearTypes.ALL.get();

    private static CatalogSnapshot cachedSnapshot = null;
    private static boolean isBuilding = false;

    private static List<GearType> ALL_GEAR_TYPES = null;
    private static List<PartType> ALL_PART_TYPES = null;
    private static List<GearProperty<?, ?>> ALL_PROPERTIES = null;

    private CatalogDataBuilder() {}

    public static CatalogSnapshot build() {
        if (cachedSnapshot != null) {
            return cachedSnapshot;
        }

        if (isBuilding) {
            return CatalogSnapshot.loading();
        }

        synchronized (CatalogDataBuilder.class) {
            if (cachedSnapshot != null) {
                return cachedSnapshot;
            }
            if (isBuilding) {
                return CatalogSnapshot.loading();
            }
            isBuilding = true;
            try {
                initCaches();

                List<CatalogEntry> materials = buildMaterials();
                List<CatalogEntry> traits = buildTraits();

                Map<String, List<CatalogEntry>> materialSources = new HashMap<>();
                for (CatalogEntry entry : materials) {
                    for (String traitId : entry.getTraitIds()) {
                        materialSources.computeIfAbsent(traitId, k -> new ArrayList<>()).add(entry);
                    }
                }

                for (CatalogEntry entry : traits) {
                    if (entry instanceof TraitCatalogEntry traitEntry) {
                        traitEntry.sourceMaterials = List.copyOf(
                                materialSources.getOrDefault(entry.getId(), List.of())
                        );
                    }
                }

                cachedSnapshot = new CatalogSnapshot(true, materials, traits);
                return cachedSnapshot;
            } catch (Exception e) {
                return CatalogSnapshot.loading();
            } finally {
                isBuilding = false;
            }
        }
    }


    public static void invalidateCache() {
        cachedSnapshot = null;
    }

    private static void initCaches() {
        if (ALL_GEAR_TYPES == null) {
            ALL_GEAR_TYPES = new ArrayList<>();
            for (GearType gearType : SgRegistries.GEAR_TYPE) {
                ALL_GEAR_TYPES.add(gearType);
            }
        }

        if (ALL_PART_TYPES == null) {
            ALL_PART_TYPES = new ArrayList<>();
            for (PartType partType : SgRegistries.PART_TYPE) {
                ALL_PART_TYPES.add(partType);
            }
        }

        if (ALL_PROPERTIES == null) {
            ALL_PROPERTIES = new ArrayList<>();
            for (GearProperty<?, ?> property : SgRegistries.GEAR_PROPERTY) {
                ALL_PROPERTIES.add(property);
            }
        }
    }

    private static List<CatalogEntry> buildMaterials() {
        List<CatalogEntry> entries = new ArrayList<>();

        List<Material> allMaterials = SgRegistries.MATERIAL.getValues(true);

        for (Material material : allMaterials) {
            try {
                ResourceLocation id = SilentGearUtils.getMaterialId(material);
                if (id == null) continue;
                String idStr = id.toString();

                Component nameComp = SilentGearUtils.getMaterialDisplayName(material, DEFAULT_PART);
                String nameStr = nameComp.getString();

                MaterialInstance instance = MaterialInstance.of(material);
                int color = instance.getColor(DEFAULT_GEAR, DEFAULT_PART);

                ItemStack displayStack = getMaterialDisplayStack(material);

                Map<String, List<PartData.TraitData>> traitsByPart = collectTraitsByPart(material);

                List<PartData.TraitData> allTraits = getTraitData(traitsByPart);

                Map<String, Map<String, Double>> attributesByPart = collectAttributesByPart(material);
                Map<String, Map<String, String>> attributeTextsByPart = collectAttributeTextsByPart(material);

                List<PartData> partDataList = new ArrayList<>();

                for (PartType partType : SilentGearUtils.getAllowedPartTypes(material)) {
                    ResourceLocation partId = SgRegistries.PART_TYPE.getKey(partType);
                    if (partId == null) continue;
                    String partTypeStr = partId.getPath();

                    String partDisplayName = Component.translatable("part.silentgear.type." + partTypeStr).getString();

                    Map<String, Double> attrs = attributesByPart.getOrDefault(partTypeStr, Map.of());
                    Map<String, String> attrTexts = attributeTextsByPart.getOrDefault(partTypeStr, Map.of());
                    List<PartData.TraitData> traits = traitsByPart.getOrDefault(partTypeStr, List.of());

                    partDataList.add(new PartData(
                            partTypeStr,
                            partDisplayName,
                            attrs,
                            attrTexts,
                            traits
                    ));
                }

                Set<String> categories = getMaterialCategories(material);
                int availableParts = SilentGearUtils.getAllowedPartTypes(material).size();

                Set<String> supportedPartTypes = new HashSet<>();
                for (PartType partType : SilentGearUtils.getAllowedPartTypes(material)) {
                    ResourceLocation partId = SgRegistries.PART_TYPE.getKey(partType);
                    if (partId != null) {
                        supportedPartTypes.add(partId.getPath());
                    }
                }

                Material parent = SilentGearUtils.getMaterialParent(material);
                String parentId = parent != null ?
                        SilentGearUtils.getMaterialId(parent).toString() : null;

                entries.add(new MaterialCatalogEntry(
                        idStr,
                        displayStack,
                        nameStr,
                        0,
                        partDataList,
                        allTraits,
                        attributesByPart,
                        attributeTextsByPart,
                        0,
                        availableParts,
                        categories,
                        color,
                        parentId,
                        material.isSimple(),
                        supportedPartTypes
                ));

            } catch (Exception ignored) {}
        }

        entries.sort(Comparator.comparing(CatalogEntry::getName));
        return List.copyOf(entries);
    }

    private static @NotNull List<PartData.TraitData> getTraitData(Map<String, List<PartData.TraitData>> traitsByPart) {
        Map<String, PartData.TraitData> allTraitMap = new LinkedHashMap<>();
        for (List<PartData.TraitData> list : traitsByPart.values()) {
            for (PartData.TraitData trait : list) {
                if (!allTraitMap.containsKey(trait.id())) {
                    allTraitMap.put(trait.id(), trait);
                } else {
                    PartData.TraitData existing = allTraitMap.get(trait.id());
                    if (trait.level() > existing.level()) {
                        allTraitMap.put(trait.id(), trait);
                    }
                }
            }
        }
        List<PartData.TraitData> allTraits = new ArrayList<>(allTraitMap.values());
        return allTraits;
    }

    private static List<CatalogEntry> buildTraits() {
        List<CatalogEntry> entries = new ArrayList<>();

        for (Trait trait : SilentGearUtils.getAllTraitsSorted()) {
            try {
                ResourceLocation id = SilentGearUtils.getTraitId(trait);
                if (id == null) continue;
                String idStr = id.toString();

                Component nameComp = SilentGearUtils.getTraitDisplayName(trait);
                String nameStr = nameComp.getString();

                Component descComp = SilentGearUtils.getTraitDescription(trait);
                String descStr = descComp.getString();

                int maxLevel = trait.getMaxLevel();

                ItemStack displayStack = new ItemStack(net.minecraft.world.item.Items.ENCHANTED_BOOK);

                entries.add(new TraitCatalogEntry(
                        idStr,
                        displayStack,
                        nameStr,
                        maxLevel,
                        List.of(descStr),
                        maxLevel
                ));

            } catch (Exception ignored) {}
        }

        entries.sort(Comparator.comparing(CatalogEntry::getName));
        return List.copyOf(entries);
    }

    private static Set<String> getMaterialCategories(Material material) {
        try {
            MaterialInstance instance = MaterialInstance.of(material);
            Collection<IMaterialCategory> categories = instance.getCategories();
            Set<String> result = new HashSet<>();
            for (IMaterialCategory cat : categories) {
                result.add(cat.getDisplayName().getString());
            }
            return result;
        } catch (Exception e) {
            return Set.of("unknown");
        }
    }

    private static ItemStack getMaterialDisplayStack(Material material) {
        try {
            ItemStack stack = SilentGearUtils.getMaterialDisplayStack(material);
            if (stack != null && !stack.isEmpty()) {
                return stack;
            }

            MaterialInstance instance = MaterialInstance.of(material);
            Ingredient ingredient = instance.getIngredient();
            {
                ItemStack[] stacks = ingredient.getItems();
                if (stacks.length > 0 && stacks[0] != null && !stacks[0].isEmpty()) {
                    return stacks[0];
                }
            }

            for (PartType partType : SilentGearUtils.getAllowedPartTypes(material)) {
                Optional<Ingredient> substitute = material.getPartSubstitute(partType);
                if (substitute.isPresent()) {
                    Ingredient ing = substitute.get();
                    ItemStack[] stacks = ing.getItems();
                    if (stacks.length > 0 && stacks[0] != null && !stacks[0].isEmpty()) {
                        return stacks[0];
                    }
                }
            }
        } catch (Exception ignored) {}
        return ItemStack.EMPTY;
    }

    private static Map<String, List<PartData.TraitData>> collectTraitsByPart(Material material) {
        Map<String, List<PartData.TraitData>> result = new LinkedHashMap<>();

        try {
            MaterialInstance instance = MaterialInstance.of(material);
            var traitProperty = GearProperties.TRAITS.get();

            for (PartType partType : ALL_PART_TYPES) {
                ResourceLocation partId = SgRegistries.PART_TYPE.getKey(partType);
                if (partId == null) continue;
                String partTypeStr = partId.getPath();

                Map<String, PartData.TraitData> traitMap = new LinkedHashMap<>();

                for (GearType gearType : ALL_GEAR_TYPES) {
                    try {
                        var mods = instance.getPropertyModifiers(
                                partType,
                                PropertyKey.of(traitProperty, gearType)
                        );

                        for (var mod : mods) {
                            for (TraitInstance traitInstance : mod.value()) {
                                if (traitInstance.isValid()) {
                                    ResourceLocation traitId = traitInstance.getTraitId();
                                    Trait trait = SgRegistries.TRAIT.get(traitId);
                                    if (trait != null) {
                                        String id = traitId.toString();
                                        if (!traitMap.containsKey(id)) {
                                            String name = trait.getDisplayName(1).getString();
                                            String desc = trait.getDescription(1).getString();
                                            traitMap.put(id, new PartData.TraitData(
                                                    id,
                                                    name,
                                                    traitInstance.getLevel(),
                                                    List.of(desc)
                                            ));
                                        } else {
                                            PartData.TraitData existing = traitMap.get(id);
                                            if (traitInstance.getLevel() > existing.level()) {
                                                traitMap.put(id, new PartData.TraitData(
                                                        existing.id(),
                                                        existing.name(),
                                                        traitInstance.getLevel(),
                                                        existing.descriptions()
                                                ));
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } catch (Exception ignored) {}
                }

                if (!traitMap.isEmpty()) {
                    result.put(partTypeStr, new ArrayList<>(traitMap.values()));
                }
            }
        } catch (Exception ignored) {}

        return result;
    }

    private static Map<String, Map<String, Double>> collectAttributesByPart(Material material) {
        Map<String, Map<String, Double>> result = new LinkedHashMap<>();

        try {
            for (PartType partType : SilentGearUtils.getAllowedPartTypes(material)) {
                ResourceLocation partId = SgRegistries.PART_TYPE.getKey(partType);
                if (partId == null) continue;
                String partTypeStr = partId.getPath();

                Map<String, Double> attributes = new LinkedHashMap<>();

                for (GearProperty<?, ?> property : ALL_PROPERTIES) {
                    try {
                        String valueStr = SilentGearUtils.getMaterialPropertyValue(
                                material, partType, property
                        );

                        if (valueStr != null && !valueStr.isEmpty() &&
                                !valueStr.equals("0") && !valueStr.equals("0.0") &&
                                !valueStr.equals("{}")) {

                            ResourceLocation propKey = SgRegistries.GEAR_PROPERTY.getKey(property);
                            if (propKey == null) continue;

                            String key = propKey.getPath();

                            if (key.equals("harvest_tier")) {
                                String tierValue = parseHarvestTierValue(valueStr);
                                try {
                                    double val = Double.parseDouble(tierValue);
                                    if (val > 0) {
                                        attributes.put("harvest_tier", val);
                                    }
                                } catch (NumberFormatException ignored) {}
                            } else {
                                try {
                                    double val = Double.parseDouble(valueStr);
                                    if (val != 0) {
                                        attributes.put(key, val);
                                    }
                                } catch (NumberFormatException ignored) {}
                            }
                        }
                    } catch (Exception ignored) {}
                }

                if (!attributes.isEmpty()) {
                    result.put(partTypeStr, attributes);
                }
            }
        } catch (Exception ignored) {}

        return result;
    }

    private static Map<String, Map<String, String>> collectAttributeTextsByPart(Material material) {
        Map<String, Map<String, String>> result = new LinkedHashMap<>();
        Map<String, Map<String, Double>> attrsByPart = collectAttributesByPart(material);

        for (Map.Entry<String, Map<String, Double>> entry : attrsByPart.entrySet()) {
            String partType = entry.getKey();
            Map<String, Double> attrs = entry.getValue();
            Map<String, String> texts = new LinkedHashMap<>();
            for (Map.Entry<String, Double> attr : attrs.entrySet()) {
                texts.put(attr.getKey(), formatAttribute(attr.getKey(), attr.getValue()));
            }
            result.put(partType, texts);
        }

        return result;
    }

    private static String parseHarvestTierValue(String rawValue) {
        if (rawValue == null || rawValue.isEmpty() || rawValue.equals("{}")) {
            return "0";
        }
        try {
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("level_hint\"\\s*:\\s*\"(\\d+)\"");
            java.util.regex.Matcher matcher = pattern.matcher(rawValue);
            if (matcher.find()) {
                return matcher.group(1);
            }
            pattern = java.util.regex.Pattern.compile("(\\d+)");
            matcher = pattern.matcher(rawValue);
            if (matcher.find()) {
                return matcher.group(1);
            }
        } catch (Exception ignored) {}
        return "0";
    }

    private static String formatAttribute(String key, double value) {
        if (key.endsWith("durability") || key.endsWith("_level") || key.endsWith("_tier")) {
            return Integer.toString((int) Math.round(value));
        }
        if (key.contains("speed") || key.contains("damage") || key.contains("armor")) {
            return String.format(Locale.ROOT, "%.1f", value);
        }
        if (value == (long) value) {
            return Long.toString((long) value);
        }
        return String.format(Locale.ROOT, "%.2f", value);
    }


    private static final class MaterialCatalogEntry implements CatalogEntry, CatalogApi.MaterialView {
        private final String id;
        private final String name;
        private final int tier;
        private final List<PartData> partDataList;
        private final List<PartData.TraitData> allTraits;
        private final Map<String, Map<String, Double>> attributesByPart;
        private final Map<String, Map<String, String>> attributeTextsByPart;
        private final ItemStack displayStack;
        private final String searchText;
        private final int sortOrder;
        private final int availableParts;
        private final Set<String> categories;
        private final int color;
        private final String parentId;
        private final boolean simple;
        private final Set<String> supportedPartTypes;

        private final List<String> traitNames;
        private final List<String> traitDescriptions;
        private final List<TraitTooltip> traitTooltips;
        private final Map<String, Integer> traitLevels;
        private final Set<String> traitIds;

        MaterialCatalogEntry(String id, ItemStack displayStack, String name, int tier,
                             List<PartData> partDataList, List<PartData.TraitData> allTraits,
                             Map<String, Map<String, Double>> attributesByPart,
                             Map<String, Map<String, String>> attributeTextsByPart,
                             int sortOrder, int availableParts, Set<String> categories,
                             int color, String parentId, boolean simple, Set<String> supportedPartTypes) {
            this.id = id;
            this.name = name;
            this.tier = tier;
            this.partDataList = List.copyOf(partDataList);
            this.allTraits = List.copyOf(allTraits);
            this.attributesByPart = attributesByPart;
            this.attributeTextsByPart = attributeTextsByPart;
            this.displayStack = displayStack.copy();
            this.sortOrder = sortOrder;
            this.availableParts = availableParts;
            this.categories = Set.copyOf(categories);
            this.color = color;
            this.parentId = parentId;
            this.simple = simple;
            this.supportedPartTypes = Set.copyOf(supportedPartTypes);

            Map<String, Integer> levels = new LinkedHashMap<>();
            Set<String> ids = new LinkedHashSet<>();
            for (PartData.TraitData trait : allTraits) {
                levels.put(trait.id(), trait.level());
                ids.add(trait.id());
            }
            this.traitLevels = Collections.unmodifiableMap(levels);
            this.traitIds = Set.copyOf(ids);
            this.traitTooltips = allTraits.stream()
                    .map(t -> new TraitTooltip(t.name(), t.descriptions()))
                    .toList();
            this.traitNames = this.traitTooltips.stream()
                    .map(TraitTooltip::name)
                    .toList();
            this.traitDescriptions = this.traitTooltips.stream()
                    .flatMap(t -> t.descriptions().stream())
                    .toList();

            List<String> searchParts = new ArrayList<>();
            searchParts.add(id);
            searchParts.add(name);
            searchParts.addAll(this.traitNames);
            searchParts.addAll(this.traitDescriptions);
            searchParts.addAll(categories);
            for (PartData partData : partDataList) {
                searchParts.add(partData.partTypeDisplayName());
            }
            this.searchText = String.join("\n", searchParts).toLowerCase(Locale.ROOT);
        }

        @Override
        public Map<String, Double> getAttributeValues() {
            Map<String, Double> merged = new LinkedHashMap<>();
            for (PartData partData : partDataList) {
                merged.putAll(partData.attributeValues());
            }
            return merged;
        }

        @Override
        public Map<String, String> getAttributeTexts() {
            Map<String, String> merged = new LinkedHashMap<>();
            for (PartData partData : partDataList) {
                merged.putAll(partData.attributeTexts());
            }
            return merged;
        }

        @Override
        public String getId() { return id; }

        @Override
        public String getName() { return name; }

        @Override
        public int getMaterialLevel() { return tier; }

        @Override
        public List<String> getTraitNames() { return traitNames; }

        @Override
        public Map<String, Integer> getTraitLevels() { return traitLevels; }

        @Override
        public Set<String> getTraitIds() { return traitIds; }

        @Override
        public List<String> getTraitDescriptions() { return traitDescriptions; }

        @Override
        public ItemStack getDisplayStack() { return displayStack.copy(); }

        @Override
        public String getSearchText() { return searchText; }

        @Override
        public List<TraitTooltip> getTraitTooltips() { return traitTooltips; }

        @Override
        public Set<String> getCategories() { return categories; }

        @Override
        public int getDefaultSortOrder() { return sortOrder; }

        @Override
        public int getAvailablePartCount() { return availableParts; }

        @Override
        public ItemStack getMaterialDisplayItem() { return displayStack.copy(); }

        @Override
        public int getColor() { return color; }

        @Override
        public String getParentId() { return parentId; }

        @Override
        public boolean isSimple() { return simple; }

        @Override
        public Set<String> getSupportedPartTypes() { return supportedPartTypes; }

        @Override
        public List<PartData> getPartData() { return partDataList; }
    }

    static final class TraitCatalogEntry implements CatalogEntry, CatalogApi.TraitView {
        private final String id;
        private final String name;
        private final int level;
        private final List<String> descriptions;
        private final int maxLevel;
        private final ItemStack displayStack;
        private final String searchText;
        private List<CatalogEntry> sourceMaterials = List.of();

        private final List<TraitTooltip> traitTooltips;
        private final Map<String, Integer> traitLevels;
        private final Set<String> traitIds;

        TraitCatalogEntry(String id, ItemStack displayStack, String name, int level,
                          List<String> descriptions, int maxLevel) {
            this.id = id;
            this.name = name;
            this.level = level;
            this.descriptions = List.copyOf(descriptions);
            this.maxLevel = maxLevel;
            this.displayStack = displayStack.copy();

            this.traitLevels = Map.of(id, level);
            this.traitIds = Set.of(id);
            this.traitTooltips = List.of(new TraitTooltip(name, descriptions));

            List<String> searchParts = new ArrayList<>();
            searchParts.add(id);
            searchParts.add(name);
            searchParts.addAll(descriptions);
            this.searchText = String.join("\n", searchParts).toLowerCase(Locale.ROOT);
        }

        @Override public String getId() { return id; }
        @Override public String getName() { return name; }
        @Override public int getMaterialLevel() { return level; }
        @Override public List<String> getTraitNames() { return List.of(name); }
        @Override public Map<String, Integer> getTraitLevels() { return traitLevels; }
        @Override public Set<String> getTraitIds() { return traitIds; }
        @Override public List<String> getTraitDescriptions() { return descriptions; }
        @Override public Map<String, Double> getAttributeValues() { return Map.of(); }
        @Override public Map<String, String> getAttributeTexts() { return Map.of(); }
        @Override public ItemStack getDisplayStack() { return displayStack.copy(); }
        @Override public String getSearchText() { return searchText; }
        @Override public List<TraitTooltip> getTraitTooltips() { return traitTooltips; }
        @Override public int getMaxLevel() { return maxLevel; }
        @Override public List<CatalogEntry> getSourceMaterials() { return sourceMaterials; }
        @Override public List<ItemStack> getSourceItems() { return List.of(); }
    }
}