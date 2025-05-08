package com.yourcompany.cmp2004_2;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.Switch;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.google.firebase.auth.FirebaseAuth;
import com.yourcompany.cmp2004_2.db.AppDatabase;
import com.yourcompany.cmp2004_2.db.ChatMessageDao;
import android.preference.PreferenceManager;

import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SettingsActivity extends AppCompatActivity {
    private Switch languageSwitch;
    private Switch themeSwitch;
    private Button deleteAllChatsButton;
    private Button logoutButton;
    private ChatMessageDao chatMessageDao;
    private ExecutorService databaseExecutor;
    private FirebaseAuth mAuth;
    private SharedPreferences prefs;
    private static final String PREF_DARK_THEME = "pref_dark_theme";
    private static final String PREF_LANGUAGE = "pref_language";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // Initialize database
        AppDatabase db = AppDatabase.getDatabase(this);
        chatMessageDao = db.chatMessageDao();
        databaseExecutor = Executors.newSingleThreadExecutor();

        // Initialize SharedPreferences
        prefs = PreferenceManager.getDefaultSharedPreferences(this);

        // Apply saved theme
        boolean isDark = prefs.getBoolean(PREF_DARK_THEME, false);
        applyThemeBackground(isDark);

        // Initialize views
        languageSwitch = findViewById(R.id.language_switch);
        themeSwitch = findViewById(R.id.theme_switch);
        deleteAllChatsButton = findViewById(R.id.delete_all_chats_button);
        logoutButton = findViewById(R.id.logout_button);
        ImageButton backButton = findViewById(R.id.back_button);

        // Set initial states
        setupLanguageSwitch();
        setupThemeSwitch();
        setupDeleteAllChatsButton();
        setupLogoutButton();

        // Setup back button
        backButton.setOnClickListener(v -> {
            finish(); // This will close the activity and return to the previous one
        });
    }

    private void setupLanguageSwitch() {
        // Set initial state based on saved preference
        String lang = prefs.getString(PREF_LANGUAGE, "en");
        languageSwitch.setChecked(lang.equals("tr"));

        languageSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            String newLanguage = isChecked ? "tr" : "en";
            prefs.edit().putString(PREF_LANGUAGE, newLanguage).apply();
            setLocale(newLanguage);
            recreate(); // Recreate activity to apply new language
        });
    }

    private void setLocale(String lang) {
        Locale locale = new Locale(lang);
        Locale.setDefault(locale);
        android.content.res.Configuration config = new android.content.res.Configuration();
        config.setLocale(locale);
        getResources().updateConfiguration(config, getResources().getDisplayMetrics());
    }

    private void setupThemeSwitch() {
        // Set initial state based on saved preference
        boolean isDark = prefs.getBoolean(PREF_DARK_THEME, false);
        themeSwitch.setChecked(isDark);

        themeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean(PREF_DARK_THEME, isChecked).apply();
            applyThemeBackground(isChecked);
        });
    }

    private void applyThemeBackground(boolean isDark) {
        findViewById(android.R.id.content).getRootView().setBackgroundResource(
            isDark ? R.drawable.dark_thema : R.drawable.login_background
        );
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
    protected void onDestroy() {
        super.onDestroy();
        if (databaseExecutor != null) {
            databaseExecutor.shutdown();
        }
    }
} 