package com.drowningfish233.silentgearcatalog.client.gui.screen;

import com.drowningfish233.silentgearcatalog.Utils.SearchUtils;
import com.drowningfish233.silentgearcatalog.client.gui.catalog.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;

import java.util.*;
import java.util.stream.Collectors;

public class CatalogScreen extends Screen {
    private static final int PANEL_MAX_WIDTH = 920;
    private static final int PANEL_MAX_HEIGHT = 640;
    private static final int NAV_WIDTH = 80;
    private static final int ROW_HEIGHT = 30;
    private static final int DETAIL_WIDTH = 240;
    private static final int DETAIL_MAX_HEIGHT = 300;

    private static final State STATE = new State();

    private static final class State {
        Page page = Page.MATERIALS;
        String searchQuery = "";
        String sortKey = "_name";
        boolean sortDescending = false;
        AttributeSort.SortMode sortMode = AttributeSort.SortMode.EFFECTIVE;
        Set<String> activeFilters = new HashSet<>();
        Set<String> activePartFilters = new HashSet<>();
        int mainScroll = 0;
        int filterPopupScroll = 0;
        int partFilterPopupScroll = 0;
        int sortPopupScroll = 0;
        int sortModePopupScroll = 0;
    }

    private final Screen parent;
    private CatalogSnapshot snapshot = CatalogSnapshot.loading();
    private List<CatalogEntry> visibleEntries = List.of();

    private Page page = STATE.page;
    private EditBox searchBox;
    private CatalogButton materialButton;
    private CatalogButton traitButton;
    private CatalogButton filterButton;
    private CatalogButton partFilterButton;
    private CatalogButton sortButton;
    private CatalogButton sortOrderButton;
    private CatalogButton sortModeButton;
    private CatalogButton resetButton;
    private String searchQuery = STATE.searchQuery;
    private int mainScroll = STATE.mainScroll;

    private String currentSortKey = STATE.sortKey;
    private boolean sortDescending = STATE.sortDescending;
    private AttributeSort.SortMode sortMode = STATE.sortMode;
    private boolean showSortPopup = false;
    private Map<String, AttributeSort> availableSorts = new LinkedHashMap<>();
    private int sortPopupScroll = STATE.sortPopupScroll;
    private static final int SORT_POPUP_MAX_ROWS = 12;

    private boolean showSortModePopup = false;
    private int sortModePopupScroll = STATE.sortModePopupScroll;
    private static final int SORT_MODE_POPUP_MAX_ROWS = 12;

    private boolean showFilterPopup = false;
    private Set<String> activeFilters = new HashSet<>(STATE.activeFilters);
    private List<AttributeFilter> availableFilters = new ArrayList<>();
    private int filterPopupScroll = STATE.filterPopupScroll;
    private static final int FILTER_POPUP_MAX_ROWS = 12;

    private boolean showPartFilterPopup = false;
    private Set<String> activePartFilters = new HashSet<>(STATE.activePartFilters);
    private List<PartTypeFilter> partTypeFilters = new ArrayList<>();
    private int partFilterPopupScroll = STATE.partFilterPopupScroll;
    private static final int PART_FILTER_POPUP_MAX_ROWS = 12;

    private CatalogEntry detailEntry = null;
    private int detailX, detailY, detailWidth, detailHeight;
    private int detailScroll = 0;
    private int detailContentHeight = 0;
    private boolean detailLocked = false;

    private String hoveredTraitId = null;
    private String hoveredTraitName = null;
    private int hoveredTraitY = 0;

    private boolean isDraggingScroll = false;
    private int dragStartY = 0;
    private int dragStartScroll = 0;
    private int scrollBarX = 0;
    private int scrollBarY = 0;
    private int scrollBarHeight = 0;
    private int thumbY = 0;
    private int thumbHeight = 0;
    private int scrollTrackHeight = 0;
    private int totalRows = 0;
    private int visibleRowCount = 0;

    private int panelX, panelY, panelWidth, panelHeight;
    private int contentX, contentY, contentWidth;
    private int listY, listBottom;

    public CatalogScreen(Screen parent) {
        super(Component.translatable("screen.silentgearcatalog.catalog"));
        this.parent = parent;
        this.detailWidth = DETAIL_WIDTH;
        this.page = STATE.page;
        this.searchQuery = STATE.searchQuery;
        this.currentSortKey = STATE.sortKey;
        this.sortDescending = STATE.sortDescending;
        this.sortMode = STATE.sortMode;
        this.mainScroll = STATE.mainScroll;
        this.filterPopupScroll = STATE.filterPopupScroll;
        this.partFilterPopupScroll = STATE.partFilterPopupScroll;
        this.sortPopupScroll = STATE.sortPopupScroll;
        this.sortModePopupScroll = STATE.sortModePopupScroll;
        this.activeFilters = new HashSet<>(STATE.activeFilters);
        this.activePartFilters = new HashSet<>(STATE.activePartFilters);
    }

    @Override
    protected void init() {
        super.init();
        this.updateLayout();
        this.createWidgets();
        this.reloadCatalog();
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {}

    private void updateLayout() {
        this.panelWidth = Math.min(PANEL_MAX_WIDTH, Math.max(360, this.width - 20));
        this.panelHeight = Math.min(PANEL_MAX_HEIGHT, Math.max(300, this.height - 20));
        this.panelX = (this.width - this.panelWidth) / 2;
        this.panelY = (this.height - this.panelHeight) / 2;
        this.contentX = this.panelX + NAV_WIDTH + 10;
        this.contentY = this.panelY + 35;
        this.contentWidth = this.panelWidth - NAV_WIDTH - 20;
        this.listY = this.panelY + 80;
        this.listBottom = this.panelY + this.panelHeight - 13;
        this.detailWidth = Math.min(DETAIL_WIDTH, this.contentWidth - 20);
    }

    private void createWidgets() {
        this.materialButton = this.addRenderableWidget(new CatalogButton(
                this.panelX + 6, this.panelY + 50, NAV_WIDTH - 12, 20,
                Component.translatable("button.silentgearcatalog.materials"),
                b -> this.switchPage(Page.MATERIALS)
        ));
        this.traitButton = this.addRenderableWidget(new CatalogButton(
                this.panelX + 6, this.panelY + 74, NAV_WIDTH - 12, 20,
                Component.translatable("button.silentgearcatalog.traits"),
                b -> this.switchPage(Page.TRAITS)
        ));
        this.updateNavigationButtons();

        int searchX = this.contentX;
        int searchY = this.panelY + 6;
        int searchWidth = Math.max(120, this.contentWidth - 342);

        this.searchBox = this.addRenderableWidget(new EditBox(
                this.font, searchX, searchY, searchWidth, 20,
                Component.translatable("gui.silentgearcatalog.search")
        ));
        this.searchBox.setMaxLength(120);
        this.searchBox.setHint(Component.translatable("gui.silentgearcatalog.search_hint"));
        this.searchBox.setValue(this.searchQuery);
        this.searchBox.setResponder(this::onSearchChanged);

        int btnX = searchX + searchWidth + 6;

        this.filterButton = this.addRenderableWidget(new CatalogButton(
                btnX, searchY, 50, 20,
                Component.translatable("button.silentgearcatalog.filter"),
                b -> this.toggleFilterPopup()
        ));

        this.partFilterButton = this.addRenderableWidget(new CatalogButton(
                btnX + 54, searchY, 50, 20,
                Component.translatable("button.silentgearcatalog.part"),
                b -> this.togglePartFilterPopup()
        ));

        this.sortButton = this.addRenderableWidget(new CatalogButton(
                btnX + 108, searchY, 50, 20,
                Component.translatable("button.silentgearcatalog.sort"),
                b -> this.toggleSortPopup()
        ));

        this.sortModeButton = this.addRenderableWidget(new CatalogButton(
                btnX + 162, searchY, 50, 20,
                Component.literal(this.sortMode.displayName()),
                b -> this.toggleSortModePopup()
        ));

        this.sortOrderButton = this.addRenderableWidget(new CatalogButton(
                btnX + 216, searchY, 30, 20,
                Component.translatable(this.sortDescending ? "button.silentgearcatalog.sort_desc" : "button.silentgearcatalog.sort_asc"),
                b -> this.toggleSortOrder()
        ));

        this.resetButton = this.addRenderableWidget(new CatalogButton(
                btnX + 250, searchY, 24, 20,
                Component.literal("↺"),
                b -> this.resetAllSettings()
        ));

        this.updateSortOrderButton();
    }

    private void toggleFilterPopup() {
        this.showFilterPopup = !this.showFilterPopup;
        this.filterPopupScroll = 0;
        if (this.showFilterPopup) {
            this.showSortPopup = false;
            this.showPartFilterPopup = false;
            this.showSortModePopup = false;
        }
    }

    private void togglePartFilterPopup() {
        this.showPartFilterPopup = !this.showPartFilterPopup;
        this.partFilterPopupScroll = 0;
        if (this.showPartFilterPopup) {
            this.showFilterPopup = false;
            this.showSortPopup = false;
            this.showSortModePopup = false;
            this.updatePartFilterOptions();
        }
    }

    private void toggleSortPopup() {
        this.showSortPopup = !this.showSortPopup;
        this.sortPopupScroll = 0;
        if (this.showSortPopup) {
            this.showFilterPopup = false;
            this.showPartFilterPopup = false;
            this.showSortModePopup = false;
        }
    }

    private void toggleSortModePopup() {
        this.showSortModePopup = !this.showSortModePopup;
        this.sortModePopupScroll = 0;
        if (this.showSortModePopup) {
            this.showFilterPopup = false;
            this.showPartFilterPopup = false;
            this.showSortPopup = false;
        }
    }

    private void toggleSortOrder() {
        this.sortDescending = !this.sortDescending;
        this.updateSortOrderButton();
        this.rebuildVisibleEntries();
        this.updateButtonStates();
    }

    private void selectSortMode(AttributeSort.SortMode mode) {
        this.sortMode = mode;
        this.sortModeButton.setMessage(Component.literal(this.sortMode.displayName()));
        this.showSortModePopup = false;
        this.rebuildVisibleEntries();
    }

    private void resetAllSettings() {
        this.page = Page.MATERIALS;
        this.searchQuery = "";
        this.currentSortKey = "_name";
        this.sortDescending = false;
        this.sortMode = AttributeSort.SortMode.EFFECTIVE;
        this.activeFilters.clear();
        this.activePartFilters.clear();
        this.mainScroll = 0;
        this.detailEntry = null;
        this.detailLocked = false;
        this.hoveredTraitName = null;
        this.hoveredTraitId = null;

        if (this.searchBox != null) {
            this.searchBox.setValue("");
        }
        if (this.sortModeButton != null) {
            this.sortModeButton.setMessage(Component.literal(this.sortMode.displayName()));
        }

        STATE.searchQuery = "";
        STATE.sortKey = "_name";
        STATE.sortDescending = false;
        STATE.sortMode = AttributeSort.SortMode.EFFECTIVE;
        STATE.activeFilters.clear();
        STATE.activePartFilters.clear();
        STATE.mainScroll = 0;
        STATE.filterPopupScroll = 0;
        STATE.partFilterPopupScroll = 0;
        STATE.sortPopupScroll = 0;
        STATE.sortModePopupScroll = 0;

        this.updateNavigationButtons();
        this.updateSortOrderButton();
        this.updateSortOptions();
        this.updateFilterOptions();
        this.updatePartFilterOptions();
        this.rebuildVisibleEntries();
        this.updateButtonStates();

        this.showFilterPopup = false;
        this.showPartFilterPopup = false;
        this.showSortPopup = false;
        this.showSortModePopup = false;
    }

    private void updateSortOrderButton() {
        if (this.sortOrderButton != null) {
            this.sortOrderButton.setMessage(Component.translatable(this.sortDescending ? "button.silentgearcatalog.sort_desc" : "button.silentgearcatalog.sort_asc"));
        }
    }

    private void onSearchChanged(String value) {
        this.searchQuery = value;
        this.mainScroll = 0;
        this.rebuildVisibleEntries();
        this.updateButtonStates();
    }

    private void reloadCatalog() {
        this.snapshot = CatalogDataBuilder.build();
        this.mainScroll = 0;
        this.updateSortOptions();
        this.updateFilterOptions();
        this.updatePartFilterOptions();
        this.rebuildVisibleEntries();
        this.updateButtonStates();
    }

    private void updateSortOptions() {
        if (!this.snapshot.fullyLoaded()) return;
        List<CatalogEntry> source = this.page == Page.MATERIALS
                ? this.snapshot.materials()
                : this.snapshot.traits();
        this.availableSorts = AttributeSort.buildFromEntries(source);
        if (!this.availableSorts.containsKey(this.currentSortKey)) {
            this.currentSortKey = "_name";
        }
    }

    private void updateFilterOptions() {
        if (!this.snapshot.fullyLoaded()) return;
        List<CatalogEntry> source = this.page == Page.MATERIALS
                ? this.snapshot.materials()
                : this.snapshot.traits();
        this.availableFilters = AttributeFilter.buildFromEntries(source);
        Set<String> validIds = this.availableFilters.stream()
                .map(AttributeFilter::id).collect(Collectors.toSet());
        this.activeFilters.retainAll(validIds);
    }

    private void updatePartFilterOptions() {
        if (!this.snapshot.fullyLoaded()) return;
        List<CatalogEntry> source = this.page == Page.MATERIALS
                ? this.snapshot.materials()
                : this.snapshot.traits();
        this.partTypeFilters = PartTypeFilter.buildFromEntries(source);
        Set<String> validIds = this.partTypeFilters.stream()
                .map(PartTypeFilter::id).collect(Collectors.toSet());
        this.activePartFilters.retainAll(validIds);
        if (this.activePartFilters.isEmpty() && !this.partTypeFilters.isEmpty()) {
            for (PartTypeFilter filter : this.partTypeFilters) {
                this.activePartFilters.add(filter.id());
            }
        }
    }

    private void switchPage(Page nextPage) {
        if (this.page != nextPage) {
            this.page = nextPage;
            this.mainScroll = 0;
            this.detailEntry = null;
            this.detailLocked = false;
            this.hoveredTraitId = null;
            this.hoveredTraitName = null;
            this.updateNavigationButtons();
            this.updateSortOptions();
            this.updateFilterOptions();
            this.updatePartFilterOptions();
            this.rebuildVisibleEntries();
            this.updateButtonStates();
        }
    }

    private void updateNavigationButtons() {
        if (this.materialButton != null && this.traitButton != null) {
            this.materialButton.active = this.page != Page.MATERIALS;
            this.traitButton.active = this.page != Page.TRAITS;
            this.materialButton.setSelected(this.page == Page.MATERIALS);
            this.traitButton.setSelected(this.page == Page.TRAITS);
        }
    }

    private void rebuildVisibleEntries() {
        if (!this.snapshot.fullyLoaded()) {
            this.visibleEntries = List.of();
            return;
        }

        List<CatalogEntry> source;
        if (this.page == Page.MATERIALS) {
            source = this.snapshot.materials();
        } else {
            source = this.snapshot.traits();
        }

        List<CatalogEntry> filtered = new ArrayList<>(source);

        if (this.page == Page.MATERIALS && !this.activeFilters.isEmpty()) {
            filtered.removeIf(entry -> {
                for (String filterId : this.activeFilters) {
                    for (AttributeFilter filter : this.availableFilters) {
                        if (filter.id().equals(filterId) && !filter.test(entry, this.activePartFilters)) {
                            return true;
                        }
                    }
                }
                return false;
            });
        }

        if (this.page == Page.MATERIALS && !this.activePartFilters.isEmpty()) {
            filtered.removeIf(entry -> {
                if (!(entry instanceof CatalogApi.MaterialView materialView)) return true;
                for (String partId : this.activePartFilters) {
                    if (materialView.getSupportedPartTypes().contains(partId)) {
                        return false;
                    }
                }
                return true;
            });
        }

        String query = this.searchQuery.trim();
        if (!query.isEmpty()) {
            filtered = filtered.stream()
                    .filter(e -> SearchUtils.matches(e.getSearchText(), query))
                    .collect(Collectors.toList());
        }

        AttributeSort sort = this.availableSorts.get(this.currentSortKey);
        if (sort != null) {
            filtered.sort(sort.getComparator(this.sortDescending, this.activePartFilters, this.sortMode));
        } else {
            filtered.sort(Comparator.comparing(CatalogEntry::getName));
        }

        this.visibleEntries = List.copyOf(filtered);
        int rows = this.visibleRows();
        this.totalRows = this.visibleEntries.size();
        this.visibleRowCount = rows;
        this.mainScroll = clamp(this.mainScroll, 0, Math.max(0, this.visibleEntries.size() - rows));

        this.updateButtonStates();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.hoveredTraitId = null;
        this.hoveredTraitName = null;

        renderPanel(graphics);
        renderNav(graphics);

        Component titleKey = this.page == Page.MATERIALS ?
                Component.translatable("button.silentgearcatalog.materials") :
                Component.translatable("button.silentgearcatalog.traits");
        graphics.drawString(this.font, Component.literal("§b寂静装备 §f- ").append(titleKey),
                this.panelX + NAV_WIDTH + 10, this.panelY + 10, 0xFFFFFF, false);

        for (var widget : this.children()) {
            if (widget instanceof net.minecraft.client.gui.components.Renderable renderable) {
                renderable.render(graphics, mouseX, mouseY, partialTick);
            }
        }

        renderList(graphics, mouseX, mouseY);
        renderStats(graphics);

        if (this.showFilterPopup) renderFilterPopup(graphics, mouseX, mouseY);
        if (this.showPartFilterPopup) renderPartFilterPopup(graphics, mouseX, mouseY);
        if (this.showSortPopup) renderSortPopup(graphics, mouseX, mouseY);
        if (this.showSortModePopup) renderSortModePopup(graphics, mouseX, mouseY);
        if (this.detailEntry != null) renderDetailPanel(graphics, mouseX, mouseY);
    }

    private void renderPanel(GuiGraphics graphics) {
        graphics.fill(this.panelX, this.panelY, this.panelX + this.panelWidth, this.panelY + this.panelHeight, 0xFF222222);
        graphics.fill(this.panelX + 1, this.panelY + 1, this.panelX + this.panelWidth - 1, this.panelY + this.panelHeight - 1, 0xFF333333);
        graphics.fill(this.panelX, this.panelY, this.panelX + this.panelWidth, this.panelY + 1, 0xFF444444);
        graphics.fill(this.panelX, this.panelY, this.panelX + 1, this.panelY + this.panelHeight, 0xFF444444);

        graphics.fill(this.panelX + 2, this.panelY + 2, this.panelX + NAV_WIDTH + 2, this.panelY + this.panelHeight - 2, 0xFF2A2A2A);
        graphics.fill(this.panelX + 3, this.panelY + 3, this.panelX + NAV_WIDTH + 1, this.panelY + this.panelHeight - 3, 0xFF1A1A1A);
        graphics.fill(this.panelX + NAV_WIDTH + 2, this.panelY + 2, this.panelX + NAV_WIDTH + 3, this.panelY + this.panelHeight - 2, 0xFF444444);

        graphics.fill(this.panelX + NAV_WIDTH + 4, this.panelY + 2, this.panelX + this.panelWidth - 2, this.panelY + 30, 0xFF2A2A2A);
        graphics.fill(this.panelX + NAV_WIDTH + 4, this.panelY + 30, this.panelX + this.panelWidth - 2, this.panelY + 31, 0xFF444444);

        graphics.fill(this.contentX, this.listY - 2, this.contentX + this.contentWidth, this.listBottom + 2, 0xFF2A2A2A);
    }

    private void renderNav(GuiGraphics graphics) {
        graphics.drawString(this.font, Component.translatable("catalog.silentgearcatalog.nav"),
                this.panelX + 12, this.panelY + 8, 0x888888, false);

        int highlightY = this.page == Page.MATERIALS ? this.panelY + 48 : this.panelY + 72;
        graphics.fill(this.panelX + 4, highlightY + 3, this.panelX + 6, highlightY + 21, 0xFF6600CC);

        graphics.drawString(this.font, Component.translatable("button.silentgearcatalog.materials"),
                this.panelX + 16, this.panelY + 52,
                this.page == Page.MATERIALS ? 0xFFFFFF : 0x888888, false);
        graphics.drawString(this.font, Component.translatable("button.silentgearcatalog.traits"),
                this.panelX + 16, this.panelY + 76,
                this.page == Page.TRAITS ? 0xFFFFFF : 0x888888, false);
    }

    private void renderList(GuiGraphics graphics, int mouseX, int mouseY) {
        if (!this.snapshot.fullyLoaded()) {
            graphics.drawString(this.font, Component.translatable("catalog.silentgearcatalog.loading"),
                    this.contentX + 10, this.listY + 20, 0x888888, false);
            return;
        }

        if (this.visibleEntries.isEmpty()) {
            Component msg = this.activeFilters.isEmpty() ?
                    Component.translatable("catalog.silentgearcatalog.no_entries") :
                    Component.translatable("catalog.silentgearcatalog.no_materials_with_filter");
            graphics.drawString(this.font, msg, this.contentX + 10, this.listY + 20, 0x888888, false);
            return;
        }

        int rows = this.visibleRows();
        int end = Math.min(this.visibleEntries.size(), this.mainScroll + rows);

        boolean mouseInDetail = this.detailEntry != null &&
                mouseX >= this.detailX && mouseX < this.detailX + this.detailWidth &&
                mouseY >= this.detailY && mouseY < this.detailY + this.detailHeight;

        boolean mouseInFilterPopup = false;
        if (this.showFilterPopup) {
            int popupX = this.filterButton.getX();
            int popupY = this.filterButton.getY() + this.filterButton.getHeight() + 2;
            int popupWidth = 160;
            int totalOptions = this.availableFilters.size();
            int visibleRows2 = Math.min(FILTER_POPUP_MAX_ROWS, totalOptions);
            int popupHeight = visibleRows2 * 18 + 18;

            if (popupX + popupWidth > this.panelX + this.panelWidth - 4) {
                popupX = this.panelX + this.panelWidth - popupWidth - 4;
            }
            if (popupX < this.panelX + 4) {
                popupX = this.panelX + 4;
            }
            if (popupY + popupHeight > this.panelY + this.panelHeight - 4) {
                popupY = this.panelY + this.panelHeight - popupHeight - 4;
            }
            if (popupY < this.panelY + 4) {
                popupY = this.panelY + 4;
            }

            mouseInFilterPopup = mouseX >= popupX && mouseX < popupX + popupWidth &&
                    mouseY >= popupY && mouseY < popupY + popupHeight;
        }

        boolean mouseInPartFilterPopup = false;
        if (this.showPartFilterPopup) {
            int popupX = this.partFilterButton.getX();
            int popupY = this.partFilterButton.getY() + this.partFilterButton.getHeight() + 2;
            int popupWidth = 160;
            int totalOptions = this.partTypeFilters.size();
            int visibleRows2 = Math.min(PART_FILTER_POPUP_MAX_ROWS, totalOptions);
            int popupHeight = visibleRows2 * 18 + 18;

            if (popupX + popupWidth > this.panelX + this.panelWidth - 4) {
                popupX = this.panelX + this.panelWidth - popupWidth - 4;
            }
            if (popupX < this.panelX + 4) {
                popupX = this.panelX + 4;
            }
            if (popupY + popupHeight > this.panelY + this.panelHeight - 4) {
                popupY = this.panelY + this.panelHeight - popupHeight - 4;
            }
            if (popupY < this.panelY + 4) {
                popupY = this.panelY + 4;
            }

            mouseInPartFilterPopup = mouseX >= popupX && mouseX < popupX + popupWidth &&
                    mouseY >= popupY && mouseY < popupY + popupHeight;
        }

        boolean mouseInSortPopup = false;
        if (this.showSortPopup) {
            int popupX = this.sortButton.getX();
            int popupY = this.sortButton.getY() + this.sortButton.getHeight() + 2;
            int popupWidth = 160;
            int totalOptions = this.availableSorts.size();
            int visibleRows2 = Math.min(SORT_POPUP_MAX_ROWS, totalOptions);
            int popupHeight = visibleRows2 * 18 + 18;

            if (popupX + popupWidth > this.panelX + this.panelWidth - 4) {
                popupX = this.panelX + this.panelWidth - popupWidth - 4;
            }
            if (popupX < this.panelX + 4) {
                popupX = this.panelX + 4;
            }
            if (popupY + popupHeight > this.panelY + this.panelHeight - 4) {
                popupY = this.panelY + this.panelHeight - popupHeight - 4;
            }
            if (popupY < this.panelY + 4) {
                popupY = this.panelY + 4;
            }

            mouseInSortPopup = mouseX >= popupX && mouseX < popupX + popupWidth &&
                    mouseY >= popupY && mouseY < popupY + popupHeight;
        }

        boolean mouseInSortModePopup = false;
        if (this.showSortModePopup) {
            int popupX = this.sortModeButton.getX();
            int popupY = this.sortModeButton.getY() + this.sortModeButton.getHeight() + 2;
            int popupWidth = 120;
            int totalOptions = AttributeSort.SortMode.values().length;
            int visibleRows2 = Math.min(SORT_MODE_POPUP_MAX_ROWS, totalOptions);
            int popupHeight = visibleRows2 * 18 + 18;

            if (popupX + popupWidth > this.panelX + this.panelWidth - 4) {
                popupX = this.panelX + this.panelWidth - popupWidth - 4;
            }
            if (popupX < this.panelX + 4) {
                popupX = this.panelX + 4;
            }
            if (popupY + popupHeight > this.panelY + this.panelHeight - 4) {
                popupY = this.panelY + this.panelHeight - popupHeight - 4;
            }
            if (popupY < this.panelY + 4) {
                popupY = this.panelY + 4;
            }

            mouseInSortModePopup = mouseX >= popupX && mouseX < popupX + popupWidth &&
                    mouseY >= popupY && mouseY < popupY + popupHeight;
        }

        boolean mouseInPopup = mouseInDetail || mouseInFilterPopup || mouseInPartFilterPopup || mouseInSortPopup || mouseInSortModePopup;

        for (int index = this.mainScroll; index < end; index++) {
            int rowY = this.listY + (index - this.mainScroll) * ROW_HEIGHT;
            CatalogEntry entry = this.visibleEntries.get(index);

            boolean hovered = !mouseInPopup && mouseX >= this.contentX && mouseX < this.contentX + this.contentWidth &&
                    mouseY >= rowY && mouseY < rowY + ROW_HEIGHT;

            graphics.fill(this.contentX, rowY, this.contentX + this.contentWidth, rowY + ROW_HEIGHT,
                    hovered ? 0x44FFFFFF : (index % 2 == 0 ? 0x22FFFFFF : 0x11FFFFFF));
            graphics.fill(this.contentX, rowY + ROW_HEIGHT - 1, this.contentX + this.contentWidth, rowY + ROW_HEIGHT, 0x22FFFFFF);

            ItemStack stack = entry.getDisplayStack();
            if (!stack.isEmpty()) {
                graphics.renderItem(stack, this.contentX + 4, rowY + 6);
            } else {
                graphics.fill(this.contentX + 4, rowY + 6, this.contentX + 22, rowY + 24, 0x33FFFFFF);
            }

            String name = this.font.plainSubstrByWidth(entry.getName(), this.contentWidth - 130);
            int color = hovered ? 0xFFFFAA : 0xFFFFFF;
            graphics.drawString(this.font, name, this.contentX + 26, rowY + 10, color, false);

            String sortValue = getSortValue(entry);
            if (!sortValue.isEmpty()) {
                int svX = this.contentX + this.contentWidth - 8 - this.font.width(sortValue);
                graphics.drawString(this.font, sortValue, svX, rowY + 10, 0xFFAA00, false);
            }

            if (entry.getMaterialLevel() > 0) {
                String levelText = "Lv." + entry.getMaterialLevel();
                int lvX = this.contentX + this.contentWidth - 8 - this.font.width(levelText)
                        - (sortValue.isEmpty() ? 0 : this.font.width(sortValue) + 8);
                graphics.drawString(this.font, levelText, lvX, rowY + 10, 0x88AAFF, false);
            }

            if (!entry.getTraitNames().isEmpty()) {
                String traitText = Component.translatable("catalog.silentgearcatalog.trait_count_short").getString() + entry.getTraitNames().size();
                int tx = this.contentX + this.contentWidth - 8 - this.font.width(traitText)
                        - (sortValue.isEmpty() ? 0 : this.font.width(sortValue) + 8)
                        - (entry.getMaterialLevel() > 0 ? this.font.width("Lv." + entry.getMaterialLevel()) + 8 : 0);
                graphics.drawString(this.font, traitText, tx, rowY + 10, 0x88FF88, false);
            }
        }

        if (this.visibleEntries.size() > rows) {
            int trackHeight = this.listBottom - this.listY - 4;
            this.scrollTrackHeight = trackHeight;
            int thumbHeight = Math.max(12, trackHeight * rows / this.visibleEntries.size());
            this.thumbHeight = thumbHeight;
            int thumbY = this.listY + 2 + (trackHeight - thumbHeight) * this.mainScroll /
                    Math.max(1, this.visibleEntries.size() - rows);
            this.thumbY = thumbY;

            this.scrollBarX = this.contentX + this.contentWidth + 4;
            this.scrollBarY = this.listY + 4;
            this.scrollBarHeight = trackHeight;

            graphics.fill(this.scrollBarX, this.scrollBarY,
                    this.scrollBarX + 4, this.scrollBarY + this.scrollBarHeight, 0x44FFFFFF);
            graphics.fill(this.scrollBarX, this.thumbY,
                    this.scrollBarX + 4, this.thumbY + this.thumbHeight, 0xCCFFFFFF);
        } else {
            this.scrollBarHeight = 0;
        }
    }

    private String getSortValue(CatalogEntry entry) {
        AttributeSort sort = this.availableSorts.get(this.currentSortKey);
        if (sort == null || sort.attributeKey() == null) return "";

        PartData.AttributeValue best = null;

        if (!this.activePartFilters.isEmpty()) {
            for (PartData partData : entry.getPartData()) {
                if (this.activePartFilters.contains(partData.partType())) {
                    PartData.AttributeValue v = partData.attributeValues().get(sort.attributeKey());
                    if (v != null && (best == null || v.effective() > best.effective())) {
                        best = v;
                    }
                }
            }
        } else {
            for (PartData partData : entry.getPartData()) {
                PartData.AttributeValue v = partData.attributeValues().get(sort.attributeKey());
                if (v != null && (best == null || v.effective() > best.effective())) {
                    best = v;
                }
            }
        }

        return best == null ? "" : best.displayText();
    }

    private void renderStats(GuiGraphics graphics) {
        if (this.snapshot.fullyLoaded()) {
            Component entryText = Component.translatable("catalog.silentgearcatalog.entry_count", this.visibleEntries.size());
            MutableComponent text = entryText.copy();

            if (!this.activeFilters.isEmpty()) {
                text.append(Component.translatable("catalog.silentgearcatalog.filter_count", this.activeFilters.size()));
            }
            if (!this.activePartFilters.isEmpty()) {
                text.append(Component.translatable("catalog.silentgearcatalog.part_count", this.activePartFilters.size()));
            }

            AttributeSort sort = this.availableSorts.get(this.currentSortKey);
            String sortName = sort != null ? sort.getDisplayComponent().getString() :
                    Component.translatable("sort.silentgearcatalog.name").getString();
            Component orderKey = this.sortDescending ?
                    Component.translatable("button.silentgearcatalog.sort_desc") :
                    Component.translatable("button.silentgearcatalog.sort_asc");
            text.append(Component.translatable("catalog.silentgearcatalog.sort_label",
                    sortName + " " + this.sortMode.displayName(), orderKey.getString()));

            graphics.drawString(this.font, text, this.contentX, this.panelY + this.panelHeight - 12, 0x888888, false);
        } else {
            graphics.drawString(this.font, Component.translatable("catalog.silentgearcatalog.loading"),
                    this.contentX, this.panelY + this.panelHeight - 12, 0x888888, false);
        }
    }

    private void renderFilterPopup(GuiGraphics graphics, int mouseX, int mouseY) {
        int popupX = this.filterButton.getX();
        int popupY = this.filterButton.getY() + this.filterButton.getHeight() + 2;
        int popupWidth = 160;
        int totalOptions = this.availableFilters.size();
        int visibleRows = Math.min(FILTER_POPUP_MAX_ROWS, totalOptions);
        int popupHeight = visibleRows * 18 + 18;

        if (popupX + popupWidth > this.panelX + this.panelWidth - 4) {
            popupX = this.panelX + this.panelWidth - popupWidth - 4;
        }
        if (popupX < this.panelX + 4) {
            popupX = this.panelX + 4;
        }
        if (popupY + popupHeight > this.panelY + this.panelHeight - 4) {
            popupY = this.panelY + this.panelHeight - popupHeight - 4;
        }
        if (popupY < this.panelY + 4) {
            popupY = this.panelY + 4;
        }

        graphics.fill(popupX - 1, popupY - 1, popupX + popupWidth + 1, popupY + popupHeight + 1, 0xFF444444);
        graphics.fill(popupX, popupY, popupX + popupWidth, popupY + popupHeight, 0xFF222222);

        graphics.drawString(this.font, Component.translatable("filter.silentgearcatalog.filter_attributes"),
                popupX + 6, popupY + 3, 0x888888, false);
        graphics.fill(popupX + 2, popupY + 16, popupX + popupWidth - 2, popupY + 17, 0xFF444444);

        String selectAll = Component.translatable("filter.silentgearcatalog.select_all").getString();
        String clear = Component.translatable("filter.silentgearcatalog.clear").getString();
        int textX = popupX + popupWidth - 6 - this.font.width(clear);
        graphics.drawString(this.font, clear, textX, popupY + 3, 0xFF8888, false);
        textX = textX - this.font.width(selectAll) - 10;
        graphics.drawString(this.font, selectAll, textX, popupY + 3, 0x88FF88, false);

        int listStartY = popupY + 18;
        int listEndY = popupY + popupHeight - 2;
        graphics.enableScissor(popupX + 2, listStartY, popupX + popupWidth - 2, listEndY);

        int maxScroll = Math.max(0, totalOptions - visibleRows);
        this.filterPopupScroll = clamp(this.filterPopupScroll, 0, maxScroll);

        for (int i = 0; i < visibleRows && i + this.filterPopupScroll < totalOptions; i++) {
            int idx = i + this.filterPopupScroll;
            AttributeFilter filter = this.availableFilters.get(idx);
            int y = listStartY + i * 18;

            boolean selected = this.activeFilters.contains(filter.id());
            boolean hovered = mouseX >= popupX && mouseX < popupX + popupWidth &&
                    mouseY >= y && mouseY < y + 18;

            if (hovered) {
                graphics.fill(popupX + 2, y, popupX + popupWidth - 2, y + 18, 0x44FFFFFF);
            }

            String text = (selected ? "✓ " : "  ") + filter.displayName();
            String displayText = this.font.plainSubstrByWidth(text, popupWidth - 16);
            int color = selected ? 0xFFFFAA : (hovered ? 0xFFFFFF : 0xCCCCCC);
            graphics.drawString(this.font, displayText, popupX + 10, y + 4, color, false);

            if (selected) {
                graphics.fill(popupX + 2, y + 2, popupX + 4, y + 16, 0xFF6600CC);
            }
        }

        graphics.disableScissor();

        if (totalOptions > visibleRows) {
            int trackHeight = listEndY - listStartY;
            int thumbHeight = Math.max(8, trackHeight * visibleRows / totalOptions);
            int thumbY = listStartY + (trackHeight - thumbHeight) * this.filterPopupScroll / maxScroll;
            graphics.fill(popupX + popupWidth - 4, listStartY,
                    popupX + popupWidth - 2, listEndY, 0x44FFFFFF);
            graphics.fill(popupX + popupWidth - 4, thumbY,
                    popupX + popupWidth - 2, thumbY + thumbHeight, 0xCCFFFFFF);
        }
    }

    private void renderPartFilterPopup(GuiGraphics graphics, int mouseX, int mouseY) {
        int popupX = this.partFilterButton.getX();
        int popupY = this.partFilterButton.getY() + this.partFilterButton.getHeight() + 2;
        int popupWidth = 160;
        int totalOptions = this.partTypeFilters.size();
        int visibleRows = Math.min(PART_FILTER_POPUP_MAX_ROWS, totalOptions);
        int popupHeight = visibleRows * 18 + 18;

        if (popupX + popupWidth > this.panelX + this.panelWidth - 4) {
            popupX = this.panelX + this.panelWidth - popupWidth - 4;
        }
        if (popupX < this.panelX + 4) {
            popupX = this.panelX + 4;
        }
        if (popupY + popupHeight > this.panelY + this.panelHeight - 4) {
            popupY = this.panelY + this.panelHeight - popupHeight - 4;
        }
        if (popupY < this.panelY + 4) {
            popupY = this.panelY + 4;
        }

        graphics.fill(popupX - 1, popupY - 1, popupX + popupWidth + 1, popupY + popupHeight + 1, 0xFF444444);
        graphics.fill(popupX, popupY, popupX + popupWidth, popupY + popupHeight, 0xFF222222);

        graphics.drawString(this.font, Component.translatable("part.silentgearcatalog.filter_parts"),
                popupX + 6, popupY + 3, 0x888888, false);
        graphics.fill(popupX + 2, popupY + 16, popupX + popupWidth - 2, popupY + 17, 0xFF444444);

        String selectAll = Component.translatable("filter.silentgearcatalog.select_all").getString();
        String clear = Component.translatable("filter.silentgearcatalog.clear").getString();
        int textX = popupX + popupWidth - 6 - this.font.width(clear);
        graphics.drawString(this.font, clear, textX, popupY + 3, 0xFF8888, false);
        textX = textX - this.font.width(selectAll) - 10;
        graphics.drawString(this.font, selectAll, textX, popupY + 3, 0x88FF88, false);

        int listStartY = popupY + 18;
        int listEndY = popupY + popupHeight - 2;
        graphics.enableScissor(popupX + 2, listStartY, popupX + popupWidth - 2, listEndY);

        int maxScroll = Math.max(0, totalOptions - visibleRows);
        this.partFilterPopupScroll = clamp(this.partFilterPopupScroll, 0, maxScroll);

        for (int i = 0; i < visibleRows && i + this.partFilterPopupScroll < totalOptions; i++) {
            int idx = i + this.partFilterPopupScroll;
            PartTypeFilter filter = this.partTypeFilters.get(idx);
            int y = listStartY + i * 18;

            boolean selected = this.activePartFilters.contains(filter.id());
            boolean hovered = mouseX >= popupX && mouseX < popupX + popupWidth &&
                    mouseY >= y && mouseY < y + 18;

            if (hovered) {
                graphics.fill(popupX + 2, y, popupX + popupWidth - 2, y + 18, 0x44FFFFFF);
            }

            String text = (selected ? "✓ " : "  ") + filter.displayName();
            String displayText = this.font.plainSubstrByWidth(text, popupWidth - 16);
            int color = selected ? 0xFFFFAA : (hovered ? 0xFFFFFF : 0xCCCCCC);
            graphics.drawString(this.font, displayText, popupX + 10, y + 4, color, false);

            if (selected) {
                graphics.fill(popupX + 2, y + 2, popupX + 4, y + 16, 0xFF6600CC);
            }
        }

        graphics.disableScissor();

        if (totalOptions > visibleRows) {
            int trackHeight = listEndY - listStartY;
            int thumbHeight = Math.max(8, trackHeight * visibleRows / totalOptions);
            int thumbY = listStartY + (trackHeight - thumbHeight) * this.partFilterPopupScroll / maxScroll;
            graphics.fill(popupX + popupWidth - 4, listStartY,
                    popupX + popupWidth - 2, listEndY, 0x44FFFFFF);
            graphics.fill(popupX + popupWidth - 4, thumbY,
                    popupX + popupWidth - 2, thumbY + thumbHeight, 0xCCFFFFFF);
        }
    }

    private void renderSortPopup(GuiGraphics graphics, int mouseX, int mouseY) {
        int popupX = this.sortButton.getX();
        int popupY = this.sortButton.getY() + this.sortButton.getHeight() + 2;
        int popupWidth = 160;
        int totalOptions = this.availableSorts.size();
        int visibleRows = Math.min(SORT_POPUP_MAX_ROWS, totalOptions);
        int popupHeight = visibleRows * 18 + 18;

        if (popupX + popupWidth > this.panelX + this.panelWidth - 4) {
            popupX = this.panelX + this.panelWidth - popupWidth - 4;
        }
        if (popupX < this.panelX + 4) {
            popupX = this.panelX + 4;
        }
        if (popupY + popupHeight > this.panelY + this.panelHeight - 4) {
            popupY = this.panelY + this.panelHeight - popupHeight - 4;
        }
        if (popupY < this.panelY + 4) {
            popupY = this.panelY + 4;
        }

        graphics.fill(popupX - 1, popupY - 1, popupX + popupWidth + 1, popupY + popupHeight + 1, 0xFF444444);
        graphics.fill(popupX, popupY, popupX + popupWidth, popupY + popupHeight, 0xFF222222);

        graphics.drawString(this.font, Component.translatable("sort.silentgearcatalog.sort_by"),
                popupX + 6, popupY + 3, 0x888888, false);
        graphics.fill(popupX + 2, popupY + 16, popupX + popupWidth - 2, popupY + 17, 0xFF444444);

        int listStartY = popupY + 18;
        int listEndY = popupY + popupHeight - 2;
        graphics.enableScissor(popupX + 2, listStartY, popupX + popupWidth - 2, listEndY);

        String[] keys = this.availableSorts.keySet().toArray(new String[0]);
        int maxScroll = Math.max(0, totalOptions - visibleRows);
        this.sortPopupScroll = clamp(this.sortPopupScroll, 0, maxScroll);

        for (int i = 0; i < visibleRows && i + this.sortPopupScroll < totalOptions; i++) {
            int idx = i + this.sortPopupScroll;
            String key = keys[idx];
            AttributeSort sort = this.availableSorts.get(key);
            int y = listStartY + i * 18;

            boolean selected = key.equals(this.currentSortKey);
            boolean hovered = mouseX >= popupX && mouseX < popupX + popupWidth &&
                    mouseY >= y && mouseY < y + 18;

            if (hovered) {
                graphics.fill(popupX + 2, y, popupX + popupWidth - 2, y + 18, 0x44FFFFFF);
            }

            String text = (selected ? "✓ " : "  ") + sort.getDisplayComponent().getString();
            String displayText = this.font.plainSubstrByWidth(text, popupWidth - 16);
            int color = selected ? 0xFFFFAA : (hovered ? 0xFFFFFF : 0xCCCCCC);
            graphics.drawString(this.font, displayText, popupX + 10, y + 4, color, false);

            if (selected) {
                graphics.fill(popupX + 2, y + 2, popupX + 4, y + 16, 0xFF6600CC);
            }
        }

        graphics.disableScissor();

        if (totalOptions > visibleRows) {
            int trackHeight = listEndY - listStartY;
            int thumbHeight = Math.max(8, trackHeight * visibleRows / totalOptions);
            int thumbY = listStartY + (trackHeight - thumbHeight) * this.sortPopupScroll / maxScroll;
            graphics.fill(popupX + popupWidth - 4, listStartY,
                    popupX + popupWidth - 2, listEndY, 0x44FFFFFF);
            graphics.fill(popupX + popupWidth - 4, thumbY,
                    popupX + popupWidth - 2, thumbY + thumbHeight, 0xCCFFFFFF);
        }
    }

    private void renderSortModePopup(GuiGraphics graphics, int mouseX, int mouseY) {
        int popupX = this.sortModeButton.getX();
        int popupY = this.sortModeButton.getY() + this.sortModeButton.getHeight() + 2;
        int popupWidth = 120;
        int totalOptions = AttributeSort.SortMode.values().length;
        int visibleRows = Math.min(SORT_MODE_POPUP_MAX_ROWS, totalOptions);
        int popupHeight = visibleRows * 18 + 18;

        if (popupX + popupWidth > this.panelX + this.panelWidth - 4) {
            popupX = this.panelX + this.panelWidth - popupWidth - 4;
        }
        if (popupX < this.panelX + 4) {
            popupX = this.panelX + 4;
        }
        if (popupY + popupHeight > this.panelY + this.panelHeight - 4) {
            popupY = this.panelY + this.panelHeight - popupHeight - 4;
        }
        if (popupY < this.panelY + 4) {
            popupY = this.panelY + 4;
        }

        graphics.fill(popupX - 1, popupY - 1, popupX + popupWidth + 1, popupY + popupHeight + 1, 0xFF444444);
        graphics.fill(popupX, popupY, popupX + popupWidth, popupY + popupHeight, 0xFF222222);

        graphics.drawString(this.font, Component.translatable("sort.silentgearcatalog.sort_mode"),
                popupX + 6, popupY + 3, 0x888888, false);
        graphics.fill(popupX + 2, popupY + 16, popupX + popupWidth - 2, popupY + 17, 0xFF444444);

        int listStartY = popupY + 18;
        int listEndY = popupY + popupHeight - 2;
        graphics.enableScissor(popupX + 2, listStartY, popupX + popupWidth - 2, listEndY);

        int maxScroll = Math.max(0, totalOptions - visibleRows);
        this.sortModePopupScroll = clamp(this.sortModePopupScroll, 0, maxScroll);

        AttributeSort.SortMode[] modes = AttributeSort.SortMode.values();
        for (int i = 0; i < visibleRows && i + this.sortModePopupScroll < totalOptions; i++) {
            int idx = i + this.sortModePopupScroll;
            AttributeSort.SortMode mode = modes[idx];
            int y = listStartY + i * 18;

            boolean selected = mode == this.sortMode;
            boolean hovered = mouseX >= popupX && mouseX < popupX + popupWidth &&
                    mouseY >= y && mouseY < y + 18;

            if (hovered) {
                graphics.fill(popupX + 2, y, popupX + popupWidth - 2, y + 18, 0x44FFFFFF);
            }

            String text = (selected ? "✓ " : "  ") + mode.displayName();
            int color = selected ? 0xFFFFAA : (hovered ? 0xFFFFFF : 0xCCCCCC);
            graphics.drawString(this.font, text, popupX + 10, y + 4, color, false);

            if (selected) {
                graphics.fill(popupX + 2, y + 2, popupX + 4, y + 16, 0xFF6600CC);
            }
        }

        graphics.disableScissor();
    }

    private Component getPropertyDisplayName(String key) {
        String rawKey = key;
        if (rawKey.contains(":")) {
            rawKey = rawKey.substring(rawKey.indexOf(":") + 1);
        }
        String translationKey = "property.silentgear." + rawKey;
        return Component.translatable(translationKey);
    }

    private void renderDetailPanel(GuiGraphics graphics, int mouseX, int mouseY) {
        if (this.detailEntry == null) return;

        Font font = this.font;

        this.detailX = this.panelX + this.panelWidth - this.detailWidth - 4;
        this.detailY = this.listY + 10;

        this.detailContentHeight = calculateDetailHeight(font);
        int maxHeight = this.listBottom - this.listY - 20;
        this.detailHeight = Math.min(maxHeight, Math.max(60, this.detailContentHeight + 8));

        if (this.detailY + this.detailHeight > this.listBottom) {
            this.detailY = this.listBottom - this.detailHeight;
        }
        if (this.detailY < this.listY + 10) {
            this.detailY = this.listY + 10;
        }

        graphics.fill(this.detailX, this.detailY, this.detailX + this.detailWidth, this.detailY + this.detailHeight, 0xCC222222);
        graphics.fill(this.detailX + 1, this.detailY + 1, this.detailX + this.detailWidth - 1, this.detailY + this.detailHeight - 1, 0xFF333333);
        graphics.fill(this.detailX, this.detailY, this.detailX + this.detailWidth, this.detailY + 1, 0xFF444444);
        graphics.fill(this.detailX, this.detailY, this.detailX + 1, this.detailY + this.detailHeight, 0xFF444444);

        int clipX = this.detailX + 2;
        int clipY = this.detailY + 2;
        int clipW = this.detailWidth - 4;
        int clipH = this.detailHeight - 4;
        graphics.enableScissor(clipX, clipY, clipX + clipW, clipY + clipH);

        int y = this.detailY + 4 - this.detailScroll;

        String name = this.detailEntry.getName();
        graphics.drawString(font, name, this.detailX + 6, y, 0xFFFFFF, false);
        y += 14;

        String idText = Component.translatable("catalog.silentgearcatalog.id").getString();
        graphics.drawString(font, "§7" + idText + ": " + this.detailEntry.getId(), this.detailX + 6, y, 0x888888, false);
        y += 12;

        if (this.detailEntry.getMaterialLevel() > 0) {
            String levelText = Component.translatable("catalog.silentgearcatalog.level").getString();
            String level = levelText + ": " + this.detailEntry.getMaterialLevel();
            graphics.drawString(font, level, this.detailX + 6, y, 0xFFAA00, false);
            y += 12;
        }

        graphics.fill(this.detailX + 4, y, this.detailX + this.detailWidth - 4, y + 1, 0x44FFFFFF);
        y += 6;

        if (this.page == Page.TRAITS) {
            List<String> descs = this.detailEntry.getTraitDescriptions();
            if (!descs.isEmpty()) {
                graphics.drawString(font, Component.translatable("detail.silentgearcatalog.description"),
                        this.detailX + 6, y, 0xFFAA00, false);
                y += font.lineHeight + 2;
                for (String desc : descs) {
                    String wrapped = font.plainSubstrByWidth("  §f" + desc, this.detailWidth - 16);
                    graphics.drawString(font, wrapped, this.detailX + 6, y, 0xCCCCCC, false);
                    y += 11;
                }
            }

            y += 4;
            graphics.fill(this.detailX + 4, y, this.detailX + this.detailWidth - 4, y + 1, 0x44FFFFFF);
            y += 6;

            String linkText = Component.translatable("detail.silentgearcatalog.click_to_view_materials").getString();
            int linkX = this.detailX + 6;
            int linkY = y;
            int linkW = font.width(linkText) + 4;
            int linkH = 11;

            boolean hovered = mouseX >= linkX && mouseX < linkX + linkW &&
                    mouseY >= linkY && mouseY < linkY + linkH;
            if (hovered) {
                this.hoveredTraitId = this.detailEntry.getId();
                this.hoveredTraitName = this.detailEntry.getName();
                this.hoveredTraitY = y;
                graphics.fill(linkX - 2, linkY - 1, linkX + linkW + 2, linkY + linkH + 1, 0x44FFAA00);
            }

            int color = hovered ? 0xFFFFAA : 0x66CCFF;
            String text = hovered ? "§n§b" + linkText : "§b" + linkText;
            graphics.drawString(font, text, this.detailX + 6, y, color, false);
            y += 14;

            graphics.disableScissor();

            int maxScroll = Math.max(0, this.detailContentHeight - this.detailHeight + 8);
            if (maxScroll > 0) {
                int trackH = this.detailHeight - 8;
                int thumbH = Math.max(10, trackH * this.detailHeight / (this.detailHeight + maxScroll));
                int thumbY = this.detailY + 4 + (trackH - thumbH) * this.detailScroll / maxScroll;
                graphics.fill(this.detailX + this.detailWidth - 4, this.detailY + 4,
                        this.detailX + this.detailWidth - 2, this.detailY + this.detailHeight - 4, 0x44FFFFFF);
                graphics.fill(this.detailX + this.detailWidth - 4, thumbY,
                        this.detailX + this.detailWidth - 2, thumbY + thumbH, 0xCCFFFFFF);
            }

            String close = "✕";
            int closeX = this.detailX + this.detailWidth - 6 - font.width(close);
            graphics.drawString(font, close, closeX, this.detailY + 4, 0xFF6666, false);
            return;
        }

        List<PartData> allPartData = this.detailEntry.getPartData();

        List<PartData> filteredPartData = new ArrayList<>();
        if (!this.activePartFilters.isEmpty()) {
            for (PartData partData : allPartData) {
                if (this.activePartFilters.contains(partData.partType())) {
                    filteredPartData.add(partData);
                }
            }
        } else {
            filteredPartData = allPartData;
        }

        if (!filteredPartData.isEmpty()) {
            for (int i = 0; i < filteredPartData.size(); i++) {
                PartData partData = filteredPartData.get(i);

                String partTitle = partData.partTypeDisplayName();
                graphics.drawString(font, "§b■ " + partTitle, this.detailX + 6, y, 0x00BBFF, false);
                y += font.lineHeight + 2;

                List<PartData.TraitData> partTraits = partData.traits();
                if (!partTraits.isEmpty()) {
                    graphics.drawString(font, Component.translatable("detail.silentgearcatalog.click_trait"),
                            this.detailX + 6, y, 0x666666, false);
                    y += 10;
                    for (PartData.TraitData trait : partTraits) {
                        String traitDisplay = "  " + trait.name();
                        int traitX = this.detailX + 6;
                        int traitY = y;
                        int traitW = font.width(traitDisplay) + 4;
                        int traitH = 11;

                        boolean hovered = mouseX >= traitX && mouseX < traitX + traitW &&
                                mouseY >= traitY && mouseY < traitY + traitH;
                        if (hovered) {
                            this.hoveredTraitId = trait.id();
                            this.hoveredTraitName = trait.name();
                            this.hoveredTraitY = y;
                            graphics.fill(traitX - 2, traitY - 1, traitX + traitW + 2, traitY + traitH + 1, 0x44FFAA00);
                        }

                        int color = hovered ? 0xFFFFAA : 0x66CCFF;
                        String text = hovered ? "§n  " + trait.name() : "  §b" + trait.name();
                        graphics.drawString(font, text, this.detailX + 6, y, color, false);
                        y += 11;

                        if (!trait.descriptions().isEmpty()) {
                            String desc = trait.descriptions().get(0);
                            String wrappedDesc = font.plainSubstrByWidth("    §7" + desc, this.detailWidth - 16);
                            graphics.drawString(font, wrappedDesc, this.detailX + 6, y, 0x888888, false);
                            y += 10;
                        }
                    }
                }

                Map<String, PartData.AttributeValue> attrs = partData.attributeValues();
                if (!attrs.isEmpty()) {
                    graphics.drawString(font, Component.translatable("detail.silentgearcatalog.attributes"),
                            this.detailX + 6, y, 0x00BBFF, false);
                    y += font.lineHeight + 2;
                    for (Map.Entry<String, PartData.AttributeValue> entry : attrs.entrySet()) {
                        String key = entry.getKey();
                        Component keyName = getPropertyDisplayName(key);
                        String value = entry.getValue().displayText();
                        String text = "  §f" + keyName.getString() + ": §e" + value;
                        graphics.drawString(font, text, this.detailX + 6, y, 0xCCCCCC, false);
                        y += 11;
                    }
                }

                if (i < filteredPartData.size() - 1) {
                    y += 4;
                }
            }
        }

        graphics.disableScissor();

        int maxScroll = Math.max(0, this.detailContentHeight - this.detailHeight + 8);
        if (maxScroll > 0) {
            int trackH = this.detailHeight - 8;
            int thumbH = Math.max(10, trackH * this.detailHeight / (this.detailHeight + maxScroll));
            int thumbY = this.detailY + 4 + (trackH - thumbH) * this.detailScroll / maxScroll;
            graphics.fill(this.detailX + this.detailWidth - 4, this.detailY + 4,
                    this.detailX + this.detailWidth - 2, this.detailY + this.detailHeight - 4, 0x44FFFFFF);
            graphics.fill(this.detailX + this.detailWidth - 4, thumbY,
                    this.detailX + this.detailWidth - 2, thumbY + thumbH, 0xCCFFFFFF);
        }

        String close = "✕";
        int closeX = this.detailX + this.detailWidth - 6 - font.width(close);
        graphics.drawString(font, close, closeX, this.detailY + 4, 0xFF6666, false);
    }

    private int calculateDetailHeight(Font font) {
        int h = 4;
        h += 14;
        h += 12;
        if (this.detailEntry.getMaterialLevel() > 0) h += 12;
        h += 6;

        if (this.page == Page.TRAITS) {
            List<String> descs = this.detailEntry.getTraitDescriptions();
            if (!descs.isEmpty()) {
                h += font.lineHeight + 2;
                h += descs.size() * 11;
            }
            h += 4 + 2;
            h += 14;
            return h + 4;
        }

        List<PartData> allPartData = this.detailEntry.getPartData();
        List<PartData> filteredPartData = new ArrayList<>();
        if (!this.activePartFilters.isEmpty()) {
            for (PartData partData : allPartData) {
                if (this.activePartFilters.contains(partData.partType())) {
                    filteredPartData.add(partData);
                }
            }
        } else {
            filteredPartData = allPartData;
        }

        if (!filteredPartData.isEmpty()) {
            for (int i = 0; i < filteredPartData.size(); i++) {
                PartData partData = filteredPartData.get(i);
                h += font.lineHeight + 2;

                if (!partData.traits().isEmpty()) {
                    h += 10;
                    for (PartData.TraitData trait : partData.traits()) {
                        h += 11;
                        if (!trait.descriptions().isEmpty()) {
                            h += 10;
                        }
                    }
                }

                if (!partData.attributeValues().isEmpty()) {
                    h += font.lineHeight + 2;
                    h += partData.attributeValues().size() * 11;
                }

                if (i < filteredPartData.size() - 1) {
                    h += 4;
                }
            }
        }

        return h + 4;
    }

    private int visibleRows() {
        return Math.max(1, (this.listBottom - this.listY) / ROW_HEIGHT);
    }

    private CatalogEntry getEntryAt(double mouseX, double mouseY) {
        if (mouseX < this.contentX || mouseX > this.contentX + this.contentWidth ||
                mouseY < this.listY || mouseY > this.listBottom) return null;
        int idx = this.mainScroll + (int) ((mouseY - this.listY) / ROW_HEIGHT);
        if (idx >= 0 && idx < this.visibleEntries.size()) return this.visibleEntries.get(idx);
        return null;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.totalRows > this.visibleRowCount && this.scrollBarHeight > 0) {
            if (mouseX >= this.scrollBarX && mouseX < this.scrollBarX + 4 &&
                    mouseY >= this.thumbY && mouseY < this.thumbY + this.thumbHeight) {
                this.isDraggingScroll = true;
                this.dragStartY = (int) mouseY;
                this.dragStartScroll = this.mainScroll;
                return true;
            }
            if (mouseX >= this.scrollBarX && mouseX < this.scrollBarX + 4 &&
                    mouseY >= this.scrollBarY && mouseY < this.scrollBarY + this.scrollBarHeight) {
                float ratio = (float) (mouseY - this.scrollBarY) / this.scrollBarHeight;
                int maxScroll = Math.max(0, this.totalRows - this.visibleRowCount);
                this.mainScroll = (int) Math.round(ratio * maxScroll);
                this.mainScroll = clamp(this.mainScroll, 0, maxScroll);
                return true;
            }
        }

        if (this.showFilterPopup) {
            int popupX = this.filterButton.getX();
            int popupY = this.filterButton.getY() + this.filterButton.getHeight() + 2;
            int popupWidth = 160;
            int totalOptions = this.availableFilters.size();
            int visibleRows = Math.min(FILTER_POPUP_MAX_ROWS, totalOptions);
            int popupHeight = visibleRows * 18 + 18;

            if (popupX + popupWidth > this.panelX + this.panelWidth - 4) {
                popupX = this.panelX + this.panelWidth - popupWidth - 4;
            }
            if (popupX < this.panelX + 4) {
                popupX = this.panelX + 4;
            }
            if (popupY + popupHeight > this.panelY + this.panelHeight - 4) {
                popupY = this.panelY + this.panelHeight - popupHeight - 4;
            }
            if (popupY < this.panelY + 4) {
                popupY = this.panelY + 4;
            }

            if (mouseX >= popupX && mouseX < popupX + popupWidth &&
                    mouseY >= popupY && mouseY < popupY + popupHeight) {
                if (mouseY >= popupY + 3 && mouseY < popupY + 16) {
                    int clearX = popupX + popupWidth - 6 - this.font.width(Component.translatable("filter.silentgearcatalog.clear").getString());
                    int selectX = clearX - this.font.width(Component.translatable("filter.silentgearcatalog.select_all").getString()) - 10;
                    if (mouseX >= selectX && mouseX < selectX + this.font.width(Component.translatable("filter.silentgearcatalog.select_all").getString())) {
                        for (AttributeFilter filter : this.availableFilters) {
                            this.activeFilters.add(filter.id());
                        }
                        this.rebuildVisibleEntries();
                        return true;
                    } else if (mouseX >= clearX && mouseX < clearX + this.font.width(Component.translatable("filter.silentgearcatalog.clear").getString())) {
                        this.activeFilters.clear();
                        this.rebuildVisibleEntries();
                        return true;
                    }
                    return true;
                }

                int listStartY = popupY + 18;
                int row = (int) ((mouseY - listStartY) / 18);
                int idx = row + this.filterPopupScroll;
                if (idx >= 0 && idx < totalOptions) {
                    AttributeFilter filter = this.availableFilters.get(idx);
                    if (!this.activeFilters.add(filter.id())) {
                        this.activeFilters.remove(filter.id());
                    }
                    this.rebuildVisibleEntries();
                    return true;
                }
                return true;
            } else {
                this.showFilterPopup = false;
                return true;
            }
        }

        if (this.showPartFilterPopup) {
            int popupX = this.partFilterButton.getX();
            int popupY = this.partFilterButton.getY() + this.partFilterButton.getHeight() + 2;
            int popupWidth = 160;
            int totalOptions = this.partTypeFilters.size();
            int visibleRows = Math.min(PART_FILTER_POPUP_MAX_ROWS, totalOptions);
            int popupHeight = visibleRows * 18 + 18;

            if (popupX + popupWidth > this.panelX + this.panelWidth - 4) {
                popupX = this.panelX + this.panelWidth - popupWidth - 4;
            }
            if (popupX < this.panelX + 4) {
                popupX = this.panelX + 4;
            }
            if (popupY + popupHeight > this.panelY + this.panelHeight - 4) {
                popupY = this.panelY + this.panelHeight - popupHeight - 4;
            }
            if (popupY < this.panelY + 4) {
                popupY = this.panelY + 4;
            }

            if (mouseX >= popupX && mouseX < popupX + popupWidth &&
                    mouseY >= popupY && mouseY < popupY + popupHeight) {
                if (mouseY >= popupY + 3 && mouseY < popupY + 16) {
                    int clearX = popupX + popupWidth - 6 - this.font.width(Component.translatable("filter.silentgearcatalog.clear").getString());
                    int selectX = clearX - this.font.width(Component.translatable("filter.silentgearcatalog.select_all").getString()) - 10;
                    if (mouseX >= selectX && mouseX < selectX + this.font.width(Component.translatable("filter.silentgearcatalog.select_all").getString())) {
                        for (PartTypeFilter filter : this.partTypeFilters) {
                            this.activePartFilters.add(filter.id());
                        }
                        this.rebuildVisibleEntries();
                        return true;
                    } else if (mouseX >= clearX && mouseX < clearX + this.font.width(Component.translatable("filter.silentgearcatalog.clear").getString())) {
                        this.activePartFilters.clear();
                        this.rebuildVisibleEntries();
                        return true;
                    }
                    return true;
                }

                int listStartY = popupY + 18;
                int row = (int) ((mouseY - listStartY) / 18);
                int idx = row + this.partFilterPopupScroll;
                if (idx >= 0 && idx < totalOptions) {
                    PartTypeFilter filter = this.partTypeFilters.get(idx);
                    if (!this.activePartFilters.add(filter.id())) {
                        this.activePartFilters.remove(filter.id());
                    }
                    this.rebuildVisibleEntries();
                    return true;
                }
                return true;
            } else {
                this.showPartFilterPopup = false;
                return true;
            }
        }

        if (this.showSortPopup) {
            int popupX = this.sortButton.getX();
            int popupY = this.sortButton.getY() + this.sortButton.getHeight() + 2;
            int popupWidth = 160;
            int totalOptions = this.availableSorts.size();
            int visibleRows = Math.min(SORT_POPUP_MAX_ROWS, totalOptions);
            int popupHeight = visibleRows * 18 + 18;

            if (popupX + popupWidth > this.panelX + this.panelWidth - 4) {
                popupX = this.panelX + this.panelWidth - popupWidth - 4;
            }
            if (popupX < this.panelX + 4) {
                popupX = this.panelX + 4;
            }
            if (popupY + popupHeight > this.panelY + this.panelHeight - 4) {
                popupY = this.panelY + this.panelHeight - popupHeight - 4;
            }
            if (popupY < this.panelY + 4) {
                popupY = this.panelY + 4;
            }

            if (mouseX >= popupX && mouseX < popupX + popupWidth &&
                    mouseY >= popupY && mouseY < popupY + popupHeight) {
                int listStartY = popupY + 18;
                int row = (int) ((mouseY - listStartY) / 18);
                int idx = row + this.sortPopupScroll;
                if (idx >= 0 && idx < totalOptions) {
                    String key = this.availableSorts.keySet().toArray(new String[0])[idx];
                    this.currentSortKey = key;
                    this.showSortPopup = false;
                    this.rebuildVisibleEntries();
                    return true;
                }
                return true;
            } else {
                this.showSortPopup = false;
                return true;
            }
        }

        if (this.showSortModePopup) {
            int popupX = this.sortModeButton.getX();
            int popupY = this.sortModeButton.getY() + this.sortModeButton.getHeight() + 2;
            int popupWidth = 120;
            int totalOptions = AttributeSort.SortMode.values().length;
            int visibleRows = Math.min(SORT_MODE_POPUP_MAX_ROWS, totalOptions);
            int popupHeight = visibleRows * 18 + 18;

            if (popupX + popupWidth > this.panelX + this.panelWidth - 4) {
                popupX = this.panelX + this.panelWidth - popupWidth - 4;
            }
            if (popupX < this.panelX + 4) {
                popupX = this.panelX + 4;
            }
            if (popupY + popupHeight > this.panelY + this.panelHeight - 4) {
                popupY = this.panelY + this.panelHeight - popupHeight - 4;
            }
            if (popupY < this.panelY + 4) {
                popupY = this.panelY + 4;
            }

            if (mouseX >= popupX && mouseX < popupX + popupWidth &&
                    mouseY >= popupY && mouseY < popupY + popupHeight) {
                int listStartY = popupY + 18;
                int row = (int) ((mouseY - listStartY) / 18);
                int idx = row + this.sortModePopupScroll;
                if (idx >= 0 && idx < totalOptions) {
                    this.selectSortMode(AttributeSort.SortMode.values()[idx]);
                    return true;
                }
                return true;
            } else {
                this.showSortModePopup = false;
                return true;
            }
        }

        if (this.detailEntry != null && this.hoveredTraitName != null && button == 0) {
            int traitX = this.detailX + 6;
            int traitY = this.hoveredTraitY;
            String traitDisplay = "  " + this.hoveredTraitName;
            int traitW = this.font.width(traitDisplay) + 4;
            int traitH = 11;

            if (this.page == Page.TRAITS) {
                traitDisplay = Component.translatable("detail.silentgearcatalog.click_to_view_materials").getString();
                traitW = this.font.width(traitDisplay) + 4;
            }

            if (mouseX >= traitX && mouseX < traitX + traitW &&
                    mouseY >= traitY && mouseY < traitY + traitH) {
                String traitId = this.hoveredTraitId;
                if (traitId != null) {
                    Minecraft.getInstance().setScreen(new TraitMaterialListScreen(
                            this, this.hoveredTraitName, traitId));
                    this.hoveredTraitName = null;
                    this.hoveredTraitId = null;
                    return true;
                }
            }
        }

        if (this.detailEntry != null) {
            int closeX = this.detailX + this.detailWidth - 6 - this.font.width("✕");
            if (mouseX >= closeX && mouseX < closeX + this.font.width("✕") &&
                    mouseY >= this.detailY + 4 && mouseY < this.detailY + 4 + this.font.lineHeight) {
                this.detailEntry = null;
                this.detailLocked = false;
                this.hoveredTraitName = null;
                this.hoveredTraitId = null;
                return true;
            }
        }

        if (this.detailEntry != null &&
                mouseX >= this.detailX && mouseX < this.detailX + this.detailWidth &&
                mouseY >= this.detailY && mouseY < this.detailY + this.detailHeight) {
            this.detailLocked = true;
            return true;
        }

        CatalogEntry clicked = getEntryAt(mouseX, mouseY);
        if (clicked != null) {
            this.detailEntry = clicked;
            this.detailScroll = 0;
            this.detailLocked = true;
            this.hoveredTraitName = null;
            this.hoveredTraitId = null;
            this.detailX = this.panelX + this.panelWidth - this.detailWidth - 4;
            this.detailY = this.listY + 10;
            return true;
        }

        if (this.detailEntry != null && !this.detailLocked) {
            this.detailEntry = null;
            this.hoveredTraitName = null;
            this.hoveredTraitId = null;
        }
        this.detailLocked = false;

        if (this.searchBox != null && this.searchBox.isFocused()) {
            if (mouseX < this.searchBox.getX() || mouseX > this.searchBox.getX() + this.searchBox.getWidth() ||
                    mouseY < this.searchBox.getY() || mouseY > this.searchBox.getY() + this.searchBox.getHeight()) {
                this.searchBox.setFocused(false);
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (this.isDraggingScroll && this.totalRows > this.visibleRowCount && this.scrollTrackHeight > 0) {
            int deltaY = (int) (mouseY - this.dragStartY);
            int maxScroll = Math.max(0, this.totalRows - this.visibleRowCount);
            float ratio = (float) deltaY / this.scrollTrackHeight;
            this.mainScroll = (int) Math.round(this.dragStartScroll + ratio * maxScroll);
            this.mainScroll = clamp(this.mainScroll, 0, maxScroll);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (this.isDraggingScroll) {
            this.isDraggingScroll = false;
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (this.detailEntry != null &&
                mouseX >= this.detailX && mouseX < this.detailX + this.detailWidth &&
                mouseY >= this.detailY && mouseY < this.detailY + this.detailHeight) {
            int maxScroll = Math.max(0, this.detailContentHeight - this.detailHeight + 8);
            this.detailScroll = clamp(this.detailScroll + (int) -scrollY * 10, 0, maxScroll);
            return true;
        }

        if (this.showFilterPopup) {
            int popupX = this.filterButton.getX();
            int popupY = this.filterButton.getY() + this.filterButton.getHeight() + 2;
            int popupWidth = 160;
            int totalOptions = this.availableFilters.size();
            int visibleRows = Math.min(FILTER_POPUP_MAX_ROWS, totalOptions);

            if (mouseX >= popupX && mouseX < popupX + popupWidth &&
                    mouseY >= popupY && mouseY < popupY + visibleRows * 18 + 18) {
                int delta = (int) -scrollY;
                int maxScroll = Math.max(0, totalOptions - visibleRows);
                this.filterPopupScroll = clamp(this.filterPopupScroll + delta, 0, maxScroll);
                return true;
            }
        }

        if (this.showPartFilterPopup) {
            int popupX = this.partFilterButton.getX();
            int popupY = this.partFilterButton.getY() + this.partFilterButton.getHeight() + 2;
            int popupWidth = 160;
            int totalOptions = this.partTypeFilters.size();
            int visibleRows = Math.min(PART_FILTER_POPUP_MAX_ROWS, totalOptions);

            if (mouseX >= popupX && mouseX < popupX + popupWidth &&
                    mouseY >= popupY && mouseY < popupY + visibleRows * 18 + 18) {
                int delta = (int) -scrollY;
                int maxScroll = Math.max(0, totalOptions - visibleRows);
                this.partFilterPopupScroll = clamp(this.partFilterPopupScroll + delta, 0, maxScroll);
                return true;
            }
        }

        if (this.showSortPopup) {
            int popupX = this.sortButton.getX();
            int popupY = this.sortButton.getY() + this.sortButton.getHeight() + 2;
            int popupWidth = 160;
            int totalOptions = this.availableSorts.size();
            int visibleRows = Math.min(SORT_POPUP_MAX_ROWS, totalOptions);

            if (mouseX >= popupX && mouseX < popupX + popupWidth &&
                    mouseY >= popupY && mouseY < popupY + visibleRows * 18 + 18) {
                int delta = (int) -scrollY;
                int maxScroll = Math.max(0, totalOptions - visibleRows);
                this.sortPopupScroll = clamp(this.sortPopupScroll + delta, 0, maxScroll);
                return true;
            }
        }

        if (this.showSortModePopup) {
            int popupX = this.sortModeButton.getX();
            int popupY = this.sortModeButton.getY() + this.sortModeButton.getHeight() + 2;
            int popupWidth = 120;
            int totalOptions = AttributeSort.SortMode.values().length;
            int visibleRows = Math.min(SORT_MODE_POPUP_MAX_ROWS, totalOptions);

            if (mouseX >= popupX && mouseX < popupX + popupWidth &&
                    mouseY >= popupY && mouseY < popupY + visibleRows * 18 + 18) {
                int delta = (int) -scrollY;
                int maxScroll = Math.max(0, totalOptions - visibleRows);
                this.sortModePopupScroll = clamp(this.sortModePopupScroll + delta, 0, maxScroll);
                return true;
            }
        }

        if (mouseX >= this.contentX && mouseX < this.contentX + this.contentWidth &&
                mouseY >= this.listY && mouseY < this.listBottom) {
            int delta = (int) -scrollY;
            int rows = this.visibleRows();
            int maxScroll = Math.max(0, this.visibleEntries.size() - rows);
            this.mainScroll = clamp(this.mainScroll + delta, 0, maxScroll);
            return true;
        }

        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256) {
            if (this.showFilterPopup) { this.showFilterPopup = false; return true; }
            if (this.showPartFilterPopup) { this.showPartFilterPopup = false; return true; }
            if (this.showSortPopup) { this.showSortPopup = false; return true; }
            if (this.showSortModePopup) { this.showSortModePopup = false; return true; }
            if (this.detailEntry != null) {
                this.detailEntry = null;
                this.detailLocked = false;
                this.hoveredTraitName = null;
                this.hoveredTraitId = null;
                return true;
            }
            this.onClose();
            return true;
        }
        if (keyCode == 257 || keyCode == 335) {
            if (this.searchBox != null && this.searchBox.isFocused()) {
                this.searchBox.setFocused(false);
                return true;
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void onClose() {
        STATE.page = this.page;
        STATE.searchQuery = this.searchQuery;
        STATE.sortKey = this.currentSortKey;
        STATE.sortDescending = this.sortDescending;
        STATE.sortMode = this.sortMode;
        STATE.activeFilters = new HashSet<>(this.activeFilters);
        STATE.activePartFilters = new HashSet<>(this.activePartFilters);
        STATE.mainScroll = this.mainScroll;
        STATE.filterPopupScroll = this.filterPopupScroll;
        STATE.partFilterPopupScroll = this.partFilterPopupScroll;
        STATE.sortPopupScroll = this.sortPopupScroll;
        STATE.sortModePopupScroll = this.sortModePopupScroll;
        Minecraft.getInstance().setScreen(this.parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private enum Page {
        MATERIALS,
        TRAITS
    }

    private void updateButtonStates() {
        if (this.filterButton != null) {
            boolean hasFilter = !this.activeFilters.isEmpty();
            MutableComponent text = Component.translatable("button.silentgearcatalog.filter");
            if (hasFilter) {
                text.append(Component.literal("*"));
            }
            this.filterButton.setMessage(text);
        }

        if (this.partFilterButton != null) {
            boolean hasPartFilter = !this.activePartFilters.isEmpty() &&
                    this.activePartFilters.size() < this.partTypeFilters.size();
            MutableComponent text = Component.translatable("button.silentgearcatalog.part");
            if (hasPartFilter) {
                text.append(Component.literal("*"));
            }
            this.partFilterButton.setMessage(text);
        }

        if (this.sortButton != null) {
            boolean hasSort = !this.currentSortKey.equals("_name");
            MutableComponent text = Component.translatable("button.silentgearcatalog.sort");
            if (hasSort) {
                text.append(Component.literal("*"));
            }
            this.sortButton.setMessage(text);
        }
    }
}