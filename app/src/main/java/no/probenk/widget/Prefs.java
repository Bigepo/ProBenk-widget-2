package no.probenk.widget;

import android.content.Context;
import android.content.SharedPreferences;

final class Prefs {
    private static final String FILE = "probenk_widget";
    private static final String BASE = "base_url";
    private static final String TOKEN = "token";
    private static final String USER = "user_name";
    private static final String WIDGET_TRANSPARENCY = "widget_transparency";

    private Prefs() {}

    static SharedPreferences p(Context c) {
        return c.getSharedPreferences(FILE, Context.MODE_PRIVATE);
    }

    static String baseUrl(Context c) {
        return normalize(p(c).getString(BASE, c.getString(R.string.default_base_url)));
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
        p(c).edit()
                .putString(BASE, normalize(base))
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
