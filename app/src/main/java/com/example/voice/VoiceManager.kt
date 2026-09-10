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
                // Try Urdu or Hindi locale first for authentic Hinglish/Urdu cadence, fallback to English
                val urduLocale = Locale.forLanguageTag("ur-PK")
                val hindiLocale = Locale.forLanguageTag("hi-IN")
                val available = textToSpeech?.isLanguageAvailable(urduLocale)
                if (available == TextToSpeech.LANG_AVAILABLE || available == TextToSpeech.LANG_COUNTRY_AVAILABLE) {
                    textToSpeech?.language = urduLocale
                } else if (textToSpeech?.isLanguageAvailable(hindiLocale) == TextToSpeech.LANG_AVAILABLE) {
                    textToSpeech?.language = hindiLocale
                } else {
                    textToSpeech?.language = Locale.ENGLISH
                }
                textToSpeech?.setSpeechRate(speechRate)
                textToSpeech?.setPitch(speechPitch)
                isTtsReady = true

                textToSpeech?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        _voiceState.value = VoiceState.SPEAKING
                    }

                    override fun onDone(utteranceId: String?) {
                        if (_voiceState.value == VoiceState.SPEAKING) {
                            _voiceState.value = VoiceState.IDLE
                        }
                        // Continuous screen-off loop: automatically resume listening after speaking
                        if (isContinuousSessionActive && isScreenOffModeEnabled) {
                            mainHandler.postDelayed({
                                if (isContinuousSessionActive && _voiceState.value == VoiceState.IDLE) {
                                    startListening()
                                }
                            }, 450)
                        }
                    }

                    override fun onError(utteranceId: String?) {
                        if (_voiceState.value == VoiceState.SPEAKING) {
                            _voiceState.value = VoiceState.IDLE
                        }
                        if (isContinuousSessionActive && isScreenOffModeEnabled) {
                            mainHandler.postDelayed({
                                if (isContinuousSessionActive && _voiceState.value == VoiceState.IDLE) {
                                    startListening()
                                }
                            }, 500)
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
            speechRecognizer?.destroy()
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
                        // If continuous screen-off mode is enabled and it's a silence timeout / no-match, auto-resume
                        if (isContinuousSessionActive && isScreenOffModeEnabled &&
                            (error == SpeechRecognizer.ERROR_NO_MATCH || error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT)
                        ) {
                            mainHandler.postDelayed({
                                if (isContinuousSessionActive && _voiceState.value == VoiceState.IDLE) {
                                    startListening()
                                }
                            }, 650)
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
                            if (isContinuousSessionActive && isScreenOffModeEnabled) {
                                mainHandler.postDelayed({
                                    if (isContinuousSessionActive && _voiceState.value == VoiceState.IDLE) {
                                        startListening()
                                    }
                                }, 500)
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
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ur-PK")
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "ur-PK")
                putExtra("android.speech.extra.EXTRA_ADDITIONAL_LANGUAGES", arrayOf("en-US", "hi-IN"))
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
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
            speechRecognizer?.stopListening()
            _audioLevel.value = 0f
            if (_voiceState.value == VoiceState.LISTENING) {
                _voiceState.value = VoiceState.IDLE
            }
        } catch (e: Exception) {
            Log.e("VoiceManager", "Error stopping listening", e)
        }
    }

    fun speak(text: String) {
        if (!isTtsReady || textToSpeech == null) {
            Log.w("VoiceManager", "TTS not ready")
            return
        }

        // Clean any markdown symbols like asterisks or hashtags from spoken text
        val cleanedText = text
            .replace(Regex("""[*#_`~>\[\]]"""), "")
            .replace(Regex("""\s+"""), " ")
            .trim()

        if (cleanedText.isBlank()) return

        stopListening()
        acquireWakeLock()
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

