package com.yourcompany.cmp2004_2.db;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.ColumnInfo;
import androidx.room.Index;

@Entity(tableName = "chat_sessions", indices = {@Index(value = {"user_id"})}) // Index for querying sessions by user
public class ChatSessionEntity {

    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "session_id")
    public String sessionId;

    @NonNull
    @ColumnInfo(name = "user_id")
    public String userId;

    @ColumnInfo(name = "created_timestamp")
    public long createdTimestamp;

    @ColumnInfo(name = "last_message_snippet") // Optional: to display in list
    public String lastMessageSnippet;

    @ColumnInfo(name = "last_message_timestamp") // Optional: for sorting sessions
    public long lastMessageTimestamp;


    // Constructor
    public ChatSessionEntity(@NonNull String sessionId, @NonNull String userId, long createdTimestamp, String lastMessageSnippet, long lastMessageTimestamp) {
        this.sessionId = sessionId;
        this.userId = userId;
        this.createdTimestamp = createdTimestamp;
        this.lastMessageSnippet = lastMessageSnippet;
        this.lastMessageTimestamp = lastMessageTimestamp;
    }

    public ChatSessionEntity() {} // Empty constructor for Room
}