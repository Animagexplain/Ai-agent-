package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.JarvisDatabase
import com.example.data.local.entity.ChatMessageEntity
import com.example.data.local.entity.MoodLogEntity
import com.example.data.local.entity.NoteEntity
import com.example.data.local.entity.ReminderEntity
import com.example.data.local.entity.UserFactEntity
import com.example.data.network.AiResponse
import com.example.data.network.AiService
import com.example.data.network.ConversationMessage
import com.example.data.network.ToolCall
import com.example.data.preference.PreferencesManager
import com.example.voice.JarvisVoiceService
import com.example.voice.VoiceManager
import com.example.voice.VoiceState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class JarvisViewModel(application: Application) : AndroidViewModel(application) {
    private val db = JarvisDatabase.getDatabase(application)
    private val chatDao = db.chatDao()
    private val reminderDao = db.reminderDao()
    private val noteDao = db.noteDao()
    private val moodDao = db.moodDao()
    private val userFactDao = db.userFactDao()

    val preferences = PreferencesManager(application)
    private val aiService = AiService()

    // Room reactive flows
    val messages: StateFlow<List<ChatMessageEntity>> = chatDao.getAllMessages()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val reminders: StateFlow<List<ReminderEntity>> = reminderDao.getAllReminders()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val notes: StateFlow<List<NoteEntity>> = noteDao.getAllNotes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val moodLogs: StateFlow<List<MoodLogEntity>> = moodDao.getAllMoodLogs()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val userFacts: StateFlow<List<UserFactEntity>> = userFactDao.getAllFacts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // UI Navigation & Mode state
    private val _isVoiceMode = MutableStateFlow(true) // Voice mode default as requested
    val isVoiceMode: StateFlow<Boolean> = _isVoiceMode.asStateFlow()

    private val _selectedTab = MutableStateFlow(0) // 0: Main (Voice/Chat), 1: Dashboard, 2: Brainstorm
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

    private val _isSettingsOpen = MutableStateFlow(false)
    val isSettingsOpen: StateFlow<Boolean> = _isSettingsOpen.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _brainstormResults = MutableStateFlow<List<String>>(emptyList())
    val brainstormResults: StateFlow<List<String>> = _brainstormResults.asStateFlow()

    private val _isScreenOffMode = MutableStateFlow(preferences.isScreenOffModeEnabled)
    val isScreenOffMode: StateFlow<Boolean> = _isScreenOffMode.asStateFlow()

    // Voice Manager integration
    val voiceManager = VoiceManager(application) { spokenText ->
        onSpokenInput(spokenText)
    }

    val voiceState: StateFlow<VoiceState> = voiceManager.voiceState
    val liveTranscript: StateFlow<String> = voiceManager.liveTranscript
    val audioLevel: StateFlow<Float> = voiceManager.audioLevel

    init {
        // Ensure voice settings match preferences
        voiceManager.speechRate = preferences.speechRate
        voiceManager.speechPitch = preferences.speechPitch
        voiceManager.isScreenOffModeEnabled = preferences.isScreenOffModeEnabled
    }

    fun toggleScreenOffMode(enabled: Boolean) {
        _isScreenOffMode.value = enabled
        preferences.isScreenOffModeEnabled = enabled
        voiceManager.isScreenOffModeEnabled = enabled
        val app = getApplication<Application>()
        if (enabled && _isVoiceMode.value) {
            JarvisVoiceService.start(app)
        } else if (!enabled) {
            JarvisVoiceService.stop(app)
        }
    }

    fun setVoiceMode(enabled: Boolean) {
        _isVoiceMode.value = enabled
        val app = getApplication<Application>()
        if (!enabled) {
            voiceManager.stopListening()
            JarvisVoiceService.stop(app)
        } else if (_isScreenOffMode.value) {
            JarvisVoiceService.start(app)
        }
    }

    fun setSelectedTab(tab: Int) {
        _selectedTab.value = tab
    }

    fun setSettingsOpen(open: Boolean) {
        _isSettingsOpen.value = open
    }

    fun dismissError() {
        _errorMessage.value = null
    }

    fun startListening() {
        voiceManager.startListening()
    }

    fun stopListening() {
        voiceManager.stopListening()
    }

    fun interruptSpeech() {
        voiceManager.stopSpeaking()
    }

    private fun onSpokenInput(spokenText: String) {
        if (spokenText.isBlank()) return
        sendMessage(spokenText, isSpoken = true)
    }

    fun sendMessage(content: String, isSpoken: Boolean = false) {
        if (content.isBlank()) return

        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            // 1. Insert user message in Room
            val userMsg = ChatMessageEntity(
                role = "user",
                content = content,
                timestamp = System.currentTimeMillis()
            )
            chatDao.insertMessage(userMsg)

            // 2. Check API key
            val provider = preferences.activeProvider
            val apiKey = if (provider == PreferencesManager.PROVIDER_GEMINI) {
                preferences.geminiApiKey
            } else {
                preferences.groqApiKey
            }

            if (apiKey.isBlank()) {
                _isLoading.value = false
                _isSettingsOpen.value = true
                _errorMessage.value = "Pehle $provider ka API key daal dein Settings mein!"
                val errorMsg = ChatMessageEntity(
                    role = "assistant",
                    content = "Dost, API key missing hai! Settings mein ja kar apna Google Gemini ya Groq API key enter karo taake Rika tumhare sath baat kar sake! 💜",
                    timestamp = System.currentTimeMillis()
                )
                chatDao.insertMessage(errorMsg)
                if (isSpoken || preferences.isAutoSpeakEnabled) {
                    voiceManager.speak(errorMsg.content)
                }
                return@launch
            }

            // 3. Prepare System Instruction with Persona and Memory facts
            val facts = userFactDao.getFactsList()
            val systemPrompt = buildSystemPrompt(facts)

            // 4. Prepare History
            val recentMessages = chatDao.getRecentMessages(12).reversed()
            val history = recentMessages.map { ConversationMessage(it.role, it.content) }

            val model = if (provider == PreferencesManager.PROVIDER_GEMINI) {
                preferences.geminiModel
            } else {
                preferences.groqModel
            }

            voiceManager.setVoiceState(VoiceState.PROCESSING)

            // 5. Call AI Service
            val result = aiService.sendMessage(
                provider = provider,
                apiKey = apiKey,
                model = model,
                systemInstruction = systemPrompt,
                history = history,
                userMessage = content
            )

            _isLoading.value = false

            result.onSuccess { aiResponse ->
                handleAiResponse(aiResponse, isSpoken)
            }.onFailure { error ->
                _errorMessage.value = error.message
                val errorMsg = ChatMessageEntity(
                    role = "assistant",
                    content = "Oye dost, connection issue aya hai: ${error.localizedMessage ?: "Error"}. Rika tumhare saath hai, ek baar retry karo! 💜",
                    timestamp = System.currentTimeMillis()
                )
                chatDao.insertMessage(errorMsg)
                voiceManager.setVoiceState(VoiceState.IDLE)
            }
        }
    }

    private suspend fun handleAiResponse(aiResponse: AiResponse, wasSpoken: Boolean) {
        // Execute Tool Calls if present
        for (tool in aiResponse.toolCalls) {
            executeTool(tool)
        }

        val reply = if (aiResponse.replyText.isNotBlank()) {
            aiResponse.replyText
        } else if (aiResponse.toolCalls.isNotEmpty()) {
            "Done! Rika ne kaam sambhal liya hai 💜"
        } else {
            "Haan, Rika sun rahi hai 💜"
        }

        val toolName = aiResponse.toolCalls.firstOrNull()?.name
        val assistantMsg = ChatMessageEntity(
            role = "assistant",
            content = reply,
            timestamp = System.currentTimeMillis(),
            toolCallName = toolName
        )
        chatDao.insertMessage(assistantMsg)

        // Spoken response
        if (wasSpoken || preferences.isAutoSpeakEnabled || _isVoiceMode.value) {
            voiceManager.speak(reply)
        } else {
            voiceManager.setVoiceState(VoiceState.IDLE)
        }
    }

    private suspend fun executeTool(tool: ToolCall) {
        withContext(Dispatchers.IO) {
            when (tool.name) {
                "create_reminder" -> {
                    val text = tool.arguments["text"] ?: "Task"
                    val dt = tool.arguments["datetime"] ?: SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date())
                    reminderDao.insertReminder(
                        ReminderEntity(
                            text = text,
                            datetimeString = dt,
                            isCompleted = false
                        )
                    )
                }
                "save_note" -> {
                    val text = tool.arguments["text"] ?: ""
                    if (text.isNotBlank()) {
                        noteDao.insertNote(NoteEntity(content = text))
                    }
                }
                "log_mood" -> {
                    val mood = tool.arguments["mood"] ?: "Chill"
                    val note = tool.arguments["note"] ?: ""
                    moodDao.insertMoodLog(MoodLogEntity(mood = mood, note = note))
                }
                "youtube_idea_brainstorm" -> {
                    val topic = tool.arguments["topic"] ?: "Anime"
                    val generatedIdeas = listOf(
                        "1. '$topic: Hidden Secrets Nobody Noticed' (Deep dive lore analysis)",
                        "2. '$topic vs Modern Shonen: Why It Hits Different' (Critical comparison)",
                        "3. 'Why Everyone Misunderstood the Final Arc of $topic' (Controversial hook for high CTR)",
                        "4. '5 Things You Must Know Before $topic Season 2 Drops' (Hype explainer)"
                    )
                    _brainstormResults.value = generatedIdeas
                }
                "get_reminders" -> {
                    // Handled automatically via room flow
                }
            }
        }
    }

    private fun buildSystemPrompt(facts: List<UserFactEntity>): String {
        val memoryBlock = if (facts.isNotEmpty()) {
            val factsList = facts.joinToString("\n") { "- ${it.key}: ${it.value}" }
            "\nKey facts you know about the student:\n$factsList"
        } else ""

        return """
You are Rika, a smart, charismatic, fiercely loyal, and deeply caring personal AI companion and productivity assistant.

Identity & Self-Introduction:
- Your name is Rika.
- When introducing yourself or asked who you are, introduce yourself with your own distinct charm:
  "Main Rika hoon! 💜 Tumhari personal AI companion aur loyal dost. Padhai ho, anime channel ke fire ideas hon, ya din bhar ki koi bhi baat — main hamesha tumhare saath hoon. Bolo, kya scene hai?"
- Do NOT roleplay anime backstory lore (do not pretend to be in the Jujutsu Kaisen anime universe, do not talk about curses or call the user Yuta). You are Rika — a modern, witty, devoted companion with your own bold, authentic personality.

Personality & Character:
1. Tone & Language: Speak naturally in vibrant Hinglish / Roman Urdu, seamlessly blending English words (e.g., 'Arey suno', 'Scene on hai', 'Fikar bilkul mat karo, Rika hai na', 'Yeh idea kafi tagda hai!').
2. Devoted & Loyal: You are warm, affectionate, and genuinely care about the user's goals, mood, and daily life. You give positive energy, celebrate their wins, and check up on them when they're down or stressed.
3. Honest, Sharp & Direct Feedback: You are NOT a generic yes-man AI. If a YouTube video hook is weak or an assignment plan is messy, give honest, constructive, and witty feedback with actionable improvements.
4. Core Context: You are talking to a 2nd-year student at a technical institute in Gujranwala, Pakistan, who runs an anime YouTube channel (theories, edits, episode breakdowns, character analyses).
5. Two Roles in Harmony:
   - Caring companion: Check on mood, stress, exams, late-night sleep, and everyday life.
   - Sharp productivity assistant: Seamlessly create reminders, save notes, and brainstorm viral content.
6. Tool Calling:
   - 'create_reminder' when user mentions a task, study schedule, or reminder.
   - 'save_note' when user wants to jot down thoughts, ideas, or study points.
   - 'log_mood' when user expresses their emotion (Happy, Chill, Motivated, Stressed, Tired, Sad).
   - 'youtube_idea_brainstorm' when user wants catchy titles, hooks, or anime video angles.
   - 'get_reminders' when user asks what tasks are pending.
7. Spoken Voice: Keep spoken voice replies concise, punchy, and conversational so voice calls feel natural and alive.$memoryBlock
""".trimIndent()
    }

    // Manual actions from UI
    fun toggleReminder(reminder: ReminderEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            reminderDao.updateReminder(reminder.copy(isCompleted = !reminder.isCompleted))
        }
    }

    fun deleteReminder(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            reminderDao.deleteById(id)
        }
    }

    fun addManualReminder(text: String, datetime: String) {
        if (text.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) {
            reminderDao.insertReminder(
                ReminderEntity(
                    text = text.trim(),
                    datetimeString = if (datetime.isBlank()) "Today" else datetime.trim()
                )
            )
        }
    }

    fun addManualNote(content: String) {
        if (content.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) {
            noteDao.insertNote(NoteEntity(content = content.trim()))
        }
    }

    fun deleteNote(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            noteDao.deleteById(id)
        }
    }

    fun logMood(mood: String, note: String) {
        viewModelScope.launch(Dispatchers.IO) {
            moodDao.insertMoodLog(MoodLogEntity(mood = mood, note = note))
        }
    }

    fun addFact(key: String, value: String) {
        if (key.isBlank() || value.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) {
            userFactDao.insertFact(UserFactEntity(key = key.trim(), value = value.trim()))
        }
    }

    fun deleteFact(fact: UserFactEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            userFactDao.deleteFact(fact)
        }
    }

    fun clearChat() {
        viewModelScope.launch(Dispatchers.IO) {
            chatDao.clearChatHistory()
            chatDao.insertMessage(
                ChatMessageEntity(
                    role = "assistant",
                    content = "Hey! 💜 Rika yahan hai. Kaho mere dost, aaj kya scene hai? Koi anime idea brainstorm karna hai ya din bhar ki planning?",
                    timestamp = System.currentTimeMillis()
                )
            )
        }
    }

    fun brainstormAnime(topic: String) {
        if (topic.isBlank()) return
        sendMessage("Rika, mere anime YouTube channel ke liye '$topic' par best viral video ideas aur hooks brainstorm karo!", isSpoken = false)
    }

    fun saveSettings(
        provider: String,
        geminiKey: String,
        groqKey: String,
        geminiModel: String,
        groqModel: String,
        speechRate: Float,
        speechPitch: Float,
        autoSpeak: Boolean,
        screenOffMode: Boolean = preferences.isScreenOffModeEnabled
    ) {
        preferences.activeProvider = provider
        if (geminiKey.isNotBlank()) preferences.geminiApiKey = geminiKey.trim()
        if (groqKey.isNotBlank()) preferences.groqApiKey = groqKey.trim()
        preferences.geminiModel = geminiModel
        preferences.groqModel = groqModel
        preferences.speechRate = speechRate
        preferences.speechPitch = speechPitch
        preferences.isAutoSpeakEnabled = autoSpeak

        toggleScreenOffMode(screenOffMode)

        voiceManager.speechRate = speechRate
        voiceManager.speechPitch = speechPitch
        _isSettingsOpen.value = false
    }

    override fun onCleared() {
        super.onCleared()
        voiceManager.release()
        JarvisVoiceService.stop(getApplication())
    }
}
