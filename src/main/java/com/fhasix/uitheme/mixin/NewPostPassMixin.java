package com.fhasix.uitheme.mixin;

import com.fhasix.uitheme.NewMotionBlur;
import com.fhasix.uitheme.NewSafe;
import com.mojang.blaze3d.buffers.GpuBuffer;
import java.util.Map;
import net.minecraft.client.gl.PostEffectPass;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Motion Blur: refreshes the blend factor (a small uniform buffer) right before our pass is drawn. */
@Mixin(PostEffectPass.class)
public abstract class NewPostPassMixin {

    @Shadow
    @Final
    private Map<String, GpuBuffer> uniformBuffers;

    @Unique
    private GpuBuffer fhasix$own;

    @Unique
    private float fhasix$ownBlend = -1.0f;

    @Inject(method = "render", at = @At("HEAD"), require = 0)
    private void fhasix$refreshBlend(CallbackInfo ci) {
        try {
            GpuBuffer current = this.uniformBuffers.get(NewMotionBlur.UNIFORM);
            if (current == null) {
                return;
            }
            float blend = NewMotionBlur.nextBlend();
            if (current == this.fhasix$own) {
                if (blend != this.fhasix$ownBlend && NewMotionBlur.writeBlend(current, blend)) {
                    this.fhasix$ownBlend = blend;
                }
                return;
            }
            GpuBuffer replacement = NewMotionBlur.createBlend(blend);
            if (replacement == null) {
                return;
            }
            this.uniformBuffers.put(NewMotionBlur.UNIFORM, replacement);
            this.fhasix$own = replacement;
            this.fhasix$ownBlend = blend;
            current.close();
        } catch (Exception | LinkageError t) {
            NewSafe.failed("motion_blur", t);
        }
    }
}
