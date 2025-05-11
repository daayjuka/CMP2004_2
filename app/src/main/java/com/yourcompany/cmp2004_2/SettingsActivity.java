package com.yourcompany.cmp2004_2;

// import android.app.Activity; // Not strictly needed for current changes
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
// Use androidx.preference.PreferenceManager for modern apps
import androidx.preference.PreferenceManager; // CHANGED
import android.util.Log; // For debugging
import android.widget.Toast;
import android.widget.ImageButton; // Keep for backButton

import androidx.appcompat.app.AppCompatActivity; // If BaseActivity extends this
import androidx.appcompat.app.AppCompatDelegate;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

// Import Material Components
import com.google.android.material.materialswitch.MaterialSwitch; // CHANGED
import com.google.android.material.button.MaterialButton;     // CHANGED

import com.google.firebase.auth.FirebaseAuth;
import com.yourcompany.cmp2004_2.db.AppDatabase;
import com.yourcompany.cmp2004_2.db.ChatMessageDao;

import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SettingsActivity extends BaseActivity { // Assuming BaseActivity handles its own theme setup or is AppCompatActivity
    private MaterialSwitch languageSwitch; // CHANGED
    private MaterialSwitch themeSwitch;    // CHANGED
    private MaterialButton deleteAllChatsButton; // CHANGED
    private MaterialButton logoutButton;         // CHANGED
    private ImageButton backButton;

    private ChatMessageDao chatMessageDao;
    private ExecutorService databaseExecutor;
    private FirebaseAuth mAuth;
    private SharedPreferences prefs;
    private static final String PREF_DARK_THEME_MODE = "pref_dark_theme_mode";
    private static final String PREF_LANGUAGE = "pref_language";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // 1. Apply theme preference *before* super.onCreate() if BaseActivity doesn't do it,
        // OR *after* super.onCreate() and *before* setContentView() if BaseActivity is simple.
        // Let's try the common pattern:
        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        applySavedThemePreference(); // Apply the stored theme preference

        super.onCreate(savedInstanceState); // Call super after setting default night mode
        setContentView(R.layout.activity_settings);

        Log.d("SettingsActivity", "onCreate: Activity created and layout set.");

        mAuth = FirebaseAuth.getInstance();
        AppDatabase db = AppDatabase.getDatabase(this.getApplicationContext()); // Use application context
        chatMessageDao = db.chatMessageDao();
        databaseExecutor = Executors.newSingleThreadExecutor();

        // Initialize views
        languageSwitch = findViewById(R.id.language_switch);
        themeSwitch = findViewById(R.id.theme_switch);
        deleteAllChatsButton = findViewById(R.id.delete_all_chats_button);
        logoutButton = findViewById(R.id.logout_button);
        backButton = findViewById(R.id.back_button);

        // Check if views are null (for debugging)
        if (languageSwitch == null) Log.e("SettingsActivity", "languageSwitch is NULL");
        if (themeSwitch == null) Log.e("SettingsActivity", "themeSwitch is NULL");
        // etc.

        setupLanguageSwitch();
        setupThemeSwitch();
        setupDeleteAllChatsButton();
        setupLogoutButton();
        backButton.setOnClickListener(v -> {
            finish(); // Simply finish
        });
    }

    private void applySavedThemePreference() {
        int currentNightMode = prefs.getInt(PREF_DARK_THEME_MODE, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        if (AppCompatDelegate.getDefaultNightMode() != currentNightMode) {
            Log.d("SettingsActivity", "Applying saved theme mode: " + currentNightMode);
            AppCompatDelegate.setDefaultNightMode(currentNightMode);
        }
    }

    private void setupLanguageSwitch() {
        String currentLang = getCurrentLocaleLanguage();
        Log.d("SettingsActivity", "Current app language for switch: " + currentLang);
        languageSwitch.setChecked("tr".equals(currentLang));

        languageSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            String newLanguage = isChecked ? "tr" : "en";
            String oldLanguage = prefs.getString(PREF_LANGUAGE, Locale.getDefault().getLanguage());
            if (!newLanguage.equals(oldLanguage)) {
                Log.d("SettingsActivity", "Language changed to: " + newLanguage);
                prefs.edit().putString(PREF_LANGUAGE, newLanguage).apply();
                setLocaleAndRecreate(newLanguage);
            }
        });
    }

    private String getCurrentLocaleLanguage() {
        return getResources().getConfiguration().getLocales().get(0).getLanguage();
    }


    private void setLocaleAndRecreate(String langCode) {
        Locale locale = new Locale(langCode);
        Locale.setDefault(locale);
        android.content.res.Configuration config = getResources().getConfiguration();
        config.setLocale(locale);
        getResources().updateConfiguration(config, getResources().getDisplayMetrics());

        recreate();
    }

    private void setupThemeSwitch() {
        int currentAppliedNightMode = getResources().getConfiguration().uiMode & android.content.res.Configuration.UI_MODE_NIGHT_MASK;
        boolean isCurrentlyDark = currentAppliedNightMode == android.content.res.Configuration.UI_MODE_NIGHT_YES;
        Log.d("SettingsActivity", "Current applied UI mode for switch: " + (isCurrentlyDark ? "Dark" : "Light"));
        themeSwitch.setChecked(isCurrentlyDark);

        themeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            int newNightMode = isChecked ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO;
            int currentDefaultNightMode = AppCompatDelegate.getDefaultNightMode();

            if (currentDefaultNightMode != newNightMode ||
                    currentDefaultNightMode == AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM) { // If system, allow user to override
                Log.d("SettingsActivity", "Theme changed to: " + (isChecked ? "Dark" : "Light"));
                prefs.edit().putInt(PREF_DARK_THEME_MODE, newNightMode).apply();
                AppCompatDelegate.setDefaultNightMode(newNightMode);
                recreate();
            }
        });
    }


    private void setupDeleteAllChatsButton() {
        deleteAllChatsButton.setOnClickListener(v -> {
            if (mAuth.getCurrentUser() != null) {
                String userId = mAuth.getCurrentUser().getUid();
                databaseExecutor.execute(() -> {
                    try {
                        // Assuming these methods exist and work as expected
                        chatMessageDao.deleteAllMessagesForUser(userId); // Deletes messages
                        chatMessageDao.deleteAllSessionsForUser(userId); // Deletes session metadata
                        runOnUiThread(() -> {
                            Toast.makeText(SettingsActivity.this, getString(R.string.delete_all_chats) + " successful", Toast.LENGTH_SHORT).show();
                            // You might want to send a broadcast or use a LiveData/ViewModel
                            // to inform MainActivity to refresh its session list if it's visible.
                        });
                    } catch (Exception e) {
                        Log.e("SettingsActivity", "Error deleting chats", e);
                        runOnUiThread(() -> Toast.makeText(SettingsActivity.this, "Error deleting chats.", Toast.LENGTH_SHORT).show());
                    }
                });
            } else {
                Toast.makeText(SettingsActivity.this, "Not logged in.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupLogoutButton() {
        logoutButton.setOnClickListener(v -> {
            Log.d("SettingsActivity", "Logout button clicked.");
            FirebaseAuth.getInstance().signOut();
            Intent intent = new Intent(getApplicationContext(), Login.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finishAffinity(); // Finishes this activity and all parent activities.
        });
    }

    @Override
    public void onBackPressed() {
        // No need to set result if recreate() handles everything or if MainActivity re-checks onResume
        super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (databaseExecutor != null && !databaseExecutor.isShutdown()) {
            databaseExecutor.shutdown();
        }
        Log.d("SettingsActivity", "onDestroy: SettingsActivity destroyed.");
    }
}