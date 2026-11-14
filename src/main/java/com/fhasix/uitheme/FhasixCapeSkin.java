package com.fhasix.uitheme;

import net.minecraft.entity.player.SkinTextures;
import net.minecraft.util.AssetInfo;
import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.Map;

/**
 * Equipped FHASIX capes are shown by the game's OWN cape renderer: the local player's SkinTextures simply get
 * the equipped cape texture (textures/gui/cosmetics/&lt;id&gt;_cape.png) as their cape. Vanilla then does all the
 * animation (hanging, swinging while you move, sneaking, flapping in the wind) correctly. Used by mixin/skin.
 */
public final class FhasixCapeSkin {
    private static final Map<String, AssetInfo.TextureAsset> CAPES = new HashMap<>();
    private static SkinTextures lastBase;
    private static String lastId = "";
    private static int lastGen = -1;
    private static SkinTextures lastResult;

    private FhasixCapeSkin() {}

    /** The skin with the equipped cape, or null when no FHASIX cape is equipped. */
    public static SkinTextures apply(SkinTextures base) {
        if (base == null) {
            return null;
        }
        String id = FhasixCosmetics.equippedCape();
        if (id.isEmpty()) {
            return null;
        }
        // UPDATE 46: capes that come from the GitHub catalog use their downloaded texture; until it is loaded
        // (or when it was removed / freed) the player simply has no FHASIX cape - never a broken texture
        Identifier remoteTex = null;
        if (!FhasixCatalog.isBuiltin(id)) {
            if (!FhasixCatalog.isRemote(id)) {
                return null;
            }
            remoteTex = FhasixCatalog.capeTexture(id);
            if (remoteTex == null) {
                return null;
            }
        }
        int gen = FhasixCatalog.generation();
        if (base == lastBase && id.equals(lastId) && gen == lastGen && lastResult != null) {
            return lastResult;
        }
        AssetInfo.TextureAsset cape;
        if (remoteTex != null) {
            cape = new AssetInfo.TextureAssetInfo(remoteTex, remoteTex);
        } else {
            cape = CAPES.computeIfAbsent(id, key -> {
                Identifier tex = Identifier.of("fhasixuitheme", "textures/gui/cosmetics/" + key + "_cape.png");
                return new AssetInfo.TextureAssetInfo(tex, tex);
            });
        }
        SkinTextures result = new SkinTextures(base.body(), cape, base.elytra(), base.model(), base.secure());
        lastBase = base;
        lastId = id;
        lastGen = gen;
        lastResult = result;
        return result;
    }
}
