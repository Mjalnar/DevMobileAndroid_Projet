package fr.android.carnetvoyage.data;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;

public final class Settings {

    private static final String PREFS_NAME = "carnet_prefs";

    private static final String KEY_SERVER_URL = "server_url";
    private static final String KEY_NIGHT_MODE = "night_mode";
    private static final String KEY_LANGUAGE = "language";

    public static final String DEFAULT_SERVER_URL = "http://10.0.2.2:8000";

    public static final String LANG_SYSTEM = "system";

    private Settings() { }

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public static String getServerUrl(Context context) {
        return prefs(context).getString(KEY_SERVER_URL, DEFAULT_SERVER_URL);
    }

    public static void setServerUrl(Context context, String url) {
        prefs(context).edit().putString(KEY_SERVER_URL, url).apply();
    }

    public static int getNightMode(Context context) {
        return prefs(context).getInt(KEY_NIGHT_MODE, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
    }

    public static void setNightMode(Context context, int mode) {
        prefs(context).edit().putInt(KEY_NIGHT_MODE, mode).apply();
        AppCompatDelegate.setDefaultNightMode(mode);
    }

    public static void applySavedNightMode(Context context) {
        AppCompatDelegate.setDefaultNightMode(getNightMode(context));
    }

    public static String getLanguage(Context context) {
        return prefs(context).getString(KEY_LANGUAGE, LANG_SYSTEM);
    }

    public static void setLanguage(Context context, String code) {
        prefs(context).edit().putString(KEY_LANGUAGE, code).apply();
        applyLanguage(code);
    }

    public static void applySavedLanguage(Context context) {
        applyLanguage(getLanguage(context));
    }

    private static void applyLanguage(String code) {
        LocaleListCompat locales = LANG_SYSTEM.equals(code)
                ? LocaleListCompat.getEmptyLocaleList()
                : LocaleListCompat.forLanguageTags(code);
        AppCompatDelegate.setApplicationLocales(locales);
    }
}
