package com.fhasix.uitheme.mixin;

import com.fhasix.uitheme.NewSafe;
import com.fhasix.uitheme.NewTweaks;
import net.minecraft.client.gui.hud.InGameOverlayRenderer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Low Fire: moves the fire you see when you burn down, so it stops covering the screen. */
@Mixin(InGameOverlayRenderer.class)
public abstract class NewLowFireMixin {

    @Inject(method = "renderFireOverlay", at = @At("HEAD"), require = 0)
    private static void fhasix$lowFire(MatrixStack matrices, VertexConsumerProvider vertexConsumers, Sprite sprite, CallbackInfo ci) {
        try {
            float drop = NewTweaks.fireDrop();
            if (drop > 0f) {
                matrices.translate(0.0F, -drop, 0.0F);
            }
        } catch (Exception | LinkageError t) {
            NewSafe.failed("low_fire", t);
        }
    }
}
