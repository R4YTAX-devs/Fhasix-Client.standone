package com.fhasix.uitheme;

import net.minecraft.util.Identifier;

/** Implemented by the GameRenderer mixin: reads / sets the post-effect slot the game renders after the world. */
public interface NewPostControl {
    Identifier fhasix$getEffectId();

    void fhasix$setEffectId(Identifier id);

    void fhasix$setEffectActive(boolean active);
}
