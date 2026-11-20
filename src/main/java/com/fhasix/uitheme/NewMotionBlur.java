package com.fhasix.uitheme;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.systems.RenderSystem;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Identifier;

/**
 * Motion Blur (1.21.11).
 *
 * How it works: the game has one "post effect" slot that is rendered after the world and before the HUD, so the HUD
 * stays sharp. This mod puts its own effect there (post_effect/motion_blur.json). The effect keeps the previous frame
 * in a persistent buffer and blends it with the new one: movement leaves a short smooth trail.
 *
 * Why it stays smooth on any device: the blend factor is worked out every frame from the real frame time, so the
 * trail lasts the same number of milliseconds at 30 FPS and at 240 FPS (no ghosting at low FPS, no flicker at high
 * FPS). After a pause, a menu or a lag spike the trail restarts cleanly instead of smearing. It is one extra
 * full-screen pass; it does not touch the world renderer.
 *
 * The way a changing value is fed to the shader (a small uniform buffer replaced per frame) follows the MIT-licensed
 * Motionsuite mod by r3vy.
 */
public final class NewMotionBlur {

    static final String ID = "motion_blur";
    /** Name of the uniform block in post_effect/motion_blur.json and the shader. */
    public static final String UNIFORM = "FhasixBlur";

    private static final Identifier DYNAMIC = Identifier.of("fhasixuitheme", "motion_blur");
    private static final Identifier[] PRESET = {
            Identifier.of("fhasixuitheme", "motion_blur_low"),
            Identifier.of("fhasixuitheme", "motion_blur_mid"),
            Identifier.of("fhasixuitheme", "motion_blur_high")
    };
    private static final int BLOCK_SIZE = 16;
    private static final int USAGE_MAP_WRITE_UNIFORM = 0x02 | 0x80;

    private static boolean active = false;
    private static boolean seed = true;
    private static long lastNs = 0L;
    private static int dynamicHits = 0;
    private static int ticksActive = 0;
    private static boolean useStatic = false;

    private NewMotionBlur() {
    }

    static void tick(MinecraftClient c) {
        if (!(c.gameRenderer instanceof NewPostControl ctl)) {
            return;
        }
        Identifier cur = ctl.fhasix$getEffectId();
        boolean want = c.world != null && c.player != null && NewModSettings.isEnabled(c, ID);
        if (want) {
            if (cur != null && !ours(cur)) {
                return; // the game (spectating a mob, for example) is using the slot
            }
            ticksActive++;
            if (!useStatic && ticksActive == 120 && dynamicHits == 0) {
                useStatic = true; // the per-frame blend could not be attached: use the fixed-strength versions
            }
            Identifier target = useStatic ? PRESET[presetIndex(c)] : DYNAMIC;
            if (!target.equals(cur)) {
                ctl.fhasix$setEffectId(target);
                seed = true;
            }
            ctl.fhasix$setEffectActive(true);
            active = true;
        } else if (active) {
            if (cur != null && ours(cur)) {
                ctl.fhasix$setEffectActive(false);
                ctl.fhasix$setEffectId(null);
            }
            active = false;
            ticksActive = 0;
            seed = true;
        }
    }

    private static boolean ours(Identifier id) {
        if (DYNAMIC.equals(id)) {
            return true;
        }
        for (Identifier p : PRESET) {
            if (p.equals(id)) {
                return true;
            }
        }
        return false;
    }

    private static int presetIndex(MinecraftClient c) {
        int s = NewModSettings.opt(c, ID, "strength");
        return s < 40 ? 0 : (s < 65 ? 1 : 2);
    }

    /** The blend factor for this frame (0 = no blur: show the new frame only). Called by the post-pass mixin. */
    public static float nextBlend() {
        dynamicHits++;
        MinecraftClient c = MinecraftClient.getInstance();
        long now = System.nanoTime();
        double dt = lastNs == 0L ? 0.0 : (now - lastNs) / 1.0E9;
        lastNs = now;
        if (seed || dt <= 0.0 || dt > 0.25) {
            seed = false; // first frame, or after a pause / lag spike: start the trail again
            return 0f;
        }
        double strength = Math.max(10, Math.min(90, NewModSettings.opt(c, ID, "strength"))) / 100.0;
        // strength describes the blend per frame at 60 FPS; other frame rates get the same trail length in time
        double blend = Math.pow(strength, Math.min(dt, 0.1) * 60.0);
        if (NewModSettings.optBool(c, ID, "lowfps") && dt > 1.0 / 40.0) {
            blend = Math.min(blend, 0.4); // under 40 FPS a long trail only looks like lag
        }
        return Math.min(Math.round(blend * 64.0) / 64.0f, 0.95f);
    }

    public static GpuBuffer createBlend(float blend) {
        try {
            ByteBuffer data = ByteBuffer.allocateDirect(BLOCK_SIZE).order(ByteOrder.nativeOrder());
            data.putFloat(0, blend);
            return RenderSystem.getDevice().createBuffer(() -> "fhasix motion blur", USAGE_MAP_WRITE_UNIFORM, data);
        } catch (RuntimeException e) {
            NewSafe.failed("motion_blur", e);
            return null;
        }
    }

    public static boolean writeBlend(GpuBuffer buffer, float blend) {
        try {
            var view = RenderSystem.getDevice().createCommandEncoder().mapBuffer(buffer, false, true);
            try {
                view.data().putFloat(0, blend);
            } finally {
                view.close();
            }
            return true;
        } catch (RuntimeException e) {
            NewSafe.failed("motion_blur", e);
            return false;
        }
    }
}
