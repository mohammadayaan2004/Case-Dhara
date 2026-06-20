package com.casedhara.ui.screens.chatbot

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

/**
 * Manages Android SpeechRecognizer (STT) and TextToSpeech (TTS) for the chatbot.
 *
 * Usage:
 *   val manager = VoiceAssistantManager(context)
 *   manager.startListening()          // begin mic recording
 *   manager.stopListening()           // force-stop before end of speech
 *   manager.speak("Hello")            // TTS playback
 *   manager.stopSpeaking()            // interrupt TTS
 *   manager.release()                 // call in onCleared() / DisposableEffect
 */
class VoiceAssistantManager(private val context: Context) {

    // ── Voice state exposed to UI ─────────────────────────────────────────
    sealed interface VoiceState {
        object Idle : VoiceState
        object Listening : VoiceState
        object Processing : VoiceState   // recognised text being sent to AI
        object Speaking : VoiceState     // TTS playing back AI reply
        data class Error(val message: String) : VoiceState
    }

    private val _voiceState = MutableStateFlow<VoiceState>(VoiceState.Idle)
    val voiceState: StateFlow<VoiceState> = _voiceState.asStateFlow()

    /** Emits the text recognised from speech (call-site inserts into chat). */
    private val _recognisedText = MutableStateFlow<String?>(null)
    val recognisedText: StateFlow<String?> = _recognisedText.asStateFlow()

    // ── SpeechRecognizer ─────────────────────────────────────────────────
    private var speechRecognizer: SpeechRecognizer? = null

    private val recognizerIntent: Intent
        get() = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "en-IN")  // Indian English preferred
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }

    private val recognitionListener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
            _voiceState.value = VoiceState.Listening
        }
        override fun onBeginningOfSpeech() {}
        override fun onRmsChanged(rmsdB: Float) {}
        override fun onBufferReceived(buffer: ByteArray?) {}
        override fun onEndOfSpeech() {
            _voiceState.value = VoiceState.Processing
        }
        override fun onError(error: Int) {
            val msg = when (error) {
                SpeechRecognizer.ERROR_AUDIO            -> "Audio recording error"
                SpeechRecognizer.ERROR_CLIENT           -> "Client side error"
                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission missing"
                SpeechRecognizer.ERROR_NETWORK          -> "Network error"
                SpeechRecognizer.ERROR_NETWORK_TIMEOUT  -> "Network timeout"
                SpeechRecognizer.ERROR_NO_MATCH         -> "No speech recognised — try again"
                SpeechRecognizer.ERROR_RECOGNIZER_BUSY  -> "Recogniser busy"
                SpeechRecognizer.ERROR_SERVER           -> "Server error"
                SpeechRecognizer.ERROR_SPEECH_TIMEOUT   -> "No speech detected"
                else                                    -> "Unknown error ($error)"
            }
            _voiceState.value = VoiceState.Error(msg)
        }
        override fun onResults(results: Bundle?) {
            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            val text = matches?.firstOrNull()
            if (!text.isNullOrBlank()) {
                _recognisedText.value = text
            }
            _voiceState.value = VoiceState.Idle
        }
        override fun onPartialResults(partialResults: Bundle?) {}
        override fun onEvent(eventType: Int, params: Bundle?) {}
    }

    // ── TextToSpeech ─────────────────────────────────────────────────────
    private var tts: TextToSpeech? = null
    private var ttsReady = false

    init {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.getDefault()
                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        _voiceState.value = VoiceState.Speaking
                    }
                    override fun onDone(utteranceId: String?) {
                        _voiceState.value = VoiceState.Idle
                    }
                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) {
                        _voiceState.value = VoiceState.Idle
                    }
                })
                ttsReady = true
            }
        }
    }

    // ── Public API ────────────────────────────────────────────────────────

    /** Returns true if the device supports speech recognition. */
    fun isSpeechAvailable(): Boolean =
        SpeechRecognizer.isRecognitionAvailable(context)

    /** Start microphone and STT. Call only after RECORD_AUDIO permission granted. */
    fun startListening() {
        stopSpeaking()
        if (speechRecognizer == null) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(recognitionListener)
            }
        }
        _voiceState.value = VoiceState.Listening
        speechRecognizer?.startListening(recognizerIntent)
    }

    /** Cancel current STT session. */
    fun stopListening() {
        speechRecognizer?.cancel()
        _voiceState.value = VoiceState.Idle
    }

    /** Speak [text] via TTS. Interrupts any current speech. */
    fun speak(text: String) {
        if (!ttsReady) return
        tts?.speak(
            text,
            TextToSpeech.QUEUE_FLUSH,
            null,
            "utterance_${System.currentTimeMillis()}"
        )
    }

    /** Interrupt TTS playback. */
    fun stopSpeaking() {
        tts?.stop()
        if (_voiceState.value == VoiceState.Speaking) {
            _voiceState.value = VoiceState.Idle
        }
    }

    /** Consume the latest recognised text (so it isn't replayed). */
    fun consumeRecognisedText() {
        _recognisedText.value = null
    }

    /** Release all resources — call from ViewModel.onCleared() or DisposableEffect. */
    fun release() {
        speechRecognizer?.destroy()
        speechRecognizer = null
        tts?.shutdown()
        tts = null
    }
}
