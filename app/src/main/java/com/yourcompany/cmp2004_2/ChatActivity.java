package com.yourcompany.cmp2004_2;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

// Gemini API Imports
import com.google.ai.client.generativeai.GenerativeModel;
import com.google.ai.client.generativeai.java.ChatFutures; // For chat session
import com.google.ai.client.generativeai.type.Content;
import com.google.ai.client.generativeai.type.BlockThreshold;
import com.google.ai.client.generativeai.type.GenerateContentResponse;
import com.google.ai.client.generativeai.type.HarmCategory;
import com.google.ai.client.generativeai.type.SafetySetting;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

// Room Database Imports
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import com.yourcompany.cmp2004_2.db.AppDatabase;
import com.yourcompany.cmp2004_2.db.ChatMessageDao;
import com.yourcompany.cmp2004_2.db.ChatMessageEntity;

import java.util.ArrayList;
// import java.util.Collections; // Not strictly needed now
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService; // For database executor
import java.util.concurrent.Executors;    // For database executor

public class ChatActivity extends AppCompatActivity {

    RecyclerView recyclerView;
    TextView welcomeTextView;
    EditText editText;
    ImageButton sendButton;
    List<Message> messageList;
    MessageAdapter messageAdapter;
    ProgressBar progressBar;
    ImageButton backButton;

    // --- Gemini AI Setup ---
    private GenerativeModel geminiModelBase; // For starting chat sessions
    private ChatFutures chatSession;         // To maintain conversation context
    private Executor mainExecutor;

    private AppDatabase db;
    private ChatMessageDao chatMessageDao;
    private ExecutorService databaseWriteExecutor = Executors.newSingleThreadExecutor(); // For background DB operations


    private String currentUserId;
    private FirebaseAuth mAuth; // For getting current user if not passed


    // Define safety settings
    private final List<SafetySetting> safetySettings = List.of(
            new SafetySetting(HarmCategory.HARASSMENT, BlockThreshold.MEDIUM_AND_ABOVE),
            new SafetySetting(HarmCategory.HATE_SPEECH, BlockThreshold.MEDIUM_AND_ABOVE),
            new SafetySetting(HarmCategory.SEXUALLY_EXPLICIT, BlockThreshold.MEDIUM_AND_ABOVE),
            new SafetySetting(HarmCategory.DANGEROUS_CONTENT, BlockThreshold.MEDIUM_AND_ABOVE)
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        mAuth = FirebaseAuth.getInstance(); // Initialize FirebaseAuth
        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser == null) {
            // This shouldn't happen if MainActivity correctly redirects, but as a safeguard:
            Log.e("ChatActivity", "User is not logged in. Finishing activity.");
            Toast.makeText(this, "User not logged in.", Toast.LENGTH_SHORT).show();
            finish(); // Close ChatActivity if no user
            return;
        }

        currentUserId = currentUser.getUid(); // Get the current user's ID
        Log.d("ChatActivity", "Current User ID: " + currentUserId);


        messageList = new ArrayList<>();


        recyclerView = findViewById(R.id.recycler_view);
        welcomeTextView = findViewById(R.id.welcome_text);
        editText = findViewById(R.id.message_edit_text);
        sendButton = findViewById(R.id.send_button);
        progressBar = findViewById(R.id.progressBar);
        backButton = findViewById(R.id.back_button);

        mainExecutor = ContextCompat.getMainExecutor(this); // Standard way to get main executor

        messageAdapter = new MessageAdapter(messageList); // Ensure MessageAdapter exists
        recyclerView.setAdapter(messageAdapter);
        LinearLayoutManager llm = new LinearLayoutManager(this);
        llm.setStackFromEnd(true);
        recyclerView.setLayoutManager(llm);


        db = AppDatabase.getDatabase(this);
        chatMessageDao = db.chatMessageDao();
        databaseWriteExecutor = Executors.newSingleThreadExecutor();

        initializeGeminiModel();

        loadMessagesFromDbAndSetupChat();

        sendButton.setOnClickListener((v) -> {
            String question = editText.getText().toString().trim();
            if (question.isEmpty()) {
                Toast.makeText(ChatActivity.this, "Please enter a message", Toast.LENGTH_SHORT).show();
                return;
            }
            addToChatAndSave(question, Message.SENT_BY_ME);
            editText.setText("");
            welcomeTextView.setVisibility(View.GONE);
            showLoading(true);
            callGeminiApi(question);
        });

        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                startActivity(intent);
                finish();
            }
        });
    }

    private void initializeGeminiModel() {
        String apiKey = BuildConfig.GEMINI_API_KEY;
        Log.d("ChatActivity", "Attempting to use API Key: '" + apiKey + "'"); // Check Logcat Debug messages
        if (apiKey == null || apiKey.isEmpty() || apiKey.equals("YOUR_API_KEY_HERE")) {
            Log.e("ChatActivity", "API Key is missing or invalid in BuildConfig!");
            // ... rest of the error handling ...
            return;
        }
        try {
            geminiModelBase = new GenerativeModel(
                    "gemini-1.5-flash-latest", // Or "gemini-pro"
                    apiKey,
                    null, // GenerationConfig
                    safetySettings
            );
            Log.i("ChatActivity", "Base Gemini Model Initialized");
        } catch (Exception e) {
            Log.e("ChatActivity", "CRITICAL: Failed to initialize GenerativeModel!", e);
            Toast.makeText(this, "Failed to initialize AI model.", Toast.LENGTH_LONG).show();
            sendButton.setEnabled(false);
            editText.setEnabled(false);
        }
    }


    private void callGeminiApi(String question) {
        if (chatSession == null) {
            Log.e("ChatActivity", "Gemini model is not initialized!");
            addToChatAndSave("Error: Model not ready.", Message.SENT_BY_LLM);
            showLoading(false);
            return;
        }

        // Build the content
        Content content = new Content.Builder().addText(question).build();

        // Generate content asynchronously
        ListenableFuture<GenerateContentResponse> responseFuture = chatSession.sendMessage(content);

        // Add the callback to handle the result
        Futures.addCallback(responseFuture, new FutureCallback<GenerateContentResponse>() {
            @Override
            public void onSuccess(GenerateContentResponse result) {
                // This code will run on the main thread because we specified mainExecutor
                String responseText = "";
                // It's good practice to check the result and text for null
                if (result != null && result.getText() != null) {
                    responseText = result.getText();
                    Log.d("ChatActivity", "Gemini Success: " + responseText);
                } else {
                    responseText = "Received empty or null response.";
                    Log.w("ChatActivity", "Gemini response or text was null.");
                }
                addToChatAndSave(responseText, Message.SENT_BY_LLM);
                showLoading(false); // Hide progress bar and re-enable input
            }

            @Override
            public void onFailure(@NonNull Throwable t) {
                // This code will also run on the main thread
                Log.e("ChatActivity", "Gemini API Error", t);
                String errorMessage = "Error generating response: " + t.getMessage();
                // Provide more specific messages for common issues if possible
                // e.g., check t instance of specific exceptions like BlockedPromptException
                addToChatAndSave(errorMessage, Message.SENT_BY_LLM);
                showLoading(false); // Hide progress bar and re-enable input
                Toast.makeText(ChatActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        }, mainExecutor);
    }

    // Renamed method to reflect it saves to DB too
    void addToChatAndSave(String messageText, String sentBy) {

        if (currentUserId == null || currentUserId.isEmpty()) {
            Log.e("ChatActivity", "Cannot save message, currentUserId is null or empty.");
            // Optionally show a toast to the user that the message won't be saved
            Toast.makeText(this, "Error: Cannot save message. User not identified.", Toast.LENGTH_LONG).show();
            // Still add to UI for immediate feedback, but it won't persist
            runOnUiThread(() -> {
                Message uiMessage = new Message(messageText, sentBy);
                messageList.add(uiMessage);
                // ... (update adapter and scroll) ...
            });
            return; // Don't proceed to DB save
        }

        // 1. Add to UI immediately
        runOnUiThread(() -> {
            Message uiMessage = new Message(messageText, sentBy);
            messageList.add(uiMessage);
            messageAdapter.notifyItemInserted(messageList.size() - 1);
            recyclerView.smoothScrollToPosition(messageAdapter.getItemCount() - 1);
            if (welcomeTextView.getVisibility() == View.VISIBLE) {
                welcomeTextView.setVisibility(View.GONE);
            }
        });

        // 2. Save to Database in the background
        ChatMessageEntity entity = new ChatMessageEntity(currentUserId ,messageText, sentBy, System.currentTimeMillis());
        // Use ListenableFuture for DB operations to run them off the main thread.
        ListenableFuture<Void> insertFuture = chatMessageDao.insertMessage(entity);
        Futures.addCallback(insertFuture, new FutureCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                Log.d("ChatActivity", "Message saved to DB: " + messageText.substring(0, Math.min(messageText.length(), 30)) + "...");
            }
            @Override
            public void onFailure(@NonNull Throwable t) {
                Log.e("ChatActivity", "Failed to save message to DB", t);
            }
        }, databaseWriteExecutor); // Execute DB operation on background thread
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (databaseWriteExecutor != null && !databaseWriteExecutor.isShutdown()) {
            databaseWriteExecutor.shutdown();
        }
    }

    // Show/hide loading state
    void showLoading(boolean isLoading) {
        runOnUiThread(() -> {
            progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            sendButton.setEnabled(!isLoading);
            editText.setEnabled(!isLoading);
        });
    }


    private void loadMessagesFromDbAndSetupChat() {

        if (currentUserId == null || currentUserId.isEmpty()) {
            Log.e("ChatActivity", "Cannot load messages, currentUserId is null or empty.");
            // Handle this error: maybe show a message and disable chat.
            // For now, attempt to start chat with empty history.
            if (geminiModelBase != null) {
                chatSession = ChatFutures.from(geminiModelBase.startChat(new ArrayList<>()));
                Log.w("ChatActivity", "Gemini chat session started with empty history (no user ID).");
            }
            return;
        }

        ListenableFuture<List<ChatMessageEntity>> future = chatMessageDao.getAllMessagesForUser(currentUserId);
        Futures.addCallback(future, new FutureCallback<List<ChatMessageEntity>>() {
            @Override
            public void onSuccess(List<ChatMessageEntity> dbMessages) {
                List<Content> geminiHistory = new ArrayList<>();
                if (dbMessages != null && !dbMessages.isEmpty()) {
                    Log.d("ChatActivity", "Loaded " + dbMessages.size() + " messages from DB");
                    List<Message> loadedUiMessages = new ArrayList<>();
                    for (ChatMessageEntity entity : dbMessages) {
                        loadedUiMessages.add(new Message(entity.messageText, entity.sender));
                        // Build Gemini history
                        String role = Message.SENT_BY_ME.equals(entity.sender) ? "user" : "model";
                        Content.Builder contentBuilder = new Content.Builder();
                        contentBuilder.setRole(role); // Set the role first
                        contentBuilder.addText(entity.messageText); // Then add the text part
                        geminiHistory.add(contentBuilder.build());
                    }
                    // Update UI
                    messageList.clear();
                    messageList.addAll(loadedUiMessages);
                    messageAdapter.notifyDataSetChanged();
                    recyclerView.scrollToPosition(messageAdapter.getItemCount() - 1);
                    if (!messageList.isEmpty()) {
                        recyclerView.scrollToPosition(messageAdapter.getItemCount() - 1);
                        welcomeTextView.setVisibility(View.GONE);
                    } else {
                        welcomeTextView.setVisibility(View.VISIBLE);
                    }
                } else {
                    Log.d("ChatActivity", "No messages in DB.");
                    runOnUiThread(() -> welcomeTextView.setVisibility(View.VISIBLE));
                }

                // Initialize Gemini chat session with loaded history
                if (geminiModelBase != null) {
                    chatSession = ChatFutures.from(geminiModelBase.startChat(geminiHistory));
                    Log.i("ChatActivity", "Gemini chat session started with " + geminiHistory.size() + " history items.");
                } else {
                    Log.e("ChatActivity", "Cannot start chat session, geminiModelBase is null");
                }
            }

            @Override
            public void onFailure(@NonNull Throwable t) {
                Log.e("ChatActivity", "Error loading messages from DB for user " + currentUserId, t);
                Toast.makeText(ChatActivity.this, "Failed to load chat history", Toast.LENGTH_SHORT).show();
                if (geminiModelBase != null) {
                    chatSession = ChatFutures.from(geminiModelBase.startChat(new ArrayList<>()));
                    Log.w("ChatActivity", "Gemini chat session started with empty history due to DB load failure (user: " + currentUserId + ").");
                }
            }
        }, mainExecutor);
    }


}