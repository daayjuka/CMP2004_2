package com.yourcompany.cmp2004_2;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.preference.PreferenceManager; // Or androidx.preference.PreferenceManager
import androidx.appcompat.app.AppCompatActivity;
import java.util.Locale;

public abstract class BaseActivity extends AppCompatActivity {

    @Override
    protected void attachBaseContext(Context newBase) {
        // Retrieve the saved language preference
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(newBase);
        String langCode = prefs.getString(MyApplication.PREF_LANGUAGE, Locale.getDefault().getLanguage());
        super.attachBaseContext(updateBaseContextLocale(newBase, langCode));
    }

    private Context updateBaseContextLocale(Context context, String language) {
        Locale locale = new Locale(language);
        Locale.setDefault(locale); // Set for the JVM

        Configuration config = new Configuration(context.getResources().getConfiguration());
        config.setLocale(locale); // Update configuration with the new locale
        config.setLayoutDirection(locale);

        return context.createConfigurationContext(config); // Create a new context with this configuration
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String langCode = prefs.getString(MyApplication.PREF_LANGUAGE, Locale.getDefault().getLanguage());
        updateActivityLocale(langCode); // A method similar to updateBaseContextLocale but for current activity
    }

    protected void updateActivityLocale(String langCode) {
        Locale locale = new Locale(langCode);
        Locale.setDefault(locale);
        Configuration config = new Configuration(getResources().getConfiguration());
        config.setLocale(locale);
        getResources().updateConfiguration(config, getResources().getDisplayMetrics());
    }
}
