package com.fhasix.uitheme;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;

/**
 * Account Switcher (update 5.9.9 redesign): a "Playing as" card, the saved offline accounts as clean rows
 * (letter avatar, Use / Remove) and a themed name field. Everything is drawn in code with the same navy buttons,
 * corner-lit edge and main colour as the title screen. Opened from the name at the top right.
 */
public final class NewAccountsScreen extends Screen {

    private static final int DW = 1394;
    private static final int DH = 847;
    private static final int PW = 760;
    private static final int PH = 664;
    private static final int ROWS = 4;
    private static final int ROW_STEP = 72;
    private static final int ROW_H = 64;
    private static final int FIELD = NewUi.rgb(21, 23, 28);
    private static final int FIELD_HOV = NewUi.rgb(27, 30, 36);
    private static final int ROW_BG = NewUi.rgb(22, 24, 29);
    private static final int ROW_HOV = NewUi.rgb(29, 32, 39);
    private static final int GREEN = NewUi.rgb(62, 200, 82);
    private static final int ERR = NewUi.rgb(226, 92, 92);
    private static final int[] AVATAR = {
            NewUi.rgb(64, 92, 150), NewUi.rgb(150, 70, 80), NewUi.rgb(60, 125, 90),
            NewUi.rgb(140, 100, 50), NewUi.rgb(110, 75, 150), NewUi.rgb(55, 120, 130)
    };

    private final Screen parent;
    private float k;
    private int ox;
    private int oy;
    private int bx;
    private int by;
    private int page = 0;
    private String note = "";
    private boolean noteBad = true;
    private int[] panel;
    private int[] closeBtn;
    private int[] cardR;
    private int[] prevBtn;
    private int[] nextBtn;
    private int[] fieldR;
    private int[] addBtn;
    private TextFieldWidget nameField;

    public NewAccountsScreen(Screen parent) {
        super(Text.literal("Accounts"));
        this.parent = parent;
    }

    /** Text size in GUI units: the design size times the panel scale. */
    private float fs(float design) {
        return design * k;
    }

    private int sx(int v) {
        return ox + Math.round(v * k);
    }

    private int sy(int v) {
        return oy + Math.round(v * k);
    }

    private int sl(int v) {
        return Math.max(1, Math.round(v * k));
    }

    private int[] rect(int x, int y, int w, int h) {
        int x0 = sx(x);
        int y0 = sy(y);
        return new int[]{x0, y0, sx(x + w) - x0, sy(y + h) - y0};
    }

    @Override
    protected void init() {
        k = NewUi.scale(width, height);
        ox = (width - Math.round(DW * k)) / 2;
        oy = (height - Math.round(DH * k)) / 2;
        bx = (DW - PW) / 2;
        by = (DH - PH) / 2;
        panel = rect(bx, by, PW, PH);
        closeBtn = rect(bx + PW - 36 - 120, by + 26, 120, 44);
        cardR = rect(bx + 36, by + 100, PW - 72, 92);
        nextBtn = rect(bx + PW - 36 - 36, by + 212, 36, 34);
        prevBtn = rect(bx + PW - 36 - 36 - 8 - 36, by + 212, 36, 34);
        fieldR = rect(bx + 36, by + 566, 470, 56);
        addBtn = rect(bx + 522, by + 566, PW - 36 - 522, 56);
        String keep = nameField != null ? nameField.getText() : "";
        // typing and focus only - this screen draws the text and the caret itself (same as the Discover search)
        nameField = new TextFieldWidget(textRenderer, fieldR[0], fieldR[1], Math.max(10, fieldR[2]), Math.max(10, fieldR[3]), Text.literal("Name"));
        nameField.setMaxLength(16);
        nameField.setDrawsBackground(false);
        nameField.setText(keep);
        addDrawableChild(nameField);
    }

    private int[] rowRect(int i) {
        return rect(bx + 36, by + 256 + i * ROW_STEP, PW - 72, ROW_H);
    }

    private int[] useRect(int i) {
        int[] r = rowRect(i);
        int w = sl(112);
        int h = sl(40);
        return new int[]{r[0] + r[2] - sl(12) - w * 2 - sl(10), r[1] + (r[3] - h) / 2, w, h};
    }

    private int[] delRect(int i) {
        int[] r = rowRect(i);
        int w = sl(112);
        int h = sl(40);
        return new int[]{r[0] + r[2] - sl(12) - w, r[1] + (r[3] - h) / 2, w, h};
    }

    /** The saved accounts without the one that is in use (that one has its own card). */
    private List<String> others() {
        String cur = client.getSession().getUsername();
        List<String> out = new ArrayList<>();
        for (String n : NewAccounts.names(client)) {
            if (!n.equals(cur)) {
                out.add(n);
            }
        }
        return out;
    }

    private int pagesFor(int count) {
        return Math.max(1, (count + ROWS - 1) / ROWS);
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        // own background in render()
    }

    private void label(DrawContext c, String s, int x, int boxY, int boxH, int color, int weight, float size) {
        NewFont.drawUi(c, textRenderer, s, x, NewFont.centerYUi(boxY, boxH, size), color, weight, size);
    }

    private void labelCenter(DrawContext c, int[] r, String s, int color, float size) {
        int tw = NewFont.widthUi(textRenderer, s, NewFont.W_BOLD, size);
        NewFont.drawUi(c, textRenderer, s, r[0] + (r[2] - tw) / 2, NewFont.centerYUi(r[1], r[3], size), color, NewFont.W_BOLD, size);
    }

    /** style 0 = navy (title-screen button), 1 = main colour, 2 = ghost red outline. */
    private void button(DrawContext c, int[] r, String text, int style, boolean enabled, int mx, int my) {
        boolean h = enabled && NewUi.in(mx, my, r[0], r[1], r[2], r[3]);
        int rad = Math.max(3, sl(10));
        int tc = NewUi.TXT;
        if (style == 1) {
            NewUi.rrect(c, r[0], r[1], r[2], r[3], rad, h ? NewUi.redHT() : NewUi.redT(), h ? NewUi.redHB() : NewUi.redB());
        } else if (style == 2) {
            if (h) {
                int f = NewUi.withAlpha(FhasixTheme.red(), 0.16f);
                NewUi.rrect(c, r[0], r[1], r[2], r[3], rad, f, f);
            }
            NewUi.ring(c, r[0], r[1], r[2], r[3], rad, NewUi.withAlpha(ERR, h ? 0.95f : 0.55f));
            tc = ERR;
        } else {
            NewUi.rrect(c, r[0], r[1], r[2], r[3], rad, h ? NewUi.HOV_T : NewUi.BTN_T, h ? NewUi.HOV_B : NewUi.BTN_B);
            NewButtonArt.edge(c, r[0], r[1], r[2], r[3], rad, 0.20f + (h ? 0.08f : 0f));
            if (!enabled) {
                tc = NewUi.TXT_DIM;
            }
        }
        labelCenter(c, r, text, tc, fs(12.5f));
    }

    private void avatar(DrawContext c, String name, int x, int y, int s) {
        int col = AVATAR[Math.floorMod(name.hashCode(), AVATAR.length)];
        NewUi.rrect(c, x, y, s, s, s / 2, col, col);
        String ch = name.isEmpty() ? "?" : name.substring(0, 1).toUpperCase();
        float sz = Math.max(6f, s * 0.44f);
        int tw = NewFont.widthUi(textRenderer, ch, NewFont.W_BOLD, sz);
        NewFont.drawUi(c, textRenderer, ch, x + (s - tw) / 2, NewFont.centerYUi(y, s, sz), 0xFFFFFFFF, NewFont.W_BOLD, sz);
    }

    @Override
    public void render(DrawContext c, int mx, int my, float delta) {
        c.fill(0, 0, width, height, 0xB0000000);
        int rad16 = Math.max(3, sl(16));
        NewUi.frame(c, panel[0], panel[1], panel[2], panel[3], rad16, NewUi.CARD_BORDER, NewUi.PANEL_T, NewUi.PANEL_B);
        NewButtonArt.edge(c, panel[0], panel[1], panel[2], panel[3], rad16, 0.28f);

        int left = sx(bx + 36);
        label(c, "Accounts", left, sy(by + 24), sl(30), NewUi.TXT, NewFont.W_BOLD, fs(21f));
        label(c, "Offline accounts - singleplayer and offline-mode servers only.", left, sy(by + 56), sl(24), NewUi.TXT_DIM, NewFont.W_MED, fs(11f));
        button(c, closeBtn, "Close", 0, true, mx, my);

        // "Playing as" card
        String current = client.getSession().getUsername();
        int rad12 = Math.max(3, sl(12));
        NewUi.rrect(c, cardR[0], cardR[1], cardR[2], cardR[3], rad12, NewUi.rgb(20, 22, 27), NewUi.rgb(17, 19, 23));
        NewButtonArt.edge(c, cardR[0], cardR[1], cardR[2], cardR[3], rad12, 0.24f);
        int as = sl(56);
        avatar(c, current, cardR[0] + sl(20), cardR[1] + (cardR[3] - as) / 2, as);
        int cx = cardR[0] + sl(20) + as + sl(18);
        label(c, "PLAYING AS", cx, cardR[1] + sl(14), sl(22), NewUi.TXT_DIM, NewFont.W_BOLD, fs(10f));
        label(c, current, cx, cardR[1] + sl(34), sl(40), NewUi.TXT, NewFont.W_BOLD, fs(22f));
        int pw = sl(104);
        int ph = sl(34);
        int[] pill = {cardR[0] + cardR[2] - sl(20) - pw, cardR[1] + (cardR[3] - ph) / 2, pw, ph};
        int pf = NewUi.withAlpha(GREEN, 0.14f);
        NewUi.rrect(c, pill[0], pill[1], pill[2], pill[3], pill[3] / 2, pf, pf);
        NewUi.ring(c, pill[0], pill[1], pill[2], pill[3], pill[3] / 2, NewUi.withAlpha(GREEN, 0.5f));
        labelCenter(c, pill, "IN USE", GREEN, fs(11f));

        // saved accounts
        List<String> list = others();
        int pages = pagesFor(list.size());
        page = Math.max(0, Math.min(page, pages - 1));
        label(c, "SAVED ACCOUNTS" + (list.isEmpty() ? "" : "  (" + list.size() + ")"), left, sy(by + 212), sl(34), NewUi.TXT_DIM, NewFont.W_BOLD, fs(11f));
        if (pages > 1) {
            button(c, prevBtn, "<", 0, page > 0, mx, my);
            button(c, nextBtn, ">", 0, page < pages - 1, mx, my);
            String pg = (page + 1) + " / " + pages;
            NewFont.drawUi(c, textRenderer, pg, prevBtn[0] - sl(14) - NewFont.widthUi(textRenderer, pg, NewFont.W_MED, fs(12f)),
                    NewFont.centerYUi(prevBtn[1], prevBtn[3], fs(12f)), NewUi.TXT_DIM, NewFont.W_MED, fs(12f));
        }
        int rad10 = Math.max(3, sl(10));
        if (list.isEmpty()) {
            int[] e = rect(bx + 36, by + 256, PW - 72, ROW_H * 2);
            NewUi.ring(c, e[0], e[1], e[2], e[3], rad12, NewUi.withAlpha(0xFF8C8F96, 0.25f));
            String a = "No other accounts yet";
            String b = "Add a name below to switch between accounts.";
            float s1 = fs(15f);
            float s2 = fs(11.5f);
            NewFont.drawUi(c, textRenderer, a, e[0] + (e[2] - NewFont.widthUi(textRenderer, a, NewFont.W_BOLD, s1)) / 2,
                    NewFont.centerYUi(e[1] + e[3] / 2 - sl(26), sl(26), s1), NewUi.TXT, NewFont.W_BOLD, s1);
            NewFont.drawUi(c, textRenderer, b, e[0] + (e[2] - NewFont.widthUi(textRenderer, b, NewFont.W_MED, s2)) / 2,
                    NewFont.centerYUi(e[1] + e[3] / 2 + 2, sl(26), s2), NewUi.TXT_DIM, NewFont.W_MED, s2);
        }
        for (int i = 0; i < ROWS; i++) {
            int idx = page * ROWS + i;
            if (idx >= list.size()) {
                break;
            }
            String n = list.get(idx);
            int[] r = rowRect(i);
            boolean rh = NewUi.in(mx, my, r[0], r[1], r[2], r[3]);
            int bg = rh ? ROW_HOV : ROW_BG;
            NewUi.rrect(c, r[0], r[1], r[2], r[3], rad10, bg, bg);
            NewButtonArt.edge(c, r[0], r[1], r[2], r[3], rad10, 0.16f + (rh ? 0.06f : 0f));
            int s = sl(40);
            avatar(c, n, r[0] + sl(14), r[1] + (r[3] - s) / 2, s);
            int nx = r[0] + sl(14) + s + sl(16);
            int maxW = useRect(i)[0] - nx - sl(12);
            String shown = NewFont.fitUi(textRenderer, n, NewFont.W_BOLD, fs(15f), Math.max(10, maxW));
            label(c, shown, nx, r[1], r[3], NewUi.TXT, NewFont.W_BOLD, fs(15f));
            button(c, useRect(i), "Use", 0, true, mx, my);
            button(c, delRect(i), "Remove", 2, true, mx, my);
        }

        // name field + add
        boolean focused = nameField != null && nameField.isFocused();
        boolean fh = focused || NewUi.in(mx, my, fieldR[0], fieldR[1], fieldR[2], fieldR[3]);
        int ff = fh ? FIELD_HOV : FIELD;
        NewUi.rrect(c, fieldR[0], fieldR[1], fieldR[2], fieldR[3], rad10, ff, ff);
        if (focused) {
            NewUi.ring(c, fieldR[0], fieldR[1], fieldR[2], fieldR[3], rad10, NewUi.withAlpha(FhasixTheme.red(), 0.85f));
        } else {
            NewButtonArt.edge(c, fieldR[0], fieldR[1], fieldR[2], fieldR[3], rad10, 0.16f);
        }
        float fsz = fs(14f);
        int tx = fieldR[0] + sl(18);
        int ty = NewFont.centerYUi(fieldR[1], fieldR[3], fsz);
        String q = nameField.getText();
        int caretX = tx;
        if (q.isEmpty()) {
            NewFont.drawUi(c, textRenderer, "New offline name", tx, ty, NewUi.TXT_DIM, NewFont.W_BOLD, fsz);
        } else {
            NewFont.drawUi(c, textRenderer, q, tx, ty, NewUi.TXT, NewFont.W_MED, fsz);
            caretX = tx + NewFont.widthUi(textRenderer, q, NewFont.W_MED, fsz);
        }
        if (focused && (System.currentTimeMillis() / 500L) % 2L == 0L) {
            int chh = Math.max(6, Math.round(fieldR[3] * 0.5f));
            int cy = fieldR[1] + (fieldR[3] - chh) / 2;
            c.fill(caretX + 1, cy, caretX + 1 + Math.max(1, sl(2)), cy + chh, NewUi.TXT);
        }
        String cnt = q.length() + "/16";
        NewFont.drawUi(c, textRenderer, cnt, fieldR[0] + fieldR[2] - sl(16) - NewFont.widthUi(textRenderer, cnt, NewFont.W_MED, fs(10.5f)),
                NewFont.centerYUi(fieldR[1], fieldR[3], fs(10.5f)), NewUi.TXT_DIM, NewFont.W_MED, fs(10.5f));
        button(c, addBtn, "Add account", 1, true, mx, my);

        if (!note.isEmpty()) {
            label(c, note, left, sy(by + 630), sl(24), noteBad ? ERR : GREEN, NewFont.W_BOLD, fs(11.5f));
        }
    }

    private void addName() {
        String n = nameField.getText();
        if (NewAccounts.add(client, n)) {
            note = "Added " + n.trim() + ".";
            noteBad = false;
            nameField.setText("");
        } else {
            noteBad = true;
            note = "Use 3 to 16 letters, numbers or _ (and not a name that is already in the list).";
        }
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        if (click.buttonInfo().button() == 0) {
            double x = click.x();
            double y = click.y();
            List<String> list = others();
            int pages = pagesFor(list.size());
            for (int i = 0; i < ROWS; i++) {
                int idx = page * ROWS + i;
                if (idx >= list.size()) {
                    break;
                }
                int[] u = useRect(i);
                int[] d = delRect(i);
                if (NewUi.in(x, y, u[0], u[1], u[2], u[3])) {
                    NewUi.click(client);
                    if (NewAccounts.use(client, list.get(idx))) {
                        client.setScreen(parent);
                    } else {
                        note = "Could not switch to that account on this version.";
                        noteBad = true;
                    }
                    return true;
                }
                if (NewUi.in(x, y, d[0], d[1], d[2], d[3])) {
                    NewUi.click(client);
                    NewAccounts.remove(client, list.get(idx));
                    note = "";
                    return true;
                }
            }
            if (NewUi.in(x, y, addBtn[0], addBtn[1], addBtn[2], addBtn[3])) {
                NewUi.click(client);
                addName();
                return true;
            }
            if (pages > 1 && NewUi.in(x, y, prevBtn[0], prevBtn[1], prevBtn[2], prevBtn[3])) {
                NewUi.click(client);
                page = Math.max(0, page - 1);
                return true;
            }
            if (pages > 1 && NewUi.in(x, y, nextBtn[0], nextBtn[1], nextBtn[2], nextBtn[3])) {
                NewUi.click(client);
                page = Math.min(pages - 1, page + 1);
                return true;
            }
            if (NewUi.in(x, y, closeBtn[0], closeBtn[1], closeBtn[2], closeBtn[3])) {
                NewUi.click(client);
                close();
                return true;
            }
            if (NewUi.in(x, y, fieldR[0], fieldR[1], fieldR[2], fieldR[3])) {
                this.setFocused(nameField);
                super.mouseClicked(click, doubled);
                return true;
            }
            this.setFocused(null);
        }
        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean keyPressed(KeyInput keyEvent) {
        if (keyEvent.key() == 257 && nameField.isFocused()) {
            addName();
            return true;
        }
        return super.keyPressed(keyEvent);
    }

    @Override
    public void close() {
        client.setScreen(parent);
    }
}
