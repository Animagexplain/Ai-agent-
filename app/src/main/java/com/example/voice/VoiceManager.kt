package com.example.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

enum class VoiceState {
    IDLE,
    LISTENING,
    PROCESSING,
    SPEAKING,
    ERROR
}

class VoiceManager(
    private val context: Context,
    private val onSpeechRecognized: (String) -> Unit
) {
    private val _voiceState = MutableStateFlow(VoiceState.IDLE)
    val voiceState: StateFlow<VoiceState> = _voiceState.asStateFlow()

    private val _liveTranscript = MutableStateFlow("")
    val liveTranscript: StateFlow<String> = _liveTranscript.asStateFlow()

    private val _audioLevel = MutableStateFlow(0f) // 0.0 to 1.0 for visualizer
    val audioLevel: StateFlow<Float> = _audioLevel.asStateFlow()

    private var speechRecognizer: SpeechRecognizer? = null
    private var textToSpeech: TextToSpeech? = null
    private var isTtsReady = false

    private val mainHandler = Handler(Looper.getMainLooper())
    private var powerManager: PowerManager? = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
    private var wakeLock: PowerManager.WakeLock? = null

    var isScreenOffModeEnabled: Boolean = true
    var isContinuousSessionActive: Boolean = true

    var speechRate: Float = 1.05f
        set(value) {
            field = value
            textToSpeech?.setSpeechRate(value)
        }

    var speechPitch: Float = 1.0f
        set(value) {
            field = value
            textToSpeech?.setPitch(value)
        }

    init {
        initTts()
        acquireWakeLock()
    }

    private fun acquireWakeLock() {
        try {
            if (wakeLock == null) {
                wakeLock = powerManager?.newWakeLock(
                    PowerManager.PARTIAL_WAKE_LOCK,
                    "JarvisVoiceManager::WakeLock"
                )?.apply {
                    setReferenceCounted(false)
                }
            }
            if (wakeLock?.isHeld == false) {
                wakeLock?.acquire(3 * 60 * 60 * 1000L) // 3 hour safety timeout
            }
        } catch (e: Exception) {
            Log.e("VoiceManager", "Error acquiring WakeLock", e)
        }
    }

    private fun initTts() {
        textToSpeech = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                configureFemaleVoice()
                isTtsReady = true

                textToSpeech?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        _voiceState.value = VoiceState.SPEAKING
                    }

                    override fun onDone(utteranceId: String?) {
                        if (_voiceState.value == VoiceState.SPEAKING) {
                            _voiceState.value = VoiceState.IDLE
                        }
                        // Continuous conversational loop: automatically resume listening after speaking
                        if (isContinuousSessionActive) {
                            mainHandler.postDelayed({
                                if (isContinuousSessionActive && _voiceState.value == VoiceState.IDLE) {
                                    startListening()
                                }
                            }, 350)
                        }
                    }

                    override fun onError(utteranceId: String?) {
                        if (_voiceState.value == VoiceState.SPEAKING) {
                            _voiceState.value = VoiceState.IDLE
                        }
                        if (isContinuousSessionActive) {
                            mainHandler.postDelayed({
                                if (isContinuousSessionActive && _voiceState.value == VoiceState.IDLE) {
                                    startListening()
                                }
                            }, 400)
                        }
                    }
                })
            } else {
                Log.e("VoiceManager", "Failed to initialize TextToSpeech: status=$status")
            }
        }
    }

    fun startListening() {
        // If TTS is currently speaking, barge-in!
        stopSpeaking()
        acquireWakeLock()

        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            Log.w("VoiceManager", "Speech recognition not available on device")
            _voiceState.value = VoiceState.ERROR
            return
        }

        try {
            try {
                speechRecognizer?.cancel()
                speechRecognizer?.destroy()
            } catch (e: Exception) {
                // Ignore cleanup errors
            }

            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        _voiceState.value = VoiceState.LISTENING
                        _liveTranscript.value = ""
                    }

                    override fun onBeginningOfSpeech() {
                        // User started speaking -> barge in
                        stopSpeaking()
                        _voiceState.value = VoiceState.LISTENING
                    }

                    override fun onRmsChanged(rmsdB: Float) {
                        // Normalize dB (-2 to 10 typical) to 0.0 - 1.0 range
                        val normalized = ((rmsdB + 2f) / 12f).coerceIn(0.05f, 1.0f)
                        _audioLevel.value = normalized
                    }

                    override fun onBufferReceived(buffer: ByteArray?) {}

                    override fun onEndOfSpeech() {
                        _voiceState.value = VoiceState.PROCESSING
                        _audioLevel.value = 0f
                    }

                    override fun onError(error: Int) {
                        Log.w("VoiceManager", "Speech recognition error code: $error")
                        _audioLevel.value = 0f
                        if (_voiceState.value == VoiceState.LISTENING) {
                            _voiceState.value = VoiceState.IDLE
                        }

                        // Auto-rearm on speech pause, silence timeout, or recognizer busy when in active conversation
                        if (isContinuousSessionActive) {
                            when (error) {
                                SpeechRecognizer.ERROR_NO_MATCH,
                                SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> {
                                    mainHandler.postDelayed({
                                        if (isContinuousSessionActive && _voiceState.value == VoiceState.IDLE) {
                                            startListening()
                                        }
                                    }, 400)
                                }
                                SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> {
                                    try {
                                        speechRecognizer?.cancel()
                                    } catch (e: Exception) {}
                                    mainHandler.postDelayed({
                                        if (isContinuousSessionActive && _voiceState.value == VoiceState.IDLE) {
                                            startListening()
                                        }
                                    }, 500)
                                }
                            }
                        }
                    }

                    override fun onResults(results: Bundle?) {
                        _voiceState.value = VoiceState.PROCESSING
                        _audioLevel.value = 0f
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val text = matches?.firstOrNull()?.trim().orEmpty()
                        if (text.isNotBlank()) {
                            _liveTranscript.value = text
                            onSpeechRecognized(text)
                        } else {
                            _voiceState.value = VoiceState.IDLE
                            if (isContinuousSessionActive) {
                                mainHandler.postDelayed({
                                    if (isContinuousSessionActive && _voiceState.value == VoiceState.IDLE) {
                                        startListening()
                                    }
                                }, 350)
                            }
                        }
                    }

                    override fun onPartialResults(partialResults: Bundle?) {
                        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val partial = matches?.firstOrNull()?.trim().orEmpty()
                        if (partial.isNotBlank()) {
                            _liveTranscript.value = partial
                        }
                    }

                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })
            }

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                // en-IN is optimal for South Asian / Pakistani Hinglish accents
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-IN")
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "en-IN")
                putExtra("android.speech.extra.EXTRA_ADDITIONAL_LANGUAGES", arrayOf("ur-PK", "hi-IN", "en-US"))
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
            }

            speechRecognizer?.startListening(intent)
            _voiceState.value = VoiceState.LISTENING
        } catch (e: Exception) {
            Log.e("VoiceManager", "Error starting speech recognition", e)
            _voiceState.value = VoiceState.ERROR
        }
    }

    fun stopListening() {
        try {
            mainHandler.removeCallbacksAndMessages(null)
            try {
                speechRecognizer?.cancel()
            } catch (e: Exception) {}
            speechRecognizer?.stopListening()
            _audioLevel.value = 0f
            if (_voiceState.value == VoiceState.LISTENING) {
                _voiceState.value = VoiceState.IDLE
            }
        } catch (e: Exception) {
            Log.e("VoiceManager", "Error stopping listening", e)
        }
    }

    private fun configureFemaleVoice() {
        try {
            val tts = textToSpeech ?: return
            val inEnglishLocale = Locale.forLanguageTag("en-IN")
            val hiLocale = Locale.forLanguageTag("hi-IN")

            // Inspect available voices on the device
            val voices = tts.voices
            var chosenVoice: Voice? = null

            if (!voices.isNullOrEmpty()) {
                // Priority 1: Indian English or Hindi female voice (fluently speaks Roman Hinglish)
                chosenVoice = voices.firstOrNull { voice ->
                    val lang = voice.locale.language.lowercase()
                    val country = voice.locale.country.lowercase()
                    val name = voice.name.lowercase()
                    val isSouthAsian = (lang == "en" && country == "in") || (lang == "hi" && country == "in")
                    val isFemale = name.contains("female") || name.contains("#female") || name.contains("-f-") || name.contains("cxx") || name.contains("network")
                    isSouthAsian && isFemale
                } ?: voices.firstOrNull { voice ->
                    val lang = voice.locale.language.lowercase()
                    val country = voice.locale.country.lowercase()
                    val name = voice.name.lowercase()
                    val isSouthAsian = (lang == "en" && country == "in") || (lang == "hi" && country == "in")
                    isSouthAsian && !name.contains("male")
                } ?: voices.firstOrNull { voice ->
                    val lang = voice.locale.language.lowercase()
                    val country = voice.locale.country.lowercase()
                    lang == "en" && country == "in"
                }
            }

            if (chosenVoice != null) {
                tts.voice = chosenVoice
                tts.language = chosenVoice.locale
                Log.d("VoiceManager", "Selected native female voice: ${chosenVoice.name} (${chosenVoice.locale})")
            } else {
                // Fallback to Indian English which handles Hinglish/Roman Urdu phonetics naturally
                if (tts.isLanguageAvailable(inEnglishLocale) >= TextToSpeech.LANG_AVAILABLE) {
                    tts.language = inEnglishLocale
                } else if (tts.isLanguageAvailable(hiLocale) >= TextToSpeech.LANG_AVAILABLE) {
                    tts.language = hiLocale
                } else {
                    tts.language = Locale.ENGLISH
                }
            }

            // Natural female pitch (1.15f - 1.25f) and conversational pace (1.05f)
            val effectivePitch = speechPitch.coerceAtLeast(1.15f)
            val effectiveRate = speechRate.coerceAtLeast(1.05f)
            tts.setPitch(effectivePitch)
            tts.setSpeechRate(effectiveRate)
        } catch (e: Exception) {
            Log.e("VoiceManager", "Error configuring female voice", e)
            textToSpeech?.language = Locale.forLanguageTag("en-IN")
        }
    }

    fun speak(text: String) {
        if (!isTtsReady || textToSpeech == null) {
            Log.w("VoiceManager", "TTS not ready")
            return
        }

        // Clean emojis, symbols, and markdown so TTS reads smoothly like a real person
        val cleanedText = text
            // Strip emojis (symbols, pictographs, flags, surrogates)
            .replace(Regex("[\\p{So}\\p{Cn}\\p{Sk}\\p{Cs}\\uFE00-\\uFE0F\\uD83C-\\uDBFF\\uDC00-\\uDFFF]"), "")
            // Strip markdown asterisks, hashtags, underscores, brackets
            .replace(Regex("""[*#_`~>\[\]()|{}–—\\]"""), " ")
            // Normalize spaces
            .replace(Regex("""\s+"""), " ")
            .trim()

        if (cleanedText.isBlank()) return

        stopListening()
        acquireWakeLock()
        configureFemaleVoice()
        _voiceState.value = VoiceState.SPEAKING
        val params = Bundle().apply {
            putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "jarvis_reply_${System.currentTimeMillis()}")
        }
        textToSpeech?.speak(cleanedText, TextToSpeech.QUEUE_FLUSH, params, "jarvis_reply")
    }

    /**
     * Immediate barge-in interrupt: instantly stops voice playback
     */
    fun stopSpeaking() {
        try {
            if (textToSpeech?.isSpeaking == true) {
                textToSpeech?.stop()
            }
            if (_voiceState.value == VoiceState.SPEAKING) {
                _voiceState.value = VoiceState.IDLE
            }
        } catch (e: Exception) {
            Log.e("VoiceManager", "Error in stopSpeaking", e)
        }
    }

    fun pauseAll() {
        isContinuousSessionActive = false
        mainHandler.removeCallbacksAndMessages(null)
        stopSpeaking()
        stopListening()
        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
            }
        } catch (e: Exception) {
            Log.e("VoiceManager", "Error releasing WakeLock on pause", e)
        }
    }

    fun resumeContinuousMode() {
        isContinuousSessionActive = true
        startListening()
    }

    fun setVoiceState(state: VoiceState) {
        _voiceState.value = state
    }

    fun release() {
        try {
            pauseAll()
            speechRecognizer?.destroy()
            speechRecognizer = null
            textToSpeech?.stop()
            textToSpeech?.shutdown()
            textToSpeech = null
        } catch (e: Exception) {
            Log.e("VoiceManager", "Error releasing voice manager", e)
        }
    }
}

