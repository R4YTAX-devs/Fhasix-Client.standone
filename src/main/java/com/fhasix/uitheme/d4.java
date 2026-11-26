package com.fhasix.uitheme;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

/**
 * The "Quit Game" button (update 5.9.9): minimal - a faint red tint and a thin red outline instead of the old dark box,
 * that fade stronger on hover and back out on mouse-leave. It is still the one button that animates its hover.
 */
public class d4 extends ButtonWidget {

    private static final long FADE_MS = 300L;

    /** update 5.9.9: flat and minimal - no filled box and no shadow, just a thin outline in the same red. */
    private static final int RED_RGB = 0x00E13B3E;
    private static final int RAD = 4;

    private static final int TEXT_COLOR = 0xFFE13B3E;

    private float hoverProgress = 0f;
    private long lastFrameMs = -1L;

    public d4(int x, int y, int width, int height, net.minecraft.text.Text message, PressAction onPress) {
        super(x, y, width, height, message, onPress, DEFAULT_NARRATION_SUPPLIER);
    }

    @Override
    public void drawIcon(DrawContext context, int mouseX, int mouseY, float delta) {
        long now = System.currentTimeMillis();
        if (lastFrameMs < 0) lastFrameMs = now;
        long dtMs = now - lastFrameMs;
        lastFrameMs = now;

        float target = this.isHovered() ? 1f : 0f;
        float step = dtMs / (float) FADE_MS;
        if (hoverProgress < target) hoverProgress = Math.min(target, hoverProgress + step);
        else if (hoverProgress > target) hoverProgress = Math.max(target, hoverProgress - step);

        int x = this.getX(), y = this.getY(), w = this.getWidth(), h = this.getHeight();
        int fillA = Math.round(10f + 34f * hoverProgress);
        int ringA = Math.round(95f + 120f * hoverProgress);
        int fill = (fillA << 24) | RED_RGB;
        NewUi.rrect(context, x, y, w, h, RAD, fill, fill);
        NewUi.ring(context, x, y, w, h, RAD, (ringA << 24) | RED_RGB);

        // The label is drawn here (ButtonWidget.drawIcon draws it normally, but we override it without super).
        TextRenderer tr = MinecraftClient.getInstance().textRenderer;
        String label = this.getMessage().getString();
        float size = NewFont.titleSize(tr, label, true, true, h, Math.max(8, w - Math.round(w * 0.12f)));
        NewFont.drawTitleSized(context, tr, label, x + (w - NewFont.widthTitleSized(tr, label, true, true, size)) / 2,
                y + (h - tr.fontHeight) / 2 + NewFont.Y_TITLE, TEXT_COLOR, true, true, size);
    }

}
