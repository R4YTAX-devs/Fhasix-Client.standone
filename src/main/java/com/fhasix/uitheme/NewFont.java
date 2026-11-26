package com.fhasix.uitheme;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Style;
import net.minecraft.text.StyleSpriteSource;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * The Product Sans font (font id "inter" is just the old internal name), used ONLY by Fhasix's own screens (title screen, Mod Menu, launcher, ...). It is a separate
 * font ("fhasixuitheme:inter"), so the normal game text and the in-game HUD are not touched.
 * Every string is upper-cased and keeps the normal text shadow when the caller asks for it.
 *
 * To resize the font change "size" in assets/fhasixuitheme/font/inter.json (bigger number = bigger text).
 * If text sits a little high or low inside buttons change Y_FIX (in GUI units).
 */
public final class NewFont {
    public static final Identifier ID = Identifier.of("fhasixuitheme", "inter");
    /** Vertical nudge (GUI units) applied where text is centred inside buttons. */
    public static final int Y_FIX = 0;
    /** Text shadow: false = off everywhere this font is used. */
    public static final boolean SHADOW = false;

    private static Style style;
    private static Style titleSansStyle;
    private static Style titleInterStyle;

    /** Vertical nudge (GUI units) for the bigger title-screen text (Y_FIX is for the smaller general text). */
    public static final int Y_TITLE = 1;

    private NewFont() {}

    /** The ONE place that depends on how this Minecraft version names a text font. */
    private static Style style() {
        if (style == null) {
            style = Style.EMPTY.withFont(new StyleSpriteSource.Font(ID));
        }
        return style;
    }

    /** true = every Fhasix screen shows capitals only; false = text keeps the case it was written in. */
    public static final boolean ALL_CAPS = true;

    private static Text make(String s, boolean upper) {
        String v = s == null ? "" : s;
        return Text.literal(upper ? v.toUpperCase(Locale.ROOT) : v).setStyle(style());
    }

    public static Text t(String s) {
        return make(s, ALL_CAPS);
    }

    /** Same font, but the text keeps its own capitalisation (used by the title-screen buttons). */
    public static void drawMixed(DrawContext c, TextRenderer tr, String s, int x, int y, int color, boolean shadow) {
        c.drawText(tr, make(s, false), x, y, color, shadow && SHADOW);
    }

    public static int widthMixed(TextRenderer tr, String s) {
        return tr.getWidth(make(s, false));
    }

    public static Text t(Text text) {
        return t(text == null ? "" : text.getString());
    }

    public static void draw(DrawContext c, TextRenderer tr, String s, int x, int y, int color, boolean shadow) {
        c.drawText(tr, t(s), x, y, color, shadow && SHADOW);
    }

    public static void draw(DrawContext c, TextRenderer tr, Text s, int x, int y, int color, boolean shadow) {
        c.drawText(tr, t(s), x, y, color, shadow && SHADOW);
    }

    public static void draw(DrawContext c, TextRenderer tr, OrderedText s, int x, int y, int color, boolean shadow) {
        c.drawText(tr, s, x, y, color, shadow && SHADOW);
    }

    public static void drawCentered(DrawContext c, TextRenderer tr, String s, int cx, int y, int color) {
        Text tx = t(s);
        c.drawText(tr, tx, cx - tr.getWidth(tx) / 2, y, color, SHADOW);
    }

    public static void drawCentered(DrawContext c, TextRenderer tr, Text s, int cx, int y, int color) {
        drawCentered(c, tr, s == null ? "" : s.getString(), cx, y, color);
    }

    public static void drawCentered(DrawContext c, TextRenderer tr, OrderedText s, int cx, int y, int color) {
        c.drawText(tr, s, cx - tr.getWidth(s) / 2, y, color, SHADOW);
    }

    public static int width(TextRenderer tr, String s) {
        return tr.getWidth(t(s));
    }

    public static int width(TextRenderer tr, Text s) {
        return tr.getWidth(t(s));
    }

    public static int width(TextRenderer tr, OrderedText s) {
        return tr.getWidth(s);
    }

    // ------------------------------------------------------------ title-screen fonts
    // The title buttons use two fonts, like the reference: Product Sans (Singleplayer, Multiplayer, ...) and
    // Inter (STORE, QUIT GAME). Both are a little bigger than the general screen text.

    private static Style titleStyle(boolean inter) {
        if (inter) {
            if (titleInterStyle == null) {
                titleInterStyle = Style.EMPTY.withFont(new StyleSpriteSource.Font(Identifier.of("fhasixuitheme", "title_inter")));
            }
            return titleInterStyle;
        }
        if (titleSansStyle == null) {
            titleSansStyle = Style.EMPTY.withFont(new StyleSpriteSource.Font(Identifier.of("fhasixuitheme", "title_sans")));
        }
        return titleSansStyle;
    }

    private static Text titleText(String s, boolean inter, boolean upper) {
        String v = s == null ? "" : s;
        return Text.literal(upper ? v.toUpperCase(Locale.ROOT) : v).setStyle(titleStyle(inter));
    }

    public static void drawTitle(DrawContext c, TextRenderer tr, String s, int x, int y, int color,
                                 boolean inter, boolean upper) {
        c.drawText(tr, titleText(s, inter, upper), x, y, color, SHADOW);
    }

    public static int widthTitle(TextRenderer tr, String s, boolean inter, boolean upper) {
        return tr.getWidth(titleText(s, inter, upper));
    }


    // ------------------------------------------------------------ fitted title / pill text (update 5.9)
    // The title-screen buttons are sized from the screen, so on small screens or high GUI scales the old fixed font
    // covered the whole button. These helpers keep the exact old look at full size and step the SAME typefaces down
    // (tf_s_NN / tf_i_NN in assets/fhasixuitheme/font) when the button is smaller, then shrink further to fit the width.

    private static final Map<Integer, Style> FIT_STYLES = new HashMap<>();
    public static final float PILL_MAX = 9.1f;

    private static Style fitStyle(boolean inter, float size, float maxSize, Style exact) {
        if (size >= maxSize - 0.01f) {
            return exact;
        }
        int half = Math.max(10, Math.min(23, Math.round(size * 2f)));
        int key = (inter ? 1000 : 0) + half;
        Style st = FIT_STYLES.get(key);
        if (st == null) {
            st = Style.EMPTY.withFont(new StyleSpriteSource.Font(Identifier.of("fhasixuitheme", (inter ? "tf_i_" : "tf_s_") + half)));
            FIT_STYLES.put(key, st);
        }
        return st;
    }

    private static Text fitText(String s, boolean inter, boolean upper, float size, float maxSize, Style exact) {
        String v = s == null ? "" : s;
        return Text.literal(upper ? v.toUpperCase(Locale.ROOT) : v).setStyle(fitStyle(inter, size, maxSize, exact));
    }

    private static float fitSizeFor(TextRenderer tr, String s, boolean inter, boolean upper, float size, float maxSize, Style exact, int maxWidth) {
        float sz = Math.min(size, maxSize);
        while (sz > 5.0f && tr.getWidth(fitText(s, inter, upper, sz, maxSize, exact)) > maxWidth) {
            sz -= 0.5f;
        }
        return sz;
    }

    private static float titleMax(boolean inter) {
        return inter ? 11.0f : 11.6f;
    }

    /** Title-button text size: the old size on a normal button, smaller on a small one, and always narrow enough for maxWidth. */
    public static float titleSize(TextRenderer tr, String s, boolean inter, boolean upper, int buttonH, int maxWidth) {
        float max = titleMax(inter);
        return fitSizeFor(tr, s, inter, upper, Math.min(max, buttonH * 0.33f), max, titleStyle(inter), maxWidth);
    }

    public static void drawTitleSized(DrawContext c, TextRenderer tr, String s, int x, int y, int color, boolean inter, boolean upper, float size) {
        float max = titleMax(inter);
        c.drawText(tr, fitText(s, inter, upper, size, max, titleStyle(inter)), x, y - Math.round((max - size) * 0.35f), color, SHADOW);
    }

    public static int widthTitleSized(TextRenderer tr, String s, boolean inter, boolean upper, float size) {
        return tr.getWidth(fitText(s, inter, upper, size, titleMax(inter), titleStyle(inter)));
    }

    /** Account-pill text size for a pill of height h (the old 9.1 unless the pill is very small). */
    public static float pillSize(int h) {
        return Math.min(PILL_MAX, h * 0.30f);
    }

    public static int widthPill(TextRenderer tr, String s, float size) {
        return tr.getWidth(fitText(s, false, ALL_CAPS, size, PILL_MAX, style()));
    }

    public static void drawPill(DrawContext c, TextRenderer tr, String s, int x, int y, int color, float size) {
        c.drawText(tr, fitText(s, false, ALL_CAPS, size, PILL_MAX, style()), x, y - Math.round((PILL_MAX - size) * 0.35f), color, false);
    }

    // ------------------------------------------------------------ Mod Menu fonts (light weights, normal case)
    // Three weights (regular / medium / bold) at many sizes. The files ui_regular.ttf, ui_medium.ttf and
    // ui_bold.ttf in assets/fhasixuitheme/font/ can simply be replaced with Product Sans Regular / Medium /
    // Bold (same file names) - nothing else has to change.

    public static final int W_REG = 0;
    public static final int W_MED = 1;
    public static final int W_BOLD = 2;

    private static final Map<Integer, Style> UI_STYLES = new HashMap<>();

    private static Style uiStyle(int weight, float size) {
        int half = Math.max(11, Math.min(28, Math.round(size * 2f)));
        int key = weight * 100 + half;
        Style s = UI_STYLES.get(key);
        if (s == null) {
            String w = weight == W_BOLD ? "b" : weight == W_MED ? "m" : "r";
            s = Style.EMPTY.withFont(new StyleSpriteSource.Font(Identifier.of("fhasixuitheme", "ui_" + w + "_" + half)));
            UI_STYLES.put(key, s);
        }
        return s;
    }

    // Every frame the screens ask for the same strings in the same sizes: keep the Text objects and their
    // measured widths instead of building / measuring them again and again (this is a big part of the UI
    // smoothness on phones). The caches are emptied when they grow too large.
    private static final Map<Integer, Map<String, Text>> TEXT_CACHE = new HashMap<>();
    private static final Map<Integer, Map<String, Integer>> WIDTH_CACHE = new HashMap<>();
    private static int cacheEntries = 0;

    private static int styleKey(int weight, float size) {
        return weight * 100 + Math.max(11, Math.min(28, Math.round(size * 2f)));
    }

    private static void trimCaches() {
        if (cacheEntries > 6000) {
            TEXT_CACHE.clear();
            WIDTH_CACHE.clear();
            cacheEntries = 0;
        }
    }

    private static Text uiText(String s, int weight, float size) {
        String v = s == null ? "" : s;
        int key = styleKey(weight, size);
        Map<String, Text> m = TEXT_CACHE.get(key);
        Text t = m == null ? null : m.get(v);
        if (t == null) {
            trimCaches();
            m = TEXT_CACHE.computeIfAbsent(key, k -> new HashMap<>());
            t = Text.literal(v).setStyle(uiStyle(weight, size));
            m.put(v, t);
            cacheEntries++;
        }
        return t;
    }

    public static void drawUi(DrawContext c, TextRenderer tr, String s, int x, int y, int color, int weight, float size) {
        c.drawText(tr, uiText(s, weight, size), x, y, color, false);
    }

    public static int widthUi(TextRenderer tr, String s, int weight, float size) {
        String v = s == null ? "" : s;
        int key = styleKey(weight, size);
        Map<String, Integer> m = WIDTH_CACHE.get(key);
        Integer w = m == null ? null : m.get(v);
        if (w == null) {
            trimCaches();
            m = WIDTH_CACHE.computeIfAbsent(key, k -> new HashMap<>());
            w = tr.getWidth(uiText(v, weight, size));
            m.put(v, w);
            cacheEntries++;
        }
        return w;
    }

    /** y for drawUi so the text is vertically centred in a box (the text baseline sits 7 units below y). */
    public static int centerYUi(int boxY, int boxH, float size) {
        return Math.round(boxY + boxH / 2f - 7f + 0.35f * Math.max(5.5f, Math.min(14f, size)));
    }

    /** Cuts text with ".." so it fits maxW. */
    public static String fitUi(TextRenderer tr, String s, int weight, float size, int maxW) {
        if (widthUi(tr, s, weight, size) <= maxW) {
            return s;
        }
        String cut = s;
        while (cut.length() > 1 && widthUi(tr, cut + "..", weight, size) > maxW) {
            cut = cut.substring(0, cut.length() - 1);
        }
        return cut + "..";
    }
}
