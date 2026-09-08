package com.drowningfish233.silentgearcatalog.client.gui.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import com.drowningfish233.silentgearcatalog.SilentGearCatalog;

public final class CatalogSkin {
    public static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(SilentGearCatalog.MODID, "textures/gui/catalog_atlas.png");

    public static final int WINDOW = 0;
    public static final int SIDEBAR = 1;
    public static final int HEADER = 2;
    public static final int POPUP = 3;
    public static final int BUTTON = 4;
    public static final int BUTTON_HOVER = 5;
    public static final int BUTTON_SELECTED = 6;
    public static final int BUTTON_DISABLED = 7;
    public static final int ROW = 8;
    public static final int ROW_HOVER = 9;
    public static final int SLOT = 10;
    public static final int SLOT_HOVER = 11;
    public static final int INPUT = 12;
    public static final int INPUT_FOCUSED = 13;
    public static final int SCROLL_TRACK = 14;
    public static final int SCROLL_THUMB = 15;

    public static final int ACCENT = 0xFF6600CC;

    private static final int ATLAS_SIZE = 256;
    private static final int CELL_SIZE = 64;
    private static final int SOURCE_BORDER = 10;
    private static final int BORDER = 3;

    private CatalogSkin() {}

    public static void draw(GuiGraphics graphics, int sprite, int x, int y, int width, int height) {
        if (width <= 0 || height <= 0) return;

        if (sprite == SCROLL_TRACK || sprite == SCROLL_THUMB) {
            blit(graphics, x, y, width, height,
                    sprite % 4 * CELL_SIZE, sprite / 4 * CELL_SIZE, CELL_SIZE, CELL_SIZE);
            return;
        }

        paint(graphics, sprite, x, y, width, height, true);
    }

    public static void frame(GuiGraphics graphics, int sprite, int x, int y, int width, int height) {
        paint(graphics, sprite, x, y, width, height, false);
    }

    private static void paint(GuiGraphics graphics, int sprite, int x, int y, int width, int height, boolean center) {
        if (width <= 0 || height <= 0) return;

        int borderX = Math.min(BORDER, width / 2);
        int borderY = Math.min(BORDER, height / 2);

        int u = sprite % 4 * CELL_SIZE;
        int v = sprite / 4 * CELL_SIZE;

        for (int row = 0; row < 3; row++) {
            int top = row == 0 ? 0 : (row == 1 ? borderY : height - borderY);
            int drawHeight = row == 1 ? height - borderY * 2 : borderY;
            int sourceY = row == 0 ? 0 : (row == 1 ? SOURCE_BORDER : CELL_SIZE - SOURCE_BORDER);
            int sourceHeight = row == 1 ? CELL_SIZE - SOURCE_BORDER * 2 : SOURCE_BORDER;

            for (int col = 0; col < 3; col++) {
                if (!center && row == 1 && col == 1) continue;

                int left = col == 0 ? 0 : (col == 1 ? borderX : width - borderX);
                int drawWidth = col == 1 ? width - borderX * 2 : borderX;
                int sourceX = col == 0 ? 0 : (col == 1 ? SOURCE_BORDER : CELL_SIZE - SOURCE_BORDER);
                int sourceWidth = col == 1 ? CELL_SIZE - SOURCE_BORDER * 2 : SOURCE_BORDER;

                if (drawWidth > 0 && drawHeight > 0) {
                    blit(graphics, x + left, y + top, drawWidth, drawHeight,
                            u + sourceX, v + sourceY, sourceWidth, sourceHeight);
                }
            }
        }
    }

    private static void blit(GuiGraphics graphics, int x, int y, int width, int height,
                             int u, int v, int sourceWidth, int sourceHeight) {
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        graphics.blit(TEXTURE, x, y, width, height,
                (float) u, (float) v, sourceWidth, sourceHeight, ATLAS_SIZE, ATLAS_SIZE);
    }
}