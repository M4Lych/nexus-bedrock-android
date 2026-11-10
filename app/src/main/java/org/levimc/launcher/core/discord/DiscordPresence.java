package org.levimc.launcher.core.discord;

import android.app.Application;

public final class DiscordPresence {

    private DiscordPresence() {
    }

    public static void initialize(Application application) {
        // Discord integration disabled.
    }

    public static boolean isConfigured() {
        return false;
    }

    public static void refresh() {
        // Discord integration disabled.
    }

    public static void shutdown() {
        // Discord integration disabled.
    }
}
