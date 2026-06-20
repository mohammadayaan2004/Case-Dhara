package com.casedhara.ui.screens.chatbot

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.casedhara.data.repository.ChatRepository
import com.casedhara.domain.model.ChatMessage
import com.casedhara.util.NetworkResult
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val repository: ChatRepository,
    @ApplicationContext private val appContext: Context,
) : ViewModel() {

    // ── Chat state ────────────────────────────────────────────────────────
    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _inputText = MutableStateFlow("")
    val inputText: StateFlow<String> = _inputText.asStateFlow()

    private val _errorEvents = MutableSharedFlow<String>()
    val errorEvents: SharedFlow<String> = _errorEvents.asSharedFlow()

    /** True when the pending/last question was submitted via voice (mic), false for typed input. */
    private var lastInputWasVoice = false

    // ── Voice assistant ───────────────────────────────────────────────────
    val voiceManager = VoiceAssistantManager(appContext)

    /** Expose voice state directly from manager. */
    val voiceState = voiceManager.voiceState
    val recognisedText = voiceManager.recognisedText

    // ── Text input ────────────────────────────────────────────────────────
    fun onInputChange(value: String) {
        _inputText.value = value
    }

    // ── Sending messages ──────────────────────────────────────────────────
    fun sendMessage() {
        val question = _inputText.value.trim()
        if (question.isBlank() || _isLoading.value) return
        lastInputWasVoice = false
        sendQuestion(question)
    }

    /** Called by the UI when STT produces recognised text. */
    fun sendVoiceMessage(text: String) {
        if (text.isBlank() || _isLoading.value) return
        lastInputWasVoice = true
        _inputText.value = text
        sendQuestion(text)
    }

    private fun sendQuestion(question: String) {
        val userMessage = ChatMessage(role = "user", content = question)
        _messages.update { it + userMessage }
        _inputText.value = ""
        _isLoading.value = true
        viewModelScope.launch {
            val historyForApi = _messages.value.dropLast(1)
            repository.sendMessage(question, historyForApi).collect { result ->
                when (result) {
                    is NetworkResult.Success -> {
                        val answer = result.data.answer
                        val assistant = ChatMessage(role = "assistant", content = answer)
                        _messages.update { it + assistant }
                        _isLoading.value = false
                        if (lastInputWasVoice) {
                            voiceManager.speak(answer)
                        }
                    }
                    is NetworkResult.Error -> {
                        _errorEvents.emit(result.message)
                        _isLoading.value = false
                    }
                    else -> Unit
                }
            }
        }
    }

    // ── Voice controls ────────────────────────────────────────────────────
    fun startVoiceInput() {
        voiceManager.startListening()
    }

    fun stopVoiceInput() {
        voiceManager.stopListening()
    }

    fun stopSpeaking() {
        voiceManager.stopSpeaking()
    }

    fun consumeRecognisedText() {
        voiceManager.consumeRecognisedText()
    }

    // ── Misc ──────────────────────────────────────────────────────────────
    fun clearHistory() {
        _messages.value = emptyList()
        voiceManager.stopSpeaking()
    }

    override fun onCleared() {
        super.onCleared()
        voiceManager.release()
    }
}
