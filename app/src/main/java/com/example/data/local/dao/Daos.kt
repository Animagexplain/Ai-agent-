package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.ChatMessageEntity
import com.example.data.local.entity.MoodLogEntity
import com.example.data.local.entity.NoteEntity
import com.example.data.local.entity.ReminderEntity
import com.example.data.local.entity.UserFactEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatDao {
    @Query("SELECT * FROM chat_messages ORDER BY timestamp ASC")
    fun getAllMessages(): Flow<List<ChatMessageEntity>>

    @Query("SELECT * FROM chat_messages ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentMessages(limit: Int): List<ChatMessageEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessageEntity): Long

    @Query("DELETE FROM chat_messages")
    suspend fun clearChatHistory()
}

@Dao
interface ReminderDao {
    @Query("SELECT * FROM reminders ORDER BY isCompleted ASC, createdAt DESC")
    fun getAllReminders(): Flow<List<ReminderEntity>>

    @Query("SELECT * FROM reminders WHERE isCompleted = 0 ORDER BY createdAt DESC")
    suspend fun getActiveReminders(): List<ReminderEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminder(reminder: ReminderEntity): Long

    @Update
    suspend fun updateReminder(reminder: ReminderEntity)

    @Delete
    suspend fun deleteReminder(reminder: ReminderEntity)

    @Query("DELETE FROM reminders WHERE id = :id")
    suspend fun deleteById(id: Long)
}

@Dao
interface NoteDao {
    @Query("SELECT * FROM notes ORDER BY createdAt DESC")
    fun getAllNotes(): Flow<List<NoteEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: NoteEntity): Long

    @Delete
    suspend fun deleteNote(note: NoteEntity)

    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun deleteById(id: Long)
}

@Dao
interface MoodDao {
    @Query("SELECT * FROM mood_logs ORDER BY timestamp DESC")
    fun getAllMoodLogs(): Flow<List<MoodLogEntity>>

    @Query("SELECT * FROM mood_logs ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestMood(): MoodLogEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMoodLog(moodLog: MoodLogEntity): Long

    @Delete
    suspend fun deleteMoodLog(moodLog: MoodLogEntity)
}

@Dao
interface UserFactDao {
    @Query("SELECT * FROM user_facts ORDER BY id ASC")
    fun getAllFacts(): Flow<List<UserFactEntity>>

    @Query("SELECT * FROM user_facts")
    suspend fun getFactsList(): List<UserFactEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFact(fact: UserFactEntity): Long

    @Delete
    suspend fun deleteFact(fact: UserFactEntity)

    @Query("SELECT COUNT(*) FROM user_facts")
    suspend fun getCount(): Int
}
