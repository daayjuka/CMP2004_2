package com.yourcompany.cmp2004_2;

import android.app.Application;
import android.content.SharedPreferences;
import android.preference.PreferenceManager; // Or androidx.preference.PreferenceManager
import androidx.appcompat.app.AppCompatDelegate;

public class MyApplication extends Application {
    // Make PREF_DARK_THEME_MODE accessible or redefine it here
    public static final String PREF_DARK_THEME_MODE = "pref_dark_theme_mode";

    @Override
    public void onCreate() {
        super.onCreate();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        int currentNightMode = prefs.getInt(PREF_DARK_THEME_MODE, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        AppCompatDelegate.setDefaultNightMode(currentNightMode);
    }
}