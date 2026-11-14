package com.fhasix.loader;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

/** Flat red/black button matching the FHASIX theme. */
public class RedButton extends ButtonWidget {

    public RedButton(int x, int y, int width, int height, net.minecraft.text.Text message, PressAction onPress) {
        super(x, y, width, height, message, onPress, DEFAULT_NARRATION_SUPPLIER);
    }

    @Override
    public void drawIcon(DrawContext context, int mouseX, int mouseY, float delta) {
        int x = this.getX(), y = this.getY(), w = this.getWidth(), h = this.getHeight();
        boolean hover = this.isHovered() && this.active;

        int border = this.active ? 0xFFB3262B : 0xFF3A3A3E;
        int base = this.active ? (hover ? 0xFF8E1D21 : 0xFF1A0E0F) : 0xFF141416;
        context.fill(x, y, x + w, y + h, border);
        context.fill(x + 1, y + 1, x + w - 1, y + h - 1, base);

        TextRenderer tr = MinecraftClient.getInstance().textRenderer;
        net.minecraft.text.Text label = this.getMessage();
        int color = this.active ? 0xFFFFFFFF : 0xFF77777C;
        context.drawText(tr, label, x + (w - tr.getWidth(label)) / 2, y + (h - tr.fontHeight) / 2 + 1, color, true);
    }
}
