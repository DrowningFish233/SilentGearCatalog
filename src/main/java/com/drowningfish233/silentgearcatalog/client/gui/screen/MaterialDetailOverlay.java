package com.drowningfish233.silentgearcatalog.client.gui.screen;

import com.drowningfish233.silentgearcatalog.client.gui.catalog.CatalogEntry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Map;

public class MaterialDetailOverlay {
    private static final int PANEL_WIDTH = 240;
    private static final int PANEL_MAX_HEIGHT = 300;
    private static final int PADDING = 8;
    private static final int LINE_HEIGHT = 11;
    private static final int TITLE_HEIGHT = 14;
    private static final int SECTION_GAP = 6;

    private CatalogEntry targetEntry;
    private int scrollOffset = 0;
    private int contentHeight = 0;
    private int panelX, panelY, panelWidth, panelHeight;
    private boolean visible = false;

    public void show(CatalogEntry entry, int x, int y, int maxHeight) {
        this.targetEntry = entry;
        this.visible = true;
        this.scrollOffset = 0;
        this.panelX = x;
        this.panelY = y;
        this.panelWidth = PANEL_WIDTH;
        this.panelHeight = Math.min(maxHeight, PANEL_MAX_HEIGHT);
        this.contentHeight = calculateContentHeight();
    }

    public void hide() {
        this.visible = false;
        this.targetEntry = null;
    }

    public boolean isVisible() {
        return visible;
    }

    public boolean isHovered(double mouseX, double mouseY) {
        return visible &&
                mouseX >= panelX && mouseX < panelX + panelWidth &&
                mouseY >= panelY && mouseY < panelY + panelHeight;
    }

    public void scroll(int delta) {
        int maxScroll = Math.max(0, contentHeight - panelHeight + PADDING * 2);
        scrollOffset = Math.max(0, Math.min(maxScroll, scrollOffset + delta * 10));
    }

    private int calculateContentHeight() {
        if (targetEntry == null) return 0;

        Font font = Minecraft.getInstance().font;
        int height = PADDING;

        // 标题
        height += TITLE_HEIGHT + 2;

        // 特性
        List<String> traits = targetEntry.getTraitNames();
        if (!traits.isEmpty()) {
            height += SECTION_GAP;
            height += font.lineHeight + 2;
            for (String trait : traits) {
                height += LINE_HEIGHT;
            }
        }

        // 属性
        Map<String, Double> attrs = targetEntry.getAttributeValues();
        if (!attrs.isEmpty()) {
            height += SECTION_GAP;
            height += font.lineHeight + 2;
            height += attrs.size() * LINE_HEIGHT;
        }

        return height + PADDING;
    }

    public void render(GuiGraphics graphics, int mouseX, int mouseY) {
        if (!visible || targetEntry == null) return;

        Font font = Minecraft.getInstance().font;

        // 面板背景
        graphics.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight, 0xCC222222);
        graphics.fill(panelX + 1, panelY + 1, panelX + panelWidth - 1, panelY + panelHeight - 1, 0xFF333333);
        graphics.fill(panelX, panelY, panelX + panelWidth, panelY + 1, 0xFF444444);
        graphics.fill(panelX, panelY, panelX + 1, panelY + panelHeight, 0xFF444444);

        // 剪裁
        int clipX = panelX + 2;
        int clipY = panelY + 2;
        int clipW = panelWidth - 4;
        int clipH = panelHeight - 4;
        graphics.enableScissor(clipX, clipY, clipX + clipW, clipY + clipH);

        int y = panelY + PADDING - scrollOffset;

        // 材料名称
        String name = targetEntry.getName();
        graphics.drawString(font, Component.literal(name).withStyle(style -> style.withBold(true)),
                panelX + PADDING, y, 0xFFFFFF, false);
        y += TITLE_HEIGHT + 2;

        // ID
        Component idLabel = Component.translatable("catalog.silentgearcatalog.id");
        graphics.drawString(font, "§7" + idLabel.getString() + ": " + targetEntry.getId(), panelX + PADDING, y, 0x888888, false);
        y += LINE_HEIGHT + SECTION_GAP;

        // 特性
        List<String> traits = targetEntry.getTraitNames();
        if (!traits.isEmpty()) {
            Component traitLabel = Component.translatable("detail.silentgearcatalog.traits");
            graphics.drawString(font, "§6" + traitLabel.getString() + ":", panelX + PADDING, y, 0xFFAA00, false);
            y += font.lineHeight + 2;
            for (String trait : traits) {
                graphics.drawString(font, "  §f" + trait, panelX + PADDING, y, 0xCCCCCC, false);
                y += LINE_HEIGHT;
            }
            y += SECTION_GAP;
        }

        // 属性
        Map<String, Double> attrs = targetEntry.getAttributeValues();
        if (!attrs.isEmpty()) {
            Component attrLabel = Component.translatable("detail.silentgearcatalog.attributes");
            graphics.drawString(font, "§b" + attrLabel.getString() + ":", panelX + PADDING, y, 0x00BBFF, false);
            y += font.lineHeight + 2;
            for (Map.Entry<String, Double> entry : attrs.entrySet()) {
                String key = getPropertyDisplayName(entry.getKey());
                String value = formatValue(entry.getValue());
                graphics.drawString(font, "  §f" + key + ": §e" + value,
                        panelX + PADDING, y, 0xCCCCCC, false);
                y += LINE_HEIGHT;
            }
        }

        graphics.disableScissor();

        // 滚动条
        int maxScroll = Math.max(0, contentHeight - panelHeight + PADDING * 2);
        if (maxScroll > 0) {
            int trackH = panelHeight - 8;
            int thumbH = Math.max(12, trackH * panelHeight / (panelHeight + maxScroll));
            int thumbY = panelY + 4 + (trackH - thumbH) * scrollOffset / maxScroll;
            graphics.fill(panelX + panelWidth - 4, panelY + 4,
                    panelX + panelWidth - 2, panelY + panelHeight - 4, 0x44FFFFFF);
            graphics.fill(panelX + panelWidth - 4, thumbY,
                    panelX + panelWidth - 2, thumbY + thumbH, 0xCCFFFFFF);
        }
    }

    private String getPropertyDisplayName(String key) {
        String rawKey = key;
        if (rawKey.contains(":")) {
            rawKey = rawKey.substring(rawKey.indexOf(":") + 1);
        }
        String translationKey = "property.silentgear." + rawKey;
        return Component.translatable(translationKey).getString();
    }

    private String formatValue(double value) {
        if (value == (long) value) {
            return String.valueOf((long) value);
        }
        return String.format("%.2f", value);
    }
}