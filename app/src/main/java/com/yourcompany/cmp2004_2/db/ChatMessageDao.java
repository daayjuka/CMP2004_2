package com.yourcompany.cmp2004_2.db;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.List;

@Dao
public interface ChatMessageDao {

    @Insert
    ListenableFuture<Void> insertMessage(ChatMessageEntity message);

    // Get all messages for a specific user, ordered by time
    @Query("SELECT * FROM chat_messages WHERE user_id = :userId ORDER BY timestamp ASC")
    ListenableFuture<List<ChatMessageEntity>> getAllMessagesForUser(String userId);

    // Optional: Clear all messages for a specific user
    @Query("DELETE FROM chat_messages WHERE user_id = :userId")
    ListenableFuture<Void> deleteAllMessagesForUser(String userId);

    // Optional: Clear all messages (be careful with this one)
    // @Query("DELETE FROM chat_messages")
    // ListenableFuture<Void> deleteAllMessages();
}