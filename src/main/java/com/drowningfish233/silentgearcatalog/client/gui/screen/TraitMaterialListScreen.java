package com.drowningfish233.silentgearcatalog.client.gui.screen;

import com.drowningfish233.silentgearcatalog.client.gui.catalog.CatalogDataBuilder;
import com.drowningfish233.silentgearcatalog.client.gui.catalog.CatalogEntry;
import com.drowningfish233.silentgearcatalog.client.gui.catalog.CatalogSnapshot;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class TraitMaterialListScreen extends Screen {
    private static final int WINDOW_WIDTH = 360;
    private static final int WINDOW_HEIGHT = 400;
    private static final int ROW_HEIGHT = 28;

    private final Screen parent;
    private final String traitName;
    private final String traitId;
    private List<CatalogEntry> materials = new ArrayList<>();
    private int scrollOffset = 0;
    private int maxScroll = 0;

    private int panelX, panelY, panelWidth, panelHeight;
    private int listY, listBottom;
    private List<String> traitDescriptions = new ArrayList<>();

    public TraitMaterialListScreen(Screen parent, String traitName, String traitId) {
        super(Component.translatable("trait.silentgearcatalog.materials_with", traitName));
        this.parent = parent;
        this.traitName = traitName;
        this.traitId = traitId;
        this.traitDescriptions = fetchTraitDescriptions(traitId);
    }

    private List<String> fetchTraitDescriptions(String traitId) {
        CatalogSnapshot snapshot = CatalogDataBuilder.build();
        if (!snapshot.fullyLoaded()) return List.of();
        for (CatalogEntry entry : snapshot.traits()) {
            if (entry.getId().equals(traitId)) {
                return entry.getTraitDescriptions();
            }
        }
        return List.of();
    }

    @Override
    protected void init() {
        super.init();
        this.panelWidth = Math.min(WINDOW_WIDTH, this.width - 20);
        this.panelHeight = Math.min(WINDOW_HEIGHT, this.height - 20);
        this.panelX = (this.width - this.panelWidth) / 2;
        this.panelY = (this.height - this.panelHeight) / 2;
        this.listY = this.panelY + 30;
        this.listBottom = this.panelY + this.panelHeight - 10;

        CatalogSnapshot snapshot = CatalogDataBuilder.build();
        if (snapshot.fullyLoaded()) {
            this.materials = snapshot.materials().stream()
                    .filter(e -> e.getTraitIds().contains(this.traitId))
                    .collect(Collectors.toList());
        }
        this.maxScroll = Math.max(0, this.materials.size() - this.visibleRows());
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {}

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(this.panelX, this.panelY, this.panelX + this.panelWidth, this.panelY + this.panelHeight, 0xFF222222);
        graphics.fill(this.panelX + 1, this.panelY + 1, this.panelX + this.panelWidth - 1, this.panelY + this.panelHeight - 1, 0xFF333333);
        graphics.fill(this.panelX, this.panelY, this.panelX + this.panelWidth, this.panelY + 1, 0xFF444444);
        graphics.fill(this.panelX, this.panelY, this.panelX + 1, this.panelY + this.panelHeight, 0xFF444444);

        // 标题
        Component title = Component.translatable("trait.silentgearcatalog.materials_with", this.traitName);
        graphics.drawString(this.font, title, this.panelX + 8, this.panelY + 8, 0xFFFFFF, false);

        // 显示特性描述
        int descY = this.panelY + 8;
        if (!this.traitDescriptions.isEmpty()) {
            descY += 14;
            for (String desc : this.traitDescriptions) {
                String wrapped = this.font.plainSubstrByWidth("§7" + desc, this.panelWidth - 16);
                graphics.drawString(this.font, wrapped, this.panelX + 8, descY, 0x888888, false);
                descY += 10;
            }
            descY += 4;
        }

        Component count = Component.translatable("trait.silentgearcatalog.material_count", this.materials.size());
        graphics.drawString(this.font, count, this.panelX + this.panelWidth - 8 - this.font.width(count.getString()),
                this.panelY + 8, 0x888888, false);

        graphics.fill(this.panelX + 4, descY + 4, this.panelX + this.panelWidth - 4, descY + 5, 0xFF444444);
        this.listY = descY + 10;
        this.listBottom = this.panelY + this.panelHeight - 10;

        renderList(graphics, mouseX, mouseY);

        for (var widget : this.children()) {
            if (widget instanceof net.minecraft.client.gui.components.Renderable renderable) {
                renderable.render(graphics, mouseX, mouseY, partialTick);
            }
        }
    }

    private void renderList(GuiGraphics graphics, int mouseX, int mouseY) {
        if (this.materials.isEmpty()) {
            graphics.drawString(this.font, Component.translatable("trait.silentgearcatalog.no_materials"),
                    this.panelX + 8, this.listY + 20, 0x888888, false);
            return;
        }

        int rows = this.visibleRows();
        int end = Math.min(this.materials.size(), this.scrollOffset + rows);

        for (int i = this.scrollOffset; i < end; i++) {
            CatalogEntry entry = this.materials.get(i);
            int rowY = this.listY + (i - this.scrollOffset) * ROW_HEIGHT;

            boolean hovered = mouseX >= this.panelX + 4 && mouseX < this.panelX + this.panelWidth - 4 &&
                    mouseY >= rowY && mouseY < rowY + ROW_HEIGHT;

            graphics.fill(this.panelX + 4, rowY, this.panelX + this.panelWidth - 4, rowY + ROW_HEIGHT,
                    hovered ? 0x44FFFFFF : (i % 2 == 0 ? 0x22FFFFFF : 0x11FFFFFF));
            graphics.fill(this.panelX + 4, rowY + ROW_HEIGHT - 1,
                    this.panelX + this.panelWidth - 4, rowY + ROW_HEIGHT, 0x22FFFFFF);

            ItemStack stack = entry.getDisplayStack();
            if (!stack.isEmpty()) {
                graphics.renderItem(stack, this.panelX + 8, rowY + 5);
            }

            String name = this.font.plainSubstrByWidth(entry.getName(), this.panelWidth - 80);
            int color = hovered ? 0xFFFFAA : 0xFFFFFF;
            graphics.drawString(this.font, name, this.panelX + 30, rowY + 9, color, false);

            if (entry.getMaterialLevel() > 0) {
                String level = "Lv." + entry.getMaterialLevel();
                graphics.drawString(this.font, level, this.panelX + this.panelWidth - 8 - this.font.width(level),
                        rowY + 9, 0xFFAA00, false);
            }
        }

        if (this.maxScroll > 0) {
            int trackHeight = this.listBottom - this.listY - 2;
            int thumbHeight = Math.max(12, trackHeight * rows / this.materials.size());
            int thumbY = this.listY + 2 + (trackHeight - thumbHeight) * this.scrollOffset / this.maxScroll;
            graphics.fill(this.panelX + this.panelWidth - 6, this.listY + 2,
                    this.panelX + this.panelWidth - 4, this.listBottom - 2, 0x44FFFFFF);
            graphics.fill(this.panelX + this.panelWidth - 6, thumbY,
                    this.panelX + this.panelWidth - 4, thumbY + thumbHeight, 0xCCFFFFFF);
        }
    }

    private int visibleRows() {
        return Math.max(1, (this.listBottom - this.listY - 4) / ROW_HEIGHT);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (this.maxScroll > 0) {
            int delta = (int) -scrollY;
            this.scrollOffset = clamp(this.scrollOffset + delta, 0, this.maxScroll);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (mouseX < this.panelX || mouseX > this.panelX + this.panelWidth ||
                mouseY < this.panelY || mouseY > this.panelY + this.panelHeight) {
            this.onClose();
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256) {
            this.onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(this.parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}