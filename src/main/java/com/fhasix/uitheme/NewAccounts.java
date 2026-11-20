package com.fhasix.uitheme;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.session.Session;

/**
 * Account Switcher: a list of offline names you can switch between on the title screen. An offline name works in
 * singleplayer and on offline-mode servers (the same way an offline launcher account does). Servers that check your
 * real Minecraft account will not accept it: use the real account from your launcher there.
 */
public final class NewAccounts {

    private static final List<String> NAMES = new ArrayList<>();
    private static boolean loaded = false;

    private NewAccounts() {
    }

    private static Path file(MinecraftClient c) {
        return c.runDirectory.toPath().resolve("fhasix_accounts.txt");
    }

    static List<String> names(MinecraftClient c) {
        if (!loaded) {
            loaded = true;
            try {
                if (Files.exists(file(c))) {
                    for (String l : Files.readAllLines(file(c), StandardCharsets.UTF_8)) {
                        if (valid(l.trim()) && !NAMES.contains(l.trim())) {
                            NAMES.add(l.trim());
                        }
                    }
                }
            } catch (IOException ignored) {
                // an unreadable list is just an empty list
            }
            String cur = c.getSession().getUsername();
            if (!NAMES.contains(cur) && valid(cur)) {
                NAMES.add(0, cur);
            }
        }
        return NAMES;
    }

    static boolean valid(String n) {
        return n.length() >= 3 && n.length() <= 16 && n.matches("[A-Za-z0-9_]+");
    }

    private static void save(MinecraftClient c) {
        try {
            Files.write(file(c), NAMES, StandardCharsets.UTF_8);
        } catch (IOException ignored) {
            // not being able to save must never break anything
        }
    }

    static boolean add(MinecraftClient c, String name) {
        names(c);
        String n = name.trim();
        if (!valid(n) || NAMES.contains(n)) {
            return false;
        }
        NAMES.add(n);
        save(c);
        return true;
    }

    static void remove(MinecraftClient c, String name) {
        names(c);
        NAMES.remove(name);
        save(c);
    }

    /** Switches the game to the offline account with this name. Returns false if it could not be done. */
    static boolean use(MinecraftClient c, String name) {
        try {
            UUID id = UUID.nameUUIDFromBytes(("OfflinePlayer:" + name).getBytes(StandardCharsets.UTF_8));
            ((com.fhasix.uitheme.mixin.NewSessionAccessor) c).fhasix$setSession(
                    new Session(name, id, "0", Optional.empty(), Optional.empty()));
            return true;
        } catch (Exception | LinkageError t) {
            NewSafe.failed("accounts", t);
            return false;
        }
    }
}
