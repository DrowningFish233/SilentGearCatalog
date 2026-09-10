package com.drowningfish233.silentgearcatalog.client.gui.catalog;

import com.drowningfish233.silentgearcatalog.Utils.SilentGearUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.silentchaos512.gear.api.item.GearType;
import net.silentchaos512.gear.api.material.IMaterialCategory;
import net.silentchaos512.gear.api.material.Material;
import net.silentchaos512.gear.api.part.PartType;
import net.silentchaos512.gear.api.property.GearProperty;
import net.silentchaos512.gear.api.property.NumberProperty;
import net.silentchaos512.gear.api.property.NumberPropertyValue;
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

                Map<String, Map<String, PartData.AttributeValue>> attributesByPart = collectAttributesByPart(material);

                List<PartData> partDataList = new ArrayList<>();

                for (PartType partType : SilentGearUtils.getAllowedPartTypes(material)) {
                    ResourceLocation partId = SgRegistries.PART_TYPE.getKey(partType);
                    if (partId == null) continue;
                    String partTypeStr = partId.getPath();

                    String partDisplayName = Component.translatable("part.silentgear.type." + partTypeStr).getString();

                    Map<String, PartData.AttributeValue> attrs = attributesByPart.getOrDefault(partTypeStr, Map.of());
                    List<PartData.TraitData> traits = traitsByPart.getOrDefault(partTypeStr, List.of());

                    partDataList.add(new PartData(
                            partTypeStr,
                            partDisplayName,
                            attrs,
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
        return new ArrayList<>(allTraitMap.values());
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

                ItemStack displayStack = new ItemStack(Items.ENCHANTED_BOOK);

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

    private static Map<String, Map<String, PartData.AttributeValue>> collectAttributesByPart(Material material) {
        Map<String, Map<String, PartData.AttributeValue>> result = new LinkedHashMap<>();

        try {
            MaterialInstance instance = MaterialInstance.of(material);

            for (PartType partType : SilentGearUtils.getAllowedPartTypes(material)) {
                ResourceLocation partId = SgRegistries.PART_TYPE.getKey(partType);
                if (partId == null) continue;
                String partTypeStr = partId.getPath();

                Map<String, PartData.AttributeValue> attributes = new LinkedHashMap<>();

                for (GearProperty<?, ?> property : ALL_PROPERTIES) {
                    if (!(property instanceof NumberProperty numberProperty)) continue;

                    ResourceLocation propKey = SgRegistries.GEAR_PROPERTY.getKey(property);
                    if (propKey == null) continue;
                    String key = propKey.getPath();

                    try {
                        Collection<NumberPropertyValue> mods = instance.getPropertyModifiers(
                                partType,
                                PropertyKey.of(numberProperty, GearTypes.ALL.get())
                        );
                        if (mods.isEmpty()) continue;

                        double avg = 0, add = 0, mulBase = 0, mulTotal = 0, max = 0;
                        for (NumberPropertyValue mod : mods) {
                            float v = mod.value();
                            switch (mod.operation()) {
                                case AVERAGE -> avg += v;
                                case ADD -> add += v;
                                case MULTIPLY_BASE -> mulBase += v;
                                case MULTIPLY_TOTAL -> mulTotal += v;
                                case MAX -> max = Math.max(max, v);
                            }
                        }

                        StringBuilder sb = new StringBuilder();
                        for (NumberPropertyValue mod : numberProperty.sortForDisplay(mods)) {
                            String s = formatMod(numberProperty, mod);
                            if (!s.isEmpty()) {
                                if (sb.length() > 0) sb.append(" ");
                                sb.append(s);
                            }
                        }

                        PartData.AttributeValue value = new PartData.AttributeValue(
                                avg, add, mulBase, mulTotal, max, sb.toString());
                        if (!value.isEmpty()) {
                            attributes.put(key, value);
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

    private static String formatMod(NumberProperty property, NumberPropertyValue mod) {
        float v = mod.value();
        return switch (mod.operation()) {
            case ADD -> trim(String.format(Locale.ROOT, "%s%.2f", v < 0 ? "" : "+", v));
            case AVERAGE -> {
                if (property.getDisplayFormat() == NumberProperty.DisplayFormat.PERCENTAGE) {
                    yield Math.round(v * 100) + "%";
                }
                String s = trim(String.format(Locale.ROOT, "%.2f", v));
                yield property.getDisplayFormat() == NumberProperty.DisplayFormat.MULTIPLIER
                        ? s + "x" : s;
            }
            case MAX -> trim(String.format(Locale.ROOT, "↑%.2f", v));
            case MULTIPLY_BASE -> (Math.round(100 * v) >= 0 ? "+" : "") + Math.round(100 * v) + "%";
            case MULTIPLY_TOTAL -> trim(String.format(Locale.ROOT, "x%.2f", 1 + v));
        };
    }

    private static String trim(String s) {
        if (s.endsWith(".0")) return s.substring(0, s.length() - 2);
        if (s.contains(".")) {
            s = s.replaceAll("0+$", "");
            if (s.endsWith(".")) s = s.substring(0, s.length() - 1);
        }
        return s;
    }

    private static final class MaterialCatalogEntry implements CatalogEntry, CatalogApi.MaterialView {
        private final String id;
        private final String name;
        private final int tier;
        private final List<PartData> partDataList;
        private final List<PartData.TraitData> allTraits;
        private final Map<String, Map<String, PartData.AttributeValue>> attributesByPart;
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
                             Map<String, Map<String, PartData.AttributeValue>> attributesByPart,
                             int sortOrder, int availableParts, Set<String> categories,
                             int color, String parentId, boolean simple, Set<String> supportedPartTypes) {
            this.id = id;
            this.name = name;
            this.tier = tier;
            this.partDataList = List.copyOf(partDataList);
            this.allTraits = List.copyOf(allTraits);
            this.attributesByPart = attributesByPart;
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
        public Map<String, PartData.AttributeValue> getAttributeValues() {
            Map<String, PartData.AttributeValue> merged = new LinkedHashMap<>();
            for (PartData partData : partDataList) {
                for (Map.Entry<String, PartData.AttributeValue> e : partData.attributeValues().entrySet()) {
                    merged.merge(e.getKey(), e.getValue(), (a, b) -> new PartData.AttributeValue(
                            a.average() + b.average(),
                            a.add() + b.add(),
                            a.multiplyBase() + b.multiplyBase(),
                            a.multiplyTotal() + b.multiplyTotal(),
                            Math.max(a.max(), b.max()),
                            a.displayText().isEmpty() ? b.displayText()
                                    : (b.displayText().isEmpty() ? a.displayText()
                                    : a.displayText() + " " + b.displayText())
                    ));
                }
            }
            return merged;
        }

        @Override public String getId() { return id; }
        @Override public String getName() { return name; }
        @Override public int getMaterialLevel() { return tier; }
        @Override public List<String> getTraitNames() { return traitNames; }
        @Override public Map<String, Integer> getTraitLevels() { return traitLevels; }
        @Override public Set<String> getTraitIds() { return traitIds; }
        @Override public List<String> getTraitDescriptions() { return traitDescriptions; }
        @Override public ItemStack getDisplayStack() { return displayStack.copy(); }
        @Override public String getSearchText() { return searchText; }
        @Override public List<TraitTooltip> getTraitTooltips() { return traitTooltips; }
        @Override public Set<String> getCategories() { return categories; }
        @Override public int getDefaultSortOrder() { return sortOrder; }
        @Override public int getAvailablePartCount() { return availableParts; }
        @Override public ItemStack getMaterialDisplayItem() { return displayStack.copy(); }
        @Override public int getColor() { return color; }
        @Override public String getParentId() { return parentId; }
        @Override public boolean isSimple() { return simple; }
        @Override public Set<String> getSupportedPartTypes() { return supportedPartTypes; }
        @Override public List<PartData> getPartData() { return partDataList; }
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
        @Override public Map<String, PartData.AttributeValue> getAttributeValues() { return Map.of(); }
        @Override public ItemStack getDisplayStack() { return displayStack.copy(); }
        @Override public String getSearchText() { return searchText; }
        @Override public List<TraitTooltip> getTraitTooltips() { return traitTooltips; }
        @Override public int getMaxLevel() { return maxLevel; }
        @Override public List<CatalogEntry> getSourceMaterials() { return sourceMaterials; }
        @Override public List<ItemStack> getSourceItems() { return List.of(); }
    }
}