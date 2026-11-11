package org.levimc.launcher.util;

import android.content.Context;
import android.content.SharedPreferences;

import org.levimc.launcher.core.content.ContentItem;
import org.levimc.launcher.core.content.ResourcePackItem;
import org.levimc.launcher.core.content.WorldItem;
import org.levimc.launcher.core.versions.GameVersion;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HashSet;
import java.util.Set;

/** Local-only favorites, recent editor visits, and dashboard visibility preferences. */
public final class LocalPersonalizationStore {
    private static final String PREFS = "oblivion_local_personalization";
    private static final String KEY_PROFILE_FAVORITES = "favorite_profiles";
    private static final String KEY_CONTENT_FAVORITES = "favorite_content";
    private static final String RECENT_WORLD_PREFIX = "recent_world_";
    private static final String DASHBOARD_PREFIX = "dashboard_";

    public static final String DASHBOARD_MODS = "mods";
    public static final String DASHBOARD_CONTENT = "content";
    public static final String DASHBOARD_SHORTCUTS = "shortcuts";
    public static final String ACTION_CURSEFORGE = "action_curseforge";
    public static final String ACTION_AI = "action_ai";
    public static final String ACTION_PACKAGES = "action_packages";
    public static final String ACTION_EXTERNAL_MODS = "action_external_mods";
    public static final String ACTION_ACCOUNTS = "action_accounts";
    public static final String ACTION_QUICK_LAUNCH = "action_quick_launch";

    private LocalPersonalizationStore() { }

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public static String profileId(GameVersion version) {
        if (version == null) return "";
        if (version.isInstalled && version.packageName != null && !version.packageName.trim().isEmpty()) {
            return "installed:" + version.packageName.trim();
        }
        if (version.directoryName == null || version.directoryName.trim().isEmpty()) return "";
        return "custom:" + LauncherStorage.sanitizeProfileId(version.directoryName);
    }

    public static boolean isProfileFavorite(Context context, GameVersion version) {
        String id = profileId(version);
        return !id.isEmpty() && prefs(context).getStringSet(KEY_PROFILE_FAVORITES, new HashSet<>()).contains(id);
    }

    public static void toggleProfileFavorite(Context context, GameVersion version) {
        String id = profileId(version);
        if (!id.isEmpty()) toggleSet(context, KEY_PROFILE_FAVORITES, id);
    }

    public static boolean isContentFavorite(Context context, ContentItem item) {
        String id = contentId(item);
        return !id.isEmpty() && prefs(context).getStringSet(KEY_CONTENT_FAVORITES, new HashSet<>()).contains(id);
    }

    public static void toggleContentFavorite(Context context, ContentItem item) {
        String id = contentId(item);
        if (!id.isEmpty()) toggleSet(context, KEY_CONTENT_FAVORITES, id);
    }

    public static Set<String> getFavoriteProfileIds(Context context) {
        return new HashSet<>(prefs(context).getStringSet(KEY_PROFILE_FAVORITES, new HashSet<>()));
    }

    public static boolean isDashboardVisible(Context context, String item) {
        return prefs(context).getBoolean(DASHBOARD_PREFIX + item, true);
    }

    public static void setDashboardVisible(Context context, String item, boolean visible) {
        prefs(context).edit().putBoolean(DASHBOARD_PREFIX + item, visible).apply();
    }

    public static void resetDashboard(Context context) {
        SharedPreferences.Editor editor = prefs(context).edit();
        for (String key : prefs(context).getAll().keySet()) {
            if (key.startsWith(DASHBOARD_PREFIX)) editor.remove(key);
        }
        editor.apply();
    }

    /** Records that a user explicitly opened a world in Oblivion's editor, not Minecraft play time. */
    public static void recordWorldEditorOpened(Context context, WorldItem world) {
        String id = contentId(world);
        if (!id.isEmpty()) prefs(context).edit().putLong(RECENT_WORLD_PREFIX + digest(id), System.currentTimeMillis()).apply();
    }

    public static long getWorldEditorVisit(Context context, WorldItem world) {
        String id = contentId(world);
        return id.isEmpty() ? 0L : prefs(context).getLong(RECENT_WORLD_PREFIX + digest(id), 0L);
    }

    private static String contentId(ContentItem item) {
        if (item == null) return "";
        if (item instanceof ResourcePackItem) {
            ResourcePackItem pack = (ResourcePackItem) item;
            ResourcePackItem.PackType packType = pack.getPackType();
            String kind = packType == null ? "unknown" : packType.name().toLowerCase(java.util.Locale.ROOT);
            String uuid = pack.getUuid();
            if (uuid != null && !uuid.trim().isEmpty()) return "pack:" + kind + ":" + uuid.trim().toLowerCase(java.util.Locale.ROOT);
            return "pack-path:" + kind + ":" + digest(item.getStablePath());
        }
        if (item instanceof WorldItem) return "world-path:" + digest(item.getStablePath());
        return "content-path:" + item.getType() + ":" + digest(item.getStablePath());
    }

    private static void toggleSet(Context context, String key, String value) {
        SharedPreferences preferences = prefs(context);
        Set<String> values = new HashSet<>(preferences.getStringSet(key, new HashSet<>()));
        if (!values.add(value)) values.remove(value);
        preferences.edit().putStringSet(key, values).apply();
    }

    private static String digest(String value) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder out = new StringBuilder(hash.length * 2);
            for (byte b : hash) out.append(String.format(java.util.Locale.ROOT, "%02x", b & 0xff));
            return out.toString();
        } catch (Exception impossible) {
            return Integer.toHexString(value.hashCode());
        }
    }
}
