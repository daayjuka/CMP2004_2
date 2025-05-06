package com.yourcompany.cmp2004_2.db;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.ColumnInfo;
import androidx.room.Index; // Import Index

// Add an index on userId for faster querying
@Entity(tableName = "chat_messages", indices = {@Index(value = {"user_id"})})
public class ChatMessageEntity {

    @PrimaryKey(autoGenerate = true)
    public int id;

    @ColumnInfo(name = "user_id") // New field for Firebase User ID
    public String userId;

    @ColumnInfo(name = "message_text")
    public String messageText;

    @ColumnInfo(name = "sender")
    public String sender;

    @ColumnInfo(name = "timestamp")
    public long timestamp;

    // Constructor updated to include userId
    public ChatMessageEntity(String userId, String messageText, String sender, long timestamp) {
        this.userId = userId;
        this.messageText = messageText;
        this.sender = sender;
        this.timestamp = timestamp;
    }

    // Empty constructor (optional but good practice for Room)
    public ChatMessageEntity() {}
}