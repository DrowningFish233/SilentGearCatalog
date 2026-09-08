package com.drowningfish233.silentgearcatalog.client.gui.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

public class CatalogButton extends Button {
    private boolean selected = false;

    public CatalogButton(int x, int y, int width, int height, Component message, OnPress onPress) {
        super(x, y, width, height, message, onPress, DEFAULT_NARRATION);
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        int bgColor, borderColor, textColor;

        if (!this.active) {
            bgColor = 0xFF444444;
            borderColor = 0xFF555555;
            textColor = 0x888888;
        } else if (this.selected) {
            bgColor = 0xFF6600CC;
            borderColor = 0xFFAA44FF;
            textColor = 0xFFFFFF;
        } else if (this.isHoveredOrFocused()) {
            bgColor = 0xFF555555;
            borderColor = 0xFF888888;
            textColor = 0xFFFFFF;
        } else {
            bgColor = 0xFF3A3A3A;
            borderColor = 0xFF444444;
            textColor = 0xCCCCCC;
        }

        graphics.fill(this.getX(), this.getY(), this.getX() + this.width, this.getY() + this.height, bgColor);
        graphics.fill(this.getX(), this.getY(), this.getX() + this.width, this.getY() + 1, borderColor);
        graphics.fill(this.getX(), this.getY(), this.getX() + 1, this.getY() + this.height, borderColor);
        graphics.fill(this.getX() + this.width - 1, this.getY(), this.getX() + this.width, this.getY() + this.height, borderColor);
        graphics.fill(this.getX(), this.getY() + this.height - 1, this.getX() + this.width, this.getY() + this.height, borderColor);

        int textX = this.getX() + this.width / 2;
        int textY = this.getY() + (this.height - 8) / 2;
        graphics.drawCenteredString(Minecraft.getInstance().font, this.getMessage(), textX, textY, textColor);
    }
}