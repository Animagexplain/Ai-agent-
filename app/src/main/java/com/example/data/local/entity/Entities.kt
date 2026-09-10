package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val role: String, // "user", "assistant", "system", "tool"
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val toolCallName: String? = null,
    val isSpoken: Boolean = false
)

@Entity(tableName = "reminders")
data class ReminderEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val text: String,
    val datetimeString: String,
    val isCompleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val content: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "mood_logs")
data class MoodLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val mood: String, // "Happy", "Chill", "Motivated", "Stressed", "Tired", "Sad"
    val note: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "user_facts")
data class UserFactEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val key: String,
    val value: String,
    val category: String = "general"
)
