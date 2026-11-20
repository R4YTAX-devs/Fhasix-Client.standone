package com.fhasix.uitheme;

/** Implemented by the OverlayTexture mixin so Hit Color can re-colour the hurt tint at any time. */
public interface NewOverlayHook {
    void fhasix$recolor(int argb);
}
