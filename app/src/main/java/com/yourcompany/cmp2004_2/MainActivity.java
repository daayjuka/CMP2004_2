package com.yourcompany.cmp2004_2;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ImageButton;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
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


import java.util.ArrayList;
import java.util.List;
import java.util.UUID; // For generating session IDs
import java.util.concurrent.Executor;

public class MainActivity extends AppCompatActivity implements ChatSessionAdapter.OnSessionClickListener {

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
            Intent intent = new Intent(getApplicationContext(), SettingsActivity.class);
            startActivity(intent);
        });

        startNewChatButton.setOnClickListener(v -> {
            startNewChat();
        });
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
    protected void onResume() {
        super.onResume();
        // Refresh sessions if user might have created one and returned
        if (currentUser != null) {
            loadChatSessions();
        }
    }
}