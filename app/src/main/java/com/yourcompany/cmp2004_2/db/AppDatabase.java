package com.yourcompany.cmp2004_2.db;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration; // For proper migrations
import androidx.sqlite.db.SupportSQLiteDatabase; // For proper migrations


@Database(entities = {ChatMessageEntity.class, ChatSessionEntity.class}, version = 3, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    public abstract ChatMessageDao chatMessageDao();

    private static volatile AppDatabase INSTANCE;

    //static final Migration MIGRATION_2_3 = new Migration(2, 3) {
    //    @Override
    //    public void migrate(SupportSQLiteDatabase database) {
    //
    //        database.execSQL("ALTER TABLE chat_messages ADD COLUMN session_id TEXT");
    //
    //        database.execSQL("CREATE INDEX IF NOT EXISTS `index_chat_messages_user_id_session_id` ON `chat_messages` (`user_id`, `session_id`)");
    //
    //
    //        database.execSQL("CREATE TABLE IF NOT EXISTS `chat_sessions` (`session_id` TEXT NOT NULL, `user_id` TEXT NOT NULL, `created_timestamp` INTEGER NOT NULL, `last_message_snippet` TEXT, `last_message_timestamp` INTEGER NOT NULL, PRIMARY KEY(`session_id`))");
    //        database.execSQL("CREATE INDEX IF NOT EXISTS `index_chat_sessions_user_id` ON `chat_sessions` (`user_id`)");
    //    }
    //};

    public static AppDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                                    AppDatabase.class, "chat_database")
                            .fallbackToDestructiveMigration()
                            //.addMigrations(MIGRATION_2_3)
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}