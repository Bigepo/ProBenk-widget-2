package no.probenk.widget;

import android.content.Context;
import android.content.SharedPreferences;

final class Prefs {
    private static final String FILE = "probenk_widget";
    private static final String BASE = "base_url";
    private static final String TOKEN = "token";
    private static final String USER = "user_name";
    private static final String WIDGET_TRANSPARENCY = "widget_transparency";

    private static final String OLD_BASE = "https://probenk.no/admin/";
    private static final String NEW_BASE = "https://probenk.no/probenk_portal/admin/";

    private Prefs() {}

    static SharedPreferences p(Context c) {
        return c.getSharedPreferences(FILE, Context.MODE_PRIVATE);
    }

    static String baseUrl(Context c) {
        String saved = normalize(p(c).getString(BASE, c.getString(R.string.default_base_url)));

        // Automatisk migrering av eksisterende installasjoner som fortsatt har gammel portalsti lagret.
        if (OLD_BASE.equals(saved)) {
            saved = NEW_BASE;
            p(c).edit().putString(BASE, saved).apply();
        }

        return saved;
    }

    static String token(Context c) {
        return p(c).getString(TOKEN, "");
    }

    static String userName(Context c) {
        return p(c).getString(USER, "");
    }

    static int widgetTransparency(Context c) {
        int value = p(c).getInt(WIDGET_TRANSPARENCY, 0);
        return Math.max(0, Math.min(100, value));
    }

    static void setWidgetTransparency(Context c, int percent) {
        int value = Math.max(0, Math.min(100, percent));
        p(c).edit().putInt(WIDGET_TRANSPARENCY, value).apply();
    }

    static void saveConnection(Context c, String base, String token, String user) {
        String normalizedBase = normalize(base);
        if (OLD_BASE.equals(normalizedBase)) {
            normalizedBase = NEW_BASE;
        }

        p(c).edit()
                .putString(BASE, normalizedBase)
                .putString(TOKEN, token == null ? "" : token)
                .putString(USER, user == null ? "" : user)
                .apply();
    }

    static String normalize(String value) {
        String s = value == null ? "" : value.trim();
        if (!s.endsWith("/")) s += "/";
        return s;
    }
}
