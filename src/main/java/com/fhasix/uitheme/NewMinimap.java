package com.fhasix.uitheme;

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Heightmap;

/**
 * Minimap: a small top-down map of the ground around you (north is up), with you in the middle, other players as red
 * dots and your waypoints as coloured diamonds. The map is rescanned a few times a second into a tiny colour grid and
 * drawn with plain filled squares, so it costs almost nothing per frame.
 */
public final class NewMinimap {

    static final String ID = "minimap";
    private static final int[] GRID = {20, 28, 40};

    private static int[] colors = new int[0];
    private static int gridN = 0;
    private static int zoomBlocks = 0;
    private static long lastScan = 0L;
    private static int scanX = Integer.MIN_VALUE;
    private static int scanZ = Integer.MIN_VALUE;

    private NewMinimap() {
    }

    static void init() {
        HudElementRegistry.attachElementAfter(
                VanillaHudElements.MISC_OVERLAYS,
                Identifier.of("fhasixuitheme", "minimap_hud"),
                NewSafe.hud("minimap", (context, tickCounter) -> render(context)));
    }

    private static void scan(MinecraftClient c, int n, int zoom) {
        long now = System.currentTimeMillis();
        int px = c.player.getBlockX();
        int pz = c.player.getBlockZ();
        boolean sameSpot = px == scanX && pz == scanZ;
        if (colors.length == n * n && n == gridN && zoom == zoomBlocks && (now - lastScan < 250L || (sameSpot && now - lastScan < 1500L))) {
            return;
        }
        lastScan = now;
        scanX = px;
        scanZ = pz;
        gridN = n;
        zoomBlocks = zoom;
        if (colors.length != n * n) {
            colors = new int[n * n];
        }
        int[] heights = new int[n * n];
        int half = n / 2;
        BlockPos.Mutable pos = new BlockPos.Mutable();
        for (int gz = 0; gz < n; gz++) {
            for (int gx = 0; gx < n; gx++) {
                int wx = px + (gx - half) * zoom;
                int wz = pz + (gz - half) * zoom;
                int top = c.world.getTopY(Heightmap.Type.WORLD_SURFACE, wx, wz) - 1;
                int rgb = 0x0A0B0D;
                if (top > c.world.getBottomY()) {
                    pos.set(wx, top, wz);
                    BlockState st = c.world.getBlockState(pos);
                    rgb = st.getMapColor(c.world, pos).color;
                }
                heights[gz * n + gx] = top;
                colors[gz * n + gx] = rgb;
            }
        }
        // relief: a cell is a little brighter when it is higher than the cell to its north-west, darker when lower
        for (int gz = 1; gz < n; gz++) {
            for (int gx = 1; gx < n; gx++) {
                int dh = heights[gz * n + gx] - heights[(gz - 1) * n + (gx - 1)];
                float f = Math.max(0.72f, Math.min(1.25f, 1f + dh * 0.07f));
                int rgb = colors[gz * n + gx];
                int r = Math.min(255, Math.round(((rgb >> 16) & 255) * f));
                int g = Math.min(255, Math.round(((rgb >> 8) & 255) * f));
                int b = Math.min(255, Math.round((rgb & 255) * f));
                colors[gz * n + gx] = (r << 16) | (g << 8) | b;
            }
        }
    }

    private static void render(DrawContext g) {
        MinecraftClient c = MinecraftClient.getInstance();
        if (c.world == null || c.player == null || c.currentScreen != null || c.options.hudHidden
                || !NewModSettings.isEnabled(c, ID)) {
            return;
        }
        int n = GRID[Math.max(0, Math.min(2, NewModSettings.opt(c, ID, "detail")))];
        int zoom = Math.max(1, Math.min(4, NewModSettings.opt(c, ID, "zoom")));
        int size = Math.max(64, Math.min(180, NewModSettings.opt(c, ID, "size")));
        int cell = Math.max(2, size / n);
        size = cell * n;
        scan(c, n, zoom);

        int margin = 8;
        int sw = g.getScaledWindowWidth();
        int sh = g.getScaledWindowHeight();
        int pos = NewModSettings.opt(c, ID, "pos");
        int x = (pos == 1 || pos == 3 || pos == 5) ? sw - size - margin : margin;
        int y = pos <= 1 ? margin + 22 : (pos <= 3 ? sh - size - margin - 30 : (sh - size) / 2);

        NewUi.frame(g, x - 3, y - 3, size + 6, size + 6, 4, NewUi.CARD_BORDER, NewUi.PANEL_T, NewUi.PANEL_B);
        for (int gz = 0; gz < n; gz++) {
            for (int gx = 0; gx < n; gx++) {
                g.fill(x + gx * cell, y + gz * cell, x + (gx + 1) * cell, y + (gz + 1) * cell,
                        0xFF000000 | colors[gz * n + gx]);
            }
        }
        double span = n * (double) zoom;          // blocks across the map
        double ppb = size / span;                 // pixels per block
        double ex = c.player.getX();
        double ez = c.player.getZ();

        if (NewModSettings.optBool(c, ID, "waypoints")) {
            for (NewWaypoints.Wp w : NewWaypoints.here(c)) {
                int mx = (int) Math.round(size / 2.0 + (w.x + 0.5 - ex) * ppb);
                int mz = (int) Math.round(size / 2.0 + (w.z + 0.5 - ez) * ppb);
                mx = Math.max(3, Math.min(size - 3, mx));
                mz = Math.max(3, Math.min(size - 3, mz));
                NewWaypoints.diamond(g, x + mx, y + mz, 3, 0xFF000000 | w.rgb);
            }
        }
        if (NewModSettings.optBool(c, ID, "players")) {
            for (PlayerEntity other : c.world.getPlayers()) {
                if (other == c.player) {
                    continue;
                }
                int mx = (int) Math.round(size / 2.0 + (other.getX() - ex) * ppb);
                int mz = (int) Math.round(size / 2.0 + (other.getZ() - ez) * ppb);
                if (mx >= 2 && mz >= 2 && mx < size - 2 && mz < size - 2) {
                    g.fill(x + mx - 1, y + mz - 1, x + mx + 2, y + mz + 2, 0xFFE73C40);
                }
            }
        }
        // you: a small arrow pointing the way you look
        double yaw = Math.toRadians(c.player.getYaw());
        double dx = -Math.sin(yaw);
        double dz = Math.cos(yaw);
        int cx = x + size / 2;
        int cz = y + size / 2;
        g.fill(cx - 2, cz - 2, cx + 3, cz + 3, 0xFFFFFFFF);
        g.fill(cx - 1, cz - 1, cx + 2, cz + 2, 0xFF1B1D22);
        for (int i = 3; i <= 7; i++) {
            int ax = cx + (int) Math.round(dx * i);
            int az = cz + (int) Math.round(dz * i);
            g.fill(ax, az, ax + 1, az + 1, 0xFFFFFFFF);
        }
        NewFont.drawUi(g, c.textRenderer, "N", x + size / 2 - 3, y + 2, 0xFFFFFFFF, NewFont.W_BOLD, 8f);
    }
}
