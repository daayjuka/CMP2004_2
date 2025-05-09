package com.yourcompany.cmp2004_2;

import android.app.Application;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.preference.PreferenceManager; // Or androidx.preference.PreferenceManager
import androidx.appcompat.app.AppCompatDelegate;

import java.util.Locale;

public class MyApplication extends Application {
    // Make PREF_DARK_THEME_MODE accessible or redefine it here
    public static final String PREF_DARK_THEME_MODE = "pref_dark_theme_mode";
    public static final String PREF_LANGUAGE = "pref_language";

    @Override
    public void onCreate() {
        super.onCreate();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        int currentNightMode = prefs.getInt(PREF_DARK_THEME_MODE, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        AppCompatDelegate.setDefaultNightMode(currentNightMode);

        String lang = prefs.getString(PREF_LANGUAGE, Locale.getDefault().getLanguage());
        setAppLocale(lang);
    }

    private void setAppLocale(String langCode) {
        Locale locale = new Locale(langCode);
        Locale.setDefault(locale);
        Configuration appConfig = getApplicationContext().getResources().getConfiguration();
        Configuration newConfig = new Configuration(appConfig); // Create a new Configuration object
        newConfig.setLocale(locale);
        // newConfig.setLayoutDirection(locale); // For RTL support
        getBaseContext().getResources().updateConfiguration(newConfig, getBaseContext().getResources().getDisplayMetrics());
        // For newer Android versions, updating the application context's resources directly is more common:
        // Resources res = getApplicationContext().getResources();
        // Configuration config = res.getConfiguration();
        // config.setLocale(locale);
        // res.updateConfiguration(config, res.getDisplayMetrics());
    }

}