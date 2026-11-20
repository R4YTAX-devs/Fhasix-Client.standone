package com.fhasix.uitheme.mixin;

import com.fhasix.uitheme.NewHitColor;
import com.fhasix.uitheme.NewOverlayHook;
import com.fhasix.uitheme.NewSafe;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Hit Color: gives NewHitColor access to the overlay texture (its top 8 rows are the red "hurt" tint). */
@Mixin(OverlayTexture.class)
public abstract class NewOverlayMixin implements NewOverlayHook {

    @Shadow
    private NativeImageBackedTexture texture;

    @Inject(method = "<init>", at = @At("TAIL"), require = 0)
    private void fhasix$attach(CallbackInfo ci) {
        NewHitColor.attach(this);
    }

    @Override
    public void fhasix$recolor(int argb) {
        try {
            NativeImage image = this.texture.getImage();
            if (image == null) {
                return;
            }
            for (int y = 0; y < 8; y++) {
                for (int x = 0; x < 16; x++) {
                    image.setColorArgb(x, y, argb);
                }
            }
            this.texture.upload();
        } catch (Exception | LinkageError t) {
            NewSafe.failed("hit_color", t);
        }
    }
}
