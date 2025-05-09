package com.yourcompany.cmp2004_2;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager; // Use androidx.preference for modern apps
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Switch; // Use androidx.appcompat.widget.SwitchCompat for consistent styling
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate; // Import this
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
// import androidx.preference.PreferenceManagerFix; // If using androidx.preference

import com.google.firebase.auth.FirebaseAuth;
import com.yourcompany.cmp2004_2.db.AppDatabase;
import com.yourcompany.cmp2004_2.db.ChatMessageDao;

import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SettingsActivity extends BaseActivity {
    private Switch languageSwitch; // Consider androidx.appcompat.widget.SwitchCompat
    private Switch themeSwitch;    // Consider androidx.appcompat.widget.SwitchCompat
    private Button deleteAllChatsButton;
    private Button logoutButton;
    private ChatMessageDao chatMessageDao;
    private ExecutorService databaseExecutor;
    private FirebaseAuth mAuth;
    private SharedPreferences prefs; // Use androidx.preference.SharedPreferences for consistency
    private static final String PREF_DARK_THEME_MODE = "pref_dark_theme_mode"; // Store mode (light, dark, system)
    private static final String PREF_LANGUAGE = "pref_language";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Apply theme BEFORE setContentView
        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        // Or if using androidx.preference:
        // prefs = androidx.preference.PreferenceManager.getDefaultSharedPreferences(this);

        applySavedThemePreference(); // Apply the stored theme preference

        setContentView(R.layout.activity_settings);

        mAuth = FirebaseAuth.getInstance();
        AppDatabase db = AppDatabase.getDatabase(this);
        chatMessageDao = db.chatMessageDao();
        databaseExecutor = Executors.newSingleThreadExecutor();

        languageSwitch = findViewById(R.id.language_switch);
        themeSwitch = findViewById(R.id.theme_switch);
        deleteAllChatsButton = findViewById(R.id.delete_all_chats_button);
        logoutButton = findViewById(R.id.logout_button);
        ImageButton backButton = findViewById(R.id.back_button);

        setupLanguageSwitch();
        setupThemeSwitch();
        setupDeleteAllChatsButton();
        setupLogoutButton();
        backButton.setOnClickListener(v -> {
            Intent resultIntent = new Intent();
            resultIntent.putExtra("languageChanged", true); // Or some other flag
            setResult(Activity.RESULT_OK, resultIntent);
            finish();
        } );
    }

    private void applySavedThemePreference() {
        // Default to system theme if no preference is saved
        int currentNightMode = prefs.getInt(PREF_DARK_THEME_MODE, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        AppCompatDelegate.setDefaultNightMode(currentNightMode);
        // No need to call recreate() here as it's done before setContentView
        // and will apply when the activity is first created.
    }

    private void setupLanguageSwitch() {
        String lang = prefs.getString(PREF_LANGUAGE, Locale.getDefault().getLanguage()); // Default to system language
        languageSwitch.setChecked(lang.equals("tr"));
        languageSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            String newLanguage = isChecked ? "tr" : "en";
            prefs.edit().putString(PREF_LANGUAGE, newLanguage).apply();
            setLocaleAndRecreate(newLanguage);
        });
    }

    private void setLocaleAndRecreate(String lang) {
        Locale locale = new Locale(lang);
        Locale.setDefault(locale);
        android.content.res.Configuration config = new android.content.res.Configuration(getResources().getConfiguration());
        config.setLocale(locale);
        getResources().updateConfiguration(config, getResources().getDisplayMetrics());

        Intent intent = new Intent("ACTION_LANGUAGE_CHANGED");
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
        recreate(); // Recreate activity to apply new language AND current theme
    }

    private void setupThemeSwitch() {
        int currentNightMode = AppCompatDelegate.getDefaultNightMode();
        // This logic might need adjustment if you want a tri-state (Light, Dark, System)
        // For a simple Light/Dark toggle:
        themeSwitch.setChecked(currentNightMode == AppCompatDelegate.MODE_NIGHT_YES);

        themeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            int newNightMode = isChecked ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO;
            // You could also offer AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM

            // Save the preference
            prefs.edit().putInt(PREF_DARK_THEME_MODE, newNightMode).apply();

            // Apply the theme
            AppCompatDelegate.setDefaultNightMode(newNightMode);
            // No need to call recreate() if changing setDefaultNightMode for the current activity.
            // However, for a more immediate effect across all components, recreate() can be useful.
            // For DayNight themes, often just setting it is enough for new activities.
            // If you want the *current* activity to immediately reflect all changes, use recreate().
            // Be mindful that recreate() can be a bit jarring.
            // If not recreating, views might only update their theme-dependent attributes
            // if they explicitly listen for configuration changes or are redrawn.
            recreate(); // To ensure immediate full theme application
        });
    }

    private void setupDeleteAllChatsButton() {
        deleteAllChatsButton.setOnClickListener(v -> {
            if (mAuth.getCurrentUser() != null) {
                String userId = mAuth.getCurrentUser().getUid();
                databaseExecutor.execute(() -> {
                    // Delete all messages and sessions for the current user
                    chatMessageDao.deleteAllMessagesForUser(userId);
                    chatMessageDao.deleteAllSessionsForUser(userId);
                    runOnUiThread(() -> {
                        Toast.makeText(SettingsActivity.this, "All chats deleted", Toast.LENGTH_SHORT).show();
                    });
                });
            }
        });
    }

    private void setupLogoutButton() {
        logoutButton.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            Intent intent = new Intent(getApplicationContext(), Login.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }


    @Override
    public void onBackPressed() {
        Intent resultIntent = new Intent();
        resultIntent.putExtra("languageChanged", true);
        setResult(Activity.RESULT_OK, resultIntent);
        super.onBackPressed(); // This will call finish()
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (databaseExecutor != null) {
            databaseExecutor.shutdown();
        }
    }
} 