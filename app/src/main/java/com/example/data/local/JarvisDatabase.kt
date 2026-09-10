package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.local.dao.ChatDao
import com.example.data.local.dao.MoodDao
import com.example.data.local.dao.NoteDao
import com.example.data.local.dao.ReminderDao
import com.example.data.local.dao.UserFactDao
import com.example.data.local.entity.ChatMessageEntity
import com.example.data.local.entity.MoodLogEntity
import com.example.data.local.entity.NoteEntity
import com.example.data.local.entity.ReminderEntity
import com.example.data.local.entity.UserFactEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        ChatMessageEntity::class,
        ReminderEntity::class,
        NoteEntity::class,
        MoodLogEntity::class,
        UserFactEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class JarvisDatabase : RoomDatabase() {
    abstract fun chatDao(): ChatDao
    abstract fun reminderDao(): ReminderDao
    abstract fun noteDao(): NoteDao
    abstract fun moodDao(): MoodDao
    abstract fun userFactDao(): UserFactDao

    companion object {
        @Volatile
        private var INSTANCE: JarvisDatabase? = null

        fun getDatabase(context: Context): JarvisDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    JarvisDatabase::class.java,
                    "jarvis_companion_db"
                ).addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        // Pre-populate core persona memory facts about the student
                        CoroutineScope(Dispatchers.IO).launch {
                            val database = getDatabase(context)
                            val factDao = database.userFactDao()
                            factDao.insertFact(
                                UserFactEntity(
                                    key = "Profile",
                                    value = "2nd-year technical student in Gujranwala, Pakistan",
                                    category = "academic"
                                )
                            )
                            factDao.insertFact(
                                UserFactEntity(
                                    key = "YouTube Channel",
                                    value = "Runs an anime YouTube channel (theories, recaps & edits)",
                                    category = "creative"
                                )
                            )
                            factDao.insertFact(
                                UserFactEntity(
                                    key = "Communication Style",
                                    value = "Hinglish / Roman Urdu friendly banter, direct and authentic advice",
                                    category = "preference"
                                )
                            )

                            // Initial welcome message
                            val chatDao = database.chatDao()
                            chatDao.insertMessage(
                                ChatMessageEntity(
                                    role = "assistant",
                                    content = "Arey salaam! Main Rika hoon 💜 Tumhari apni AI companion aur dost! Gujranwala technical institute ki padhai ho, anime channel ke viral content ideas hon, ya koi reminder set karna ho — main hamesha ready hoon. Mic dabao ya text karo, bolo kya chal raha hai?",
                                    timestamp = System.currentTimeMillis()
                                )
                            )
                        }
                    }
                }).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
