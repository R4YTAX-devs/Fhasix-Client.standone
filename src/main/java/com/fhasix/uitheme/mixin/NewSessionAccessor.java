package com.fhasix.uitheme.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.session.Session;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Lets the Account Switcher replace the game's session (the logged-in name). */
@Mixin(MinecraftClient.class)
public interface NewSessionAccessor {

    @Mutable
    @Accessor("session")
    void fhasix$setSession(Session session);
}
