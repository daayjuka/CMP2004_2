package com.yourcompany.cmp2004_2.db;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.List;

@Dao
public interface ChatMessageDao {

    @Insert
    ListenableFuture<Void> insertMessage(ChatMessageEntity message);

    // Get all messages for a specific user AND session, ordered by time
    @Query("SELECT * FROM chat_messages WHERE user_id = :userId AND session_id = :sessionId ORDER BY timestamp ASC")
    ListenableFuture<List<ChatMessageEntity>> getAllMessagesForSession(String userId, String sessionId);

    // --- Session Info DAO Methods ---
    @Insert(onConflict = OnConflictStrategy.REPLACE) // Create new session or update if exists
    ListenableFuture<Void> insertChatSession(ChatSessionEntity session);

    @Update
    ListenableFuture<Void> updateChatSession(ChatSessionEntity session); // For updating last message snippet

    @Query("SELECT * FROM chat_sessions WHERE user_id = :userId ORDER BY last_message_timestamp DESC")
    ListenableFuture<List<ChatSessionEntity>> getAllSessionsForUser(String userId);

    @Query("SELECT * FROM chat_sessions WHERE session_id = :sessionId AND user_id = :userId")
    ListenableFuture<ChatSessionEntity> getChatSession(String userId, String sessionId);


    // Optional: Delete all messages for a specific session
    @Query("DELETE FROM chat_messages WHERE user_id = :userId AND session_id = :sessionId")
    ListenableFuture<Void> deleteMessagesForSession(String userId, String sessionId);

    // Optional: Delete a specific session info (and its messages separately if needed)
    @Query("DELETE FROM chat_sessions WHERE session_id = :sessionId AND user_id = :userId")
    ListenableFuture<Void> deleteChatSessionInfo(String userId, String sessionId);

    @Query("DELETE FROM chat_messages WHERE user_id = :userId")
    ListenableFuture<Void> deleteAllMessagesForUser(String userId);

    @Query("DELETE FROM chat_sessions WHERE user_id = :userId")
    ListenableFuture<Void> deleteAllSessionsForUser(String userId);
}