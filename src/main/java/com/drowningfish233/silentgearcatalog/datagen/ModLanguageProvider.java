package com.drowningfish233.silentgearcatalog.datagen;

import com.drowningfish233.silentgearcatalog.SilentGearCatalog;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.LanguageProvider;

public class ModLanguageProvider extends LanguageProvider {
    public static final String EN_US = "en_us";
    public static final String ZH_CN = "zh_cn";

    private final String locale;

    public ModLanguageProvider(PackOutput output, String locale) {
        super(output, SilentGearCatalog.MODID, locale);
        this.locale = locale;
    }

    @Override
    protected void addTranslations() {
        switch (locale) {
            case EN_US -> addEnglish();
            case ZH_CN -> addChinese();
        }
    }

    private void addEnglish() {
        add("item.silentgearcatalog.catalog", "Silent Gear Catalog");
        add("item.silentgearcatalog.catalog.tooltip", "A powerful tome for querying Silent Gear materials and traits");
        add("itemGroup.silentgearcatalog", "Silent Gear Catalog");

        add("screen.silentgearcatalog.catalog", "Silent Gear Catalog");
        add("gui.silentgearcatalog.search", "Search");
        add("gui.silentgearcatalog.search_hint", "Search materials or traits...");

        add("button.silentgearcatalog.materials", "Materials");
        add("button.silentgearcatalog.traits", "Traits");
        add("button.silentgearcatalog.filter", "Filter");
        add("button.silentgearcatalog.part", "Part");
        add("button.silentgearcatalog.sort", "Sort");
        add("button.silentgearcatalog.sort_asc", "↑");
        add("button.silentgearcatalog.sort_desc", "↓");

        add("catalog.silentgearcatalog.nav", "Navigation");
        add("catalog.silentgearcatalog.loading", "Loading data...");
        add("catalog.silentgearcatalog.no_entries", "No matching entries");
        add("catalog.silentgearcatalog.no_materials_with_filter", "No materials have the selected attributes");
        add("catalog.silentgearcatalog.entry_count", "%d entries");
        add("catalog.silentgearcatalog.filter_count", " | Filter:%d");
        add("catalog.silentgearcatalog.part_count", " | Part:%d");
        add("catalog.silentgearcatalog.sort_label", " | %s %s");
        add("catalog.silentgearcatalog.id", "ID");
        add("catalog.silentgearcatalog.level", "Level");
        add("catalog.silentgearcatalog.trait_count_short", "Traits:");

        add("filter.silentgearcatalog.filter_attributes", "Filter Attributes");
        add("filter.silentgearcatalog.select_all", "Select All");
        add("filter.silentgearcatalog.clear", "Clear");
        add("filter.silentgearcatalog.has_traits", "Has Traits");

        add("part.silentgearcatalog.filter_parts", "Filter Parts");

        add("sort.silentgearcatalog.sort_by", "Sort By");
        add("sort.silentgearcatalog.name", "Name");
        add("sort.silentgearcatalog.level", "Level");
        add("sort.silentgearcatalog.trait_count", "Trait Count");

        add("detail.silentgearcatalog.traits", "Traits");
        add("detail.silentgearcatalog.attributes", "Attributes");
        add("detail.silentgearcatalog.description", "Description");
        add("detail.silentgearcatalog.click_trait", "(Click trait to view materials with this trait)");
        add("detail.silentgearcatalog.click_to_view_materials", "Click to view materials with this trait");

        add("trait.silentgearcatalog.materials_with", "Materials with \"%s\"");
        add("trait.silentgearcatalog.material_count", "%d materials");
        add("trait.silentgearcatalog.no_materials", "No materials have this trait");

    }

    private void addChinese() {
        add("item.silentgearcatalog.catalog", "寂静装备目录");
        add("item.silentgearcatalog.catalog.tooltip", "可以用来查询寂静装备材料/特性的强大宝典");
        add("itemGroup.silentgearcatalog", "寂静装备目录");

        add("screen.silentgearcatalog.catalog", "寂静装备目录");
        add("gui.silentgearcatalog.search", "搜索");
        add("gui.silentgearcatalog.search_hint", "搜索材料或特性...");

        add("button.silentgearcatalog.materials", "材料");
        add("button.silentgearcatalog.traits", "特性");
        add("button.silentgearcatalog.filter", "筛选");
        add("button.silentgearcatalog.part", "部位");
        add("button.silentgearcatalog.sort", "排序");
        add("button.silentgearcatalog.sort_asc", "↑");
        add("button.silentgearcatalog.sort_desc", "↓");

        add("catalog.silentgearcatalog.nav", "导航");
        add("catalog.silentgearcatalog.loading", "加载数据中...");
        add("catalog.silentgearcatalog.no_entries", "没有匹配的条目");
        add("catalog.silentgearcatalog.no_materials_with_filter", "没有材料拥有选中的属性");
        add("catalog.silentgearcatalog.entry_count", "%d 个条目");
        add("catalog.silentgearcatalog.filter_count", " | 筛选:%d项");
        add("catalog.silentgearcatalog.part_count", " | 部位:%d项");
        add("catalog.silentgearcatalog.sort_label", " | %s %s");
        add("catalog.silentgearcatalog.id", "ID");
        add("catalog.silentgearcatalog.level", "等级");
        add("catalog.silentgearcatalog.trait_count_short", "特性:");

        add("filter.silentgearcatalog.filter_attributes", "筛选属性");
        add("filter.silentgearcatalog.select_all", "全选");
        add("filter.silentgearcatalog.clear", "清除");
        add("filter.silentgearcatalog.has_traits", "有特性");

        add("part.silentgearcatalog.filter_parts", "部位筛选");

        add("sort.silentgearcatalog.sort_by", "排序方式");
        add("sort.silentgearcatalog.name", "名称");
        add("sort.silentgearcatalog.level", "等级");
        add("sort.silentgearcatalog.trait_count", "特性数量");

        add("detail.silentgearcatalog.traits", "特性");
        add("detail.silentgearcatalog.attributes", "属性");
        add("detail.silentgearcatalog.description", "描述");
        add("detail.silentgearcatalog.click_trait", "(点击特性查看拥有该特性的材料)");
        add("detail.silentgearcatalog.click_to_view_materials", "点击查看拥有此特性的材料");

        add("trait.silentgearcatalog.materials_with", "拥有「%s」的材料");
        add("trait.silentgearcatalog.material_count", "%d 个材料");
        add("trait.silentgearcatalog.no_materials", "没有材料拥有此特性");
    }
}