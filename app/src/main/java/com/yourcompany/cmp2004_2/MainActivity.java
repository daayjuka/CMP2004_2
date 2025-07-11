package com.yourcompany.cmp2004_2;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ImageButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.yourcompany.cmp2004_2.db.AppDatabase; // DB imports
import com.yourcompany.cmp2004_2.db.ChatMessageDao;
import com.yourcompany.cmp2004_2.db.ChatSessionEntity;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import androidx.appcompat.app.AlertDialog; // For confirmation dialog
import android.content.DialogInterface;    // For confirmation dialog

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID; // For generating session IDs
import java.util.concurrent.Executor;

public class MainActivity extends BaseActivity implements ChatSessionAdapter.OnSessionActionsListener  {

    FirebaseAuth auth;
    TextView userDetailsTextView;
    FirebaseUser currentUser;
    Button startNewChatButton;

    RecyclerView sessionsRecyclerView;
    ChatSessionAdapter sessionAdapter;
    List<ChatSessionEntity> sessionList = new ArrayList<>();

    // Database
    AppDatabase db;
    ChatMessageDao chatMessageDao;
    Executor mainExecutor;
    private static final int SETTINGS_ACTIVITY_REQUEST_CODE = 101;
    private String currentLanguage; // Store the language MainActivity was created with


    private BroadcastReceiver languageChangeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if ("ACTION_LANGUAGE_CHANGED".equals(intent.getAction())) {
                recreate();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        auth = FirebaseAuth.getInstance();
        userDetailsTextView = findViewById(R.id.user_details);
        startNewChatButton = findViewById(R.id.start_new_chat_button);
        sessionsRecyclerView = findViewById(R.id.sessions_recycler_view);
        ImageButton settingsButton = findViewById(R.id.settings_button);

        currentUser = auth.getCurrentUser();
        mainExecutor = ContextCompat.getMainExecutor(this);

        // Initialize Database
        db = AppDatabase.getDatabase(getApplicationContext());
        chatMessageDao = db.chatMessageDao();

        if (currentUser == null) {
            redirectToLogin();
        } else {
            userDetailsTextView.setText(currentUser.getEmail());
            setupRecyclerView();
            loadChatSessions(); // Load existing sessions
        }

        settingsButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
            startActivityForResult(intent, SETTINGS_ACTIVITY_REQUEST_CODE);
        });

        startNewChatButton.setOnClickListener(v -> {
            startNewChat();
        });

        currentLanguage = getResources().getConfiguration().getLocales().get(0).getLanguage();
        Log.d("MainActivity", "onCreate - current language: " + currentLanguage);

        LocalBroadcastManager.getInstance(this).registerReceiver(languageChangeReceiver,
                new IntentFilter("ACTION_LANGUAGE_CHANGED"));
    }

    private void setupRecyclerView() {
        sessionAdapter = new ChatSessionAdapter(this); // Pass 'this' as listener
        sessionsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        sessionsRecyclerView.setAdapter(sessionAdapter);
    }

    private void loadChatSessions() {
        if (currentUser == null) return;
        ListenableFuture<List<ChatSessionEntity>> future = chatMessageDao.getAllSessionsForUser(currentUser.getUid());
        Futures.addCallback(future, new FutureCallback<List<ChatSessionEntity>>() {
            @Override
            public void onSuccess(List<ChatSessionEntity> sessions) {
                Log.d("MainActivity", "Loaded " + (sessions != null ? sessions.size() : 0) + " sessions.");
                sessionList.clear();
                if (sessions != null) {
                    sessionList.addAll(sessions);
                }
                sessionAdapter.setSessions(sessionList);
            }
            @Override
            public void onFailure(@NonNull Throwable t) {
                Log.e("MainActivity", "Failed to load chat sessions", t);
                Toast.makeText(MainActivity.this, "Could not load chat history.", Toast.LENGTH_SHORT).show();
            }
        }, mainExecutor);
    }



    private void startNewChat() {
        if (currentUser == null) {
            Toast.makeText(this, "Please log in to start a chat.", Toast.LENGTH_SHORT).show();
            return;
        }
        String newSessionId = UUID.randomUUID().toString();
        long currentTime = System.currentTimeMillis();

        // Create a new session entry in the database
        ChatSessionEntity newSession = new ChatSessionEntity(
                newSessionId,
                currentUser.getUid(),
                currentTime,
                "New Chat Started...", // Initial snippet
                currentTime
        );

        ListenableFuture<Void> insertFuture = chatMessageDao.insertChatSession(newSession);
        Futures.addCallback(insertFuture, new FutureCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                Log.d("MainActivity", "New chat session created in DB: " + newSessionId);
                // Refresh the list or add it directly
                loadChatSessions(); // Simple way to refresh
                // Now open ChatActivity with this new session ID
                openChatActivity(newSessionId);
            }
            @Override
            public void onFailure(@NonNull Throwable t) {
                Log.e("MainActivity", "Failed to create new session in DB", t);
                Toast.makeText(MainActivity.this, "Error starting new chat.", Toast.LENGTH_SHORT).show();
            }
        }, ContextCompat.getMainExecutor(this)); // Or a background executor for DB write
    }

    private void openChatActivity(String sessionId) {
        Intent intent = new Intent(getApplicationContext(), ChatActivity.class);
        intent.putExtra("SESSION_ID", sessionId);
        // userId is fetched within ChatActivity from FirebaseAuth.getInstance().getCurrentUser()
        startActivity(intent);
        // Do NOT finish MainActivity here if you want to return to it
    }

    @Override
    public void onSessionClick(ChatSessionEntity session) {
        Log.d("MainActivity", "Clicked session: " + session.sessionId);
        openChatActivity(session.sessionId);
    }

    private void redirectToLogin() {
        Intent intent = new Intent(getApplicationContext(), Login.class);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == SETTINGS_ACTIVITY_REQUEST_CODE) {
            // Check if settings actually changed the language
            // This is simpler: just check current applied language vs stored one
            String newLanguage = getResources().getConfiguration().getLocales().get(0).getLanguage();
            Log.d("MainActivity", "onActivityResult - new language: " + newLanguage + ", old: " + currentLanguage);

            if (!newLanguage.equals(currentLanguage)) {
                Log.d("MainActivity", "Language changed, recreating MainActivity.");
                currentLanguage = newLanguage; // Update stored language
                recreate(); // Recreate MainActivity
            }
            // You could also check the 'languageChanged' extra from the resultIntent
            // if (resultCode == Activity.RESULT_OK && data != null && data.getBooleanExtra("languageChanged", false)) {
            //     recreate();
            // }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        super.onResume();
        // Check if language has changed since MainActivity was last active
        // This is an alternative to onActivityResult if SettingsActivity is not started for result
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String savedLang = prefs.getString(MyApplication.PREF_LANGUAGE, Locale.getDefault().getLanguage());

        if (currentLanguage != null && !currentLanguage.equals(savedLang)) {
            Log.d("MainActivity", "onResume - language mismatch, recreating. Current: " + currentLanguage + " Saved: " + savedLang);
            // currentLanguage = savedLang; // Update before recreate or it will be set in next onCreate
            recreate();
        } else if (currentLanguage == null) {
            // This case can happen if onResume is called before onCreate fully initializes currentLanguage
            // For safety, update currentLanguage here after BaseActivity has done its job.
            currentLanguage = getResources().getConfiguration().getLocales().get(0).getLanguage();
        }

        // Refresh sessions if user might have created one and returned
        if (currentUser != null) {
            loadChatSessions();
        }
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(languageChangeReceiver);
    }

    @Override
    public void onDeleteSessionClick(ChatSessionEntity session) {
        Log.d("MainActivity", "Attempting to delete session: " + session.sessionId);

        // Show a confirmation dialog
        new AlertDialog.Builder(this)
                .setTitle("Delete Chat Session")
                .setMessage("Are you sure you want to delete this chat session and all its messages? This action cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> {
                    // User confirmed deletion
                    deleteSessionFromDatabase(session);
                })
                .setNegativeButton("Cancel", null)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    private void deleteSessionFromDatabase(ChatSessionEntity session) {
        if (currentUser == null || session == null) {
            Toast.makeText(this, "Error: Cannot delete session.", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = currentUser.getUid();
        String sessionId = session.sessionId;

        ListenableFuture<Void> deleteMessagesFuture = chatMessageDao.deleteMessagesForSession(userId, sessionId);
        Futures.addCallback(deleteMessagesFuture, new FutureCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                Log.d("MainActivity", "Messages deleted for session: " + sessionId);
                // 2. After messages are deleted, delete the session info itself
                ListenableFuture<Void> deleteSessionInfoFuture = chatMessageDao.deleteChatSessionInfo(userId, sessionId);
                Futures.addCallback(deleteSessionInfoFuture, new FutureCallback<Void>() {
                    @Override
                    public void onSuccess(Void result) {
                        Log.d("MainActivity", "Session info deleted: " + sessionId);
                        Toast.makeText(MainActivity.this, "Chat session deleted.", Toast.LENGTH_SHORT).show();
                        // Refresh the list in the UI
                        loadChatSessions();
                    }

                    @Override
                    public void onFailure(@NonNull Throwable t) {
                        Log.e("MainActivity", "Failed to delete session info for: " + sessionId, t);
                        Toast.makeText(MainActivity.this, "Error deleting session info.", Toast.LENGTH_SHORT).show();
                    }
                }, mainExecutor); // Or a background executor
            }

            @Override
            public void onFailure(@NonNull Throwable t) {
                Log.e("MainActivity", "Failed to delete messages for session: " + sessionId, t);
                Toast.makeText(MainActivity.this, "Error deleting session messages.", Toast.LENGTH_SHORT).show();
            }
        }, ContextCompat.getMainExecutor(this));

    }
}