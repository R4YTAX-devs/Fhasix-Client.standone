package com.fhasix.uitheme;

import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Util;
import org.lwjgl.glfw.GLFW;

/**
 * The "Join Discord" button action: opens the invite in the browser and also copies it to the clipboard, so it can be
 * pasted on a device that cannot open a browser from the game (phones).
 */
public final class NewDiscordLink {
    public static final String INVITE = "https://discord.com/invite/KdxZAmxnmv";

    private NewDiscordLink() {
    }

    public static void open(MinecraftClient c) {
        try {
            GLFW.glfwSetClipboardString(c.getWindow().getHandle(), INVITE);
        } catch (Throwable ignored) {
            // no clipboard - the browser still opens
        }
        try {
            Util.getOperatingSystem().open(INVITE);
        } catch (Throwable ignored) {
            // no browser on this device - the link is on the clipboard
        }
    }
}
