package com.example.data.network

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class ToolCall(
    val name: String,
    val arguments: Map<String, String>
)

data class AiResponse(
    val replyText: String,
    val toolCalls: List<ToolCall> = emptyList()
)

data class ConversationMessage(
    val role: String, // "user", "assistant"
    val content: String
)

class AiService {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    suspend fun sendMessage(
        provider: String,
        apiKey: String,
        model: String,
        systemInstruction: String,
        history: List<ConversationMessage>,
        userMessage: String
    ): Result<AiResponse> = withContext(Dispatchers.IO) {
        try {
            if (apiKey.isBlank()) {
                return@withContext Result.failure(
                    IllegalArgumentException("API Key missing. Please configure your $provider API key in Settings.")
                )
            }

            if (provider == "groq") {
                callGroq(apiKey, model, systemInstruction, history, userMessage)
            } else {
                callGemini(apiKey, model, systemInstruction, history, userMessage)
            }
        } catch (e: Exception) {
            Log.e("AiService", "Error calling $provider", e)
            Result.failure(e)
        }
    }

    private fun callGemini(
        apiKey: String,
        model: String,
        systemInstruction: String,
        history: List<ConversationMessage>,
        userMessage: String
    ): Result<AiResponse> {
        // Automatically map non-existent or legacy models to official ultra-fast Gemini 2.5 Flash
        val cleanModel = when {
            model.contains("3.8") || model.isBlank() -> "gemini-2.5-flash"
            model.contains("3.5") -> "gemini-3.5-flash"
            model.contains("lite") -> "gemini-3.1-flash-lite-preview"
            else -> model.trim()
        }

        // Only valid, highly responsive models in priority order
        val candidateModels = linkedSetOf(
            cleanModel,
            "gemini-2.5-flash",
            "gemini-flash-latest"
        ).toList()

        // Smart tool inclusion: only declare tools when user asks for an action or task
        // Skipping tools for general chat boosts Gemini speed by 2x to 3x!
        val lowerMsg = userMessage.lowercase()
        val wantsTools = lowerMsg.contains("remind") || lowerMsg.contains("yaad") ||
                lowerMsg.contains("note") || lowerMsg.contains("likh") ||
                lowerMsg.contains("mood") || lowerMsg.contains("stress") ||
                lowerMsg.contains("idea") || lowerMsg.contains("video") ||
                lowerMsg.contains("hook") || lowerMsg.contains("script") ||
                lowerMsg.contains("channel") || lowerMsg.contains("task") ||
                lowerMsg.contains("schedule")

        var lastError: Throwable? = null

        for (candidate in candidateModels) {
            Log.d("AiService", "Attempting Gemini with model: $candidate (tools: $wantsTools)")
            val attempt = executeGeminiRequest(apiKey, candidate, systemInstruction, history, userMessage, includeTools = wantsTools)
            if (attempt.isSuccess) {
                return attempt
            }

            val err = attempt.exceptionOrNull()?.message.orEmpty()
            lastError = attempt.exceptionOrNull()

            // If it failed due to tool calling schema / argument validation, retry candidate without tools
            if (wantsTools && (err.contains("INVALID_ARGUMENT", ignoreCase = true) || err.contains("functionDeclarations", ignoreCase = true))) {
                val toolFreeAttempt = executeGeminiRequest(apiKey, candidate, systemInstruction, history, userMessage, includeTools = false)
                if (toolFreeAttempt.isSuccess) {
                    return toolFreeAttempt
                }
            }

            // Check if failure is due to high demand, unavailable, rate limit, or model not found
            val isRecoverableWithAnotherModel =
                err.contains("503") ||
                err.contains("high demand", ignoreCase = true) ||
                err.contains("UNAVAILABLE", ignoreCase = true) ||
                err.contains("429") ||
                err.contains("RESOURCE_EXHAUSTED", ignoreCase = true) ||
                err.contains("404") ||
                err.contains("not found", ignoreCase = true) ||
                err.contains("models/") ||
                err.contains("500")

            if (isRecoverableWithAnotherModel) {
                Log.w("AiService", "Model '$candidate' encountered ($err). Trying next model in fallback list...")
                continue
            } else {
                // If it's a hard authentication error, stop early
                if (err.contains("API_KEY_INVALID", ignoreCase = true) || err.contains("401") || err.contains("403")) {
                    return attempt
                }
            }
        }

        return Result.failure(lastError ?: Exception("Unable to connect to Gemini after trying fallback models."))
    }

    private fun executeGeminiRequest(
        apiKey: String,
        modelName: String,
        systemInstruction: String,
        history: List<ConversationMessage>,
        userMessage: String,
        includeTools: Boolean
    ): Result<AiResponse> {
        val url = "https://generativelanguage.googleapis.com/v1beta/models/$modelName:generateContent?key=$apiKey"

        val rootJson = JSONObject()

        // Fast generation configuration for snappy conversational responses
        val genConfig = JSONObject()
            .put("temperature", 0.7)
            .put("topK", 40)
            .put("topP", 0.95)
            .put("maxOutputTokens", 500)
        rootJson.put("generationConfig", genConfig)

        // System Instruction
        val sysContent = JSONObject()
        val sysParts = JSONArray()
        sysParts.put(JSONObject().put("text", systemInstruction))
        sysContent.put("parts", sysParts)
        rootJson.put("systemInstruction", sysContent)

        // Contents (Recent History + User Message) - 6 messages is optimal for context & fast upload
        val contentsArray = JSONArray()
        val recentHistory = history.takeLast(6)
        for (msg in recentHistory) {
            val cObj = JSONObject()
            cObj.put("role", if (msg.role == "assistant") "model" else "user")
            val pArr = JSONArray()
            pArr.put(JSONObject().put("text", msg.content))
            cObj.put("parts", pArr)
            contentsArray.put(cObj)
        }

        // Add current user message
        val currentObj = JSONObject()
        currentObj.put("role", "user")
        val currentParts = JSONArray()
        currentParts.put(JSONObject().put("text", userMessage))
        currentObj.put("parts", currentParts)
        contentsArray.put(currentObj)
        rootJson.put("contents", contentsArray)

        // Tools / Function Declarations
        if (includeTools) {
            val toolsArray = JSONArray()
            val funcDeclObj = JSONObject()
            val funcDeclarations = JSONArray()

            // 1. create_reminder
            funcDeclarations.put(
                JSONObject()
                    .put("name", "create_reminder")
                    .put("description", "Save a reminder or task for the student")
                    .put(
                        "parameters", JSONObject()
                            .put("type", "OBJECT")
                            .put(
                                "properties", JSONObject()
                                    .put("text", JSONObject().put("type", "STRING").put("description", "The task or reminder description"))
                                    .put("datetime", JSONObject().put("type", "STRING").put("description", "When to remind, e.g., 'Today 6 PM', 'Tomorrow 10 AM'"))
                            )
                            .put("required", JSONArray().put("text").put("datetime"))
                    )
            )

            // 2. get_reminders
            funcDeclarations.put(
                JSONObject()
                    .put("name", "get_reminders")
                    .put("description", "Get upcoming reminders and tasks list")
                    .put(
                        "parameters", JSONObject()
                            .put("type", "OBJECT")
                            .put("properties", JSONObject())
                    )
            )

            // 3. save_note
            funcDeclarations.put(
                JSONObject()
                    .put("name", "save_note")
                    .put("description", "Quickly save an important note, code snippet, study note, or anime observation")
                    .put(
                        "parameters", JSONObject()
                            .put("type", "OBJECT")
                            .put(
                                "properties", JSONObject()
                                    .put("text", JSONObject().put("type", "STRING").put("description", "The note content to store"))
                            )
                            .put("required", JSONArray().put("text"))
                    )
            )

            // 4. youtube_idea_brainstorm
            funcDeclarations.put(
                JSONObject()
                    .put("name", "youtube_idea_brainstorm")
                    .put("description", "Brainstorm viral YouTube video ideas for an anime channel based on a topic or anime title")
                    .put(
                        "parameters", JSONObject()
                            .put("type", "OBJECT")
                            .put(
                                "properties", JSONObject()
                                    .put("topic", JSONObject().put("type", "STRING").put("description", "Anime name, theme, or topic (e.g. Jujutsu Kaisen, Solo Leveling, Top 10 fights)"))
                            )
                            .put("required", JSONArray().put("topic"))
                    )
            )

            // 5. log_mood
            funcDeclarations.put(
                JSONObject()
                    .put("name", "log_mood")
                    .put("description", "Log the student's emotional state or mood with an optional personal note")
                    .put(
                        "parameters", JSONObject()
                            .put("type", "OBJECT")
                            .put(
                                "properties", JSONObject()
                                    .put("mood", JSONObject().put("type", "STRING").put("description", "Mood name: Happy, Chill, Motivated, Stressed, Tired, or Sad"))
                                    .put("note", JSONObject().put("type", "STRING").put("description", "Why they feel this way or brief context"))
                            )
                            .put("required", JSONArray().put("mood"))
                    )
            )

            funcDeclObj.put("functionDeclarations", funcDeclarations)
            toolsArray.put(funcDeclObj)
            rootJson.put("tools", toolsArray)
        }

        val requestBody = rootJson.toString().toRequestBody(jsonMediaType)
        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()

        return client.newCall(request).execute().use { response ->
            val responseBody = response.body?.string() ?: ""
            if (!response.isSuccessful) {
                return Result.failure(Exception("Gemini API error (${response.code}): $responseBody"))
            }

            val respJson = JSONObject(responseBody)
            val candidates = respJson.optJSONArray("candidates")
            if (candidates == null || candidates.length() == 0) {
                return Result.failure(Exception("No response generated by Gemini."))
            }

            val candidate = candidates.getJSONObject(0)
            val content = candidate.optJSONObject("content")
            val parts = content?.optJSONArray("parts")

            val toolCalls = mutableListOf<ToolCall>()
            val textBuilder = StringBuilder()

            if (parts != null) {
                for (i in 0 until parts.length()) {
                    val part = parts.getJSONObject(i)
                    if (part.has("text")) {
                        textBuilder.append(part.getString("text"))
                    }
                    if (part.has("functionCall")) {
                        val fc = part.getJSONObject("functionCall")
                        val funcName = fc.getString("name")
                        val argsObj = fc.optJSONObject("args")
                        val argsMap = mutableMapOf<String, String>()
                        if (argsObj != null) {
                            val keys = argsObj.keys()
                            while (keys.hasNext()) {
                                val key = keys.next()
                                argsMap[key] = argsObj.optString(key, "")
                            }
                        }
                        toolCalls.add(ToolCall(funcName, argsMap))
                    }
                }
            }

            val resultText = textBuilder.toString().trim()
            Result.success(AiResponse(resultText, toolCalls))
        }
    }

    private fun callGroq(
        apiKey: String,
        model: String,
        systemInstruction: String,
        history: List<ConversationMessage>,
        userMessage: String
    ): Result<AiResponse> {
        val resolvedModel = if (model.isBlank()) "llama-3.3-70b-versatile" else model
        val url = "https://api.groq.com/openai/v1/chat/completions"

        val rootJson = JSONObject()
        rootJson.put("model", resolvedModel)

        val messagesArray = JSONArray()
        // System message
        messagesArray.put(JSONObject().put("role", "system").put("content", systemInstruction))

        // History
        val recentHistory = history.takeLast(10)
        for (msg in recentHistory) {
            messagesArray.put(JSONObject().put("role", msg.role).put("content", msg.content))
        }

        // Current user message
        messagesArray.put(JSONObject().put("role", "user").put("content", userMessage))
        rootJson.put("messages", messagesArray)

        // Tools for Groq
        val toolsArray = JSONArray()
        val toolDefs = listOf(
            Triple("create_reminder", "Save a reminder or task for the student", listOf("text" to "STRING", "datetime" to "STRING")),
            Triple("get_reminders", "Get upcoming reminders and tasks list", emptyList()),
            Triple("save_note", "Quickly save an important note or anime observation", listOf("text" to "STRING")),
            Triple("youtube_idea_brainstorm", "Brainstorm viral YouTube video ideas for an anime channel", listOf("topic" to "STRING")),
            Triple("log_mood", "Log emotional state (Happy, Chill, Motivated, Stressed, Tired, Sad)", listOf("mood" to "STRING", "note" to "STRING"))
        )

        for ((name, desc, params) in toolDefs) {
            val toolObj = JSONObject().put("type", "function")
            val funcObj = JSONObject().put("name", name).put("description", desc)
            val paramsObj = JSONObject().put("type", "object")
            val propsObj = JSONObject()
            val reqArr = JSONArray()
            for ((pName, pType) in params) {
                propsObj.put(pName, JSONObject().put("type", pType.lowercase()))
                reqArr.put(pName)
            }
            paramsObj.put("properties", propsObj)
            if (reqArr.length() > 0) paramsObj.put("required", reqArr)
            funcObj.put("parameters", paramsObj)
            toolObj.put("function", funcObj)
            toolsArray.put(toolObj)
        }
        rootJson.put("tools", toolsArray)

        val requestBody = rootJson.toString().toRequestBody(jsonMediaType)
        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $apiKey")
            .post(requestBody)
            .build()

        return client.newCall(request).execute().use { response ->
            val responseBody = response.body?.string() ?: ""
            if (!response.isSuccessful) {
                return Result.failure(Exception("Groq API error (${response.code}): $responseBody"))
            }

            val respJson = JSONObject(responseBody)
            val choices = respJson.optJSONArray("choices")
            if (choices == null || choices.length() == 0) {
                return Result.failure(Exception("No choices returned from Groq."))
            }

            val choice = choices.getJSONObject(0)
            val messageObj = choice.optJSONObject("message")
            val content = messageObj?.optString("content", "") ?: ""

            val toolCalls = mutableListOf<ToolCall>()
            val groqToolCalls = messageObj?.optJSONArray("tool_calls")
            if (groqToolCalls != null) {
                for (i in 0 until groqToolCalls.length()) {
                    val tc = groqToolCalls.getJSONObject(i)
                    val func = tc.optJSONObject("function")
                    if (func != null) {
                        val fnName = func.getString("name")
                        val argsRaw = func.optString("arguments", "{}")
                        val argsMap = mutableMapOf<String, String>()
                        runCatching {
                            val argsObj = JSONObject(argsRaw)
                            val keys = argsObj.keys()
                            while (keys.hasNext()) {
                                val k = keys.next()
                                argsMap[k] = argsObj.optString(k, "")
                            }
                        }
                        toolCalls.add(ToolCall(fnName, argsMap))
                    }
                }
            }

            Result.success(AiResponse(content.trim(), toolCalls))
        }
    }
}
