package com.fhasix.uitheme.mixin;

import com.fhasix.uitheme.NewPostControl;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

/** Gives Motion Blur access to the game's post-effect slot (id + on/off). */
@Mixin(GameRenderer.class)
public abstract class NewPostControlMixin implements NewPostControl {

    @Shadow
    private Identifier postProcessorId;

    @Shadow
    private boolean postProcessorEnabled;

    @Override
    public Identifier fhasix$getEffectId() {
        return this.postProcessorId;
    }

    @Override
    public void fhasix$setEffectId(Identifier id) {
        this.postProcessorId = id;
    }

    @Override
    public void fhasix$setEffectActive(boolean active) {
        this.postProcessorEnabled = active;
    }
}
