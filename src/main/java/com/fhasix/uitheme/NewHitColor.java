package com.fhasix.uitheme;

import net.minecraft.client.MinecraftClient;

/**
 * Hit Color: the tint on a mob (or player) that was just hit. Vanilla paints it red from a small 16 x 16 overlay
 * texture; this re-colours the "hurt" half of that texture. The mixin gives us the live OverlayTexture; nothing is
 * touched while the mod is off (the vanilla red stays exactly as the game made it).
 */
public final class NewHitColor {

    static final String ID = "hit_color";
    private static final int VANILLA = 0xB2FF0000;
    private static final int UNKNOWN = 0x00000001;

    private static NewOverlayHook texture;
    private static int applied = UNKNOWN;

    private NewHitColor() {
    }

    /** Called by the OverlayTexture mixin when the game creates it. */
    public static void attach(Object overlayTexture) {
        if (overlayTexture instanceof NewOverlayHook hook) {
            texture = hook;
            applied = UNKNOWN;
        }
    }

    static void tick(MinecraftClient c) {
        if (texture == null) {
            return;
        }
        int wanted = VANILLA;
        if (NewModSettings.isEnabled(c, ID)) {
            int rgb = NewModSettings.opt(c, ID, "rgb") & 0xFFFFFF;
            int a = Math.max(20, Math.min(100, NewModSettings.opt(c, ID, "alpha"))) * 255 / 100;
            wanted = (a << 24) | rgb;
        }
        if (wanted == applied) {
            return;
        }
        if (applied == UNKNOWN && wanted == VANILLA) {
            applied = VANILLA; // never touched: nothing to restore
            return;
        }
        applied = wanted;
        texture.fhasix$recolor(wanted);
    }
}
