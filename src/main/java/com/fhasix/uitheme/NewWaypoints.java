package com.fhasix.uitheme;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

/**
 * Waypoints: named places you save per server / world and dimension. They show in the world as a small marker with
 * the name and the distance, and on the Minimap. Keys: B = save a waypoint here, N = open the list.
 * Saved in fhasix_waypoints.txt (one line per waypoint) next to the other Fhasix settings.
 */
public final class NewWaypoints {

    static final String ID = "waypoints";
    static final int[] COLORS = {0xE73C40, 0x3FBF4D, 0x4DA3FF, 0xF2B632, 0xB45CFF, 0x2EE6D6, 0xFF7A3D, 0xFFFFFF};

    static final class Wp {
        String name;
        final String world;
        final String dim;
        int x;
        int y;
        int z;
        int rgb;

        Wp(String name, String world, String dim, int x, int y, int z, int rgb) {
            this.name = name;
            this.world = world;
            this.dim = dim;
            this.x = x;
            this.y = y;
            this.z = z;
            this.rgb = rgb;
        }
    }

    private static final NewProjection PROJ = new NewProjection();
    private static final List<Wp> ALL = new ArrayList<>();
    private static boolean loaded = false;
    private static KeyBinding addKey;
    private static KeyBinding listKey;

    private NewWaypoints() {
    }

    static void init(KeyBinding.Category category) {
        addKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.fhasixuitheme.waypoint_add", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_B, category));
        listKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.fhasixuitheme.waypoint_list", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_N, category));
        ClientTickEvents.END_CLIENT_TICK.register(NewSafe.tick("waypoints", NewWaypoints::tick));
        HudElementRegistry.attachElementAfter(
                VanillaHudElements.MISC_OVERLAYS,
                Identifier.of("fhasixuitheme", "waypoints_hud"),
                NewSafe.hud("waypoints", (context, tickCounter) -> render(context, tickCounter.getTickProgress(false))));
    }

    private static void tick(MinecraftClient c) {
        while (addKey != null && addKey.wasPressed()) {
            if (c.player != null && c.currentScreen == null && NewModSettings.isEnabled(c, ID)) {
                add(c, "");
            }
        }
        while (listKey != null && listKey.wasPressed()) {
            if (c.player != null && c.currentScreen == null && NewModSettings.isEnabled(c, ID)) {
                c.setScreen(new NewWaypointsScreen());
            }
        }
    }

    // ---------------------------------------------------------------- data

    private static Path file(MinecraftClient c) {
        return c.runDirectory.toPath().resolve("fhasix_waypoints.txt");
    }

    private static void ensureLoaded(MinecraftClient c) {
        if (loaded) {
            return;
        }
        loaded = true;
        try {
            Path f = file(c);
            if (!Files.exists(f)) {
                return;
            }
            for (String line : Files.readAllLines(f, StandardCharsets.UTF_8)) {
                String[] p = line.split("\t", 7);
                if (p.length == 7) {
                    ALL.add(new Wp(p[6], p[0], p[1], Integer.parseInt(p[2]), Integer.parseInt(p[3]),
                            Integer.parseInt(p[4]), Integer.parseInt(p[5])));
                }
            }
        } catch (IOException | RuntimeException ignored) {
            // a damaged file must never break the game
        }
    }

    private static void save(MinecraftClient c) {
        try {
            List<String> lines = new ArrayList<>();
            for (Wp w : ALL) {
                lines.add(w.world + "\t" + w.dim + "\t" + w.x + "\t" + w.y + "\t" + w.z + "\t" + w.rgb + "\t"
                        + w.name.replace('\t', ' ').replace('\n', ' '));
            }
            Files.write(file(c), lines, StandardCharsets.UTF_8);
        } catch (IOException ignored) {
            // not being able to save must never break anything
        }
    }

    /** The key of the current server / world, so waypoints of different servers do not mix. */
    static String worldKey(MinecraftClient c) {
        net.minecraft.client.network.ServerInfo si = c.getCurrentServerEntry();
        if (si != null) {
            return si.address;
        }
        return c.isInSingleplayer() ? "singleplayer" : "other";
    }

    static String dimension(MinecraftClient c) {
        return c.world == null ? "" : c.world.getRegistryKey().getValue().toString();
    }

    /** Waypoints of the current world and dimension. */
    static List<Wp> here(MinecraftClient c) {
        ensureLoaded(c);
        List<Wp> out = new ArrayList<>();
        if (c.world == null) {
            return out;
        }
        String wk = worldKey(c);
        String dim = dimension(c);
        for (Wp w : ALL) {
            if (w.world.equals(wk) && w.dim.equals(dim)) {
                out.add(w);
            }
        }
        return out;
    }

    static void add(MinecraftClient c, String name) {
        ensureLoaded(c);
        if (c.player == null || c.world == null) {
            return;
        }
        List<Wp> mine = here(c);
        String n = name == null || name.isBlank() ? "Waypoint " + (mine.size() + 1) : name.trim();
        if (n.length() > 24) {
            n = n.substring(0, 24);
        }
        ALL.add(new Wp(n, worldKey(c), dimension(c), c.player.getBlockX(), c.player.getBlockY(), c.player.getBlockZ(),
                COLORS[mine.size() % COLORS.length]));
        save(c);
    }

    static void remove(MinecraftClient c, Wp w) {
        ALL.remove(w);
        save(c);
    }

    // ---------------------------------------------------------------- in-world markers

    private static void render(DrawContext g, float tickProgress) {
        MinecraftClient c = MinecraftClient.getInstance();
        if (c.world == null || c.player == null || c.currentScreen != null || !NewModSettings.isEnabled(c, ID)
                || !NewModSettings.optBool(c, ID, "markers") || !PROJ.begin(c, g, tickProgress)) {
            return;
        }
        double range = NewModSettings.opt(c, ID, "range");
        for (Wp w : here(c)) {
            double wx = w.x + 0.5;
            double wy = w.y + 1.0;
            double wz = w.z + 0.5;
            double d = Math.sqrt(PROJ.distanceSq(wx, wy, wz));
            if (d > range || d < 2.0) {
                continue;
            }
            int[] pt = PROJ.project(wx, wy, wz);
            if (pt == null) {
                continue;
            }
            int color = 0xFF000000 | w.rgb;
            diamond(g, pt[0], pt[1], 5, color);
            String label = w.name + "  " + Math.round(d) + " m";
            int tw = NewFont.widthUi(c.textRenderer, label, NewFont.W_MED, 9f);
            int tx = pt[0] - tw / 2;
            int ty = pt[1] - 18;
            g.fill(tx - 3, ty - 2, tx + tw + 3, ty + 11, 0x99000000);
            NewFont.drawUi(g, c.textRenderer, label, tx, ty, 0xFFFFFFFF, NewFont.W_MED, 9f);
        }
    }

    static void diamond(DrawContext g, int cx, int cy, int r, int argb) {
        for (int dy = -r; dy <= r; dy++) {
            int half = r - Math.abs(dy);
            g.fill(cx - half, cy + dy, cx + half + 1, cy + dy + 1, argb);
        }
    }
}
