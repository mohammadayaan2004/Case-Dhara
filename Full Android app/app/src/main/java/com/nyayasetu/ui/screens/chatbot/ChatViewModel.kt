package com.nyayasetu.ui.screens.chatbot

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nyayasetu.data.repository.ChatRepository
import com.nyayasetu.domain.model.ChatMessage
import com.nyayasetu.util.NetworkResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.MutableSharedFlow
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
) : ViewModel() {

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _inputText = MutableStateFlow("")
    val inputText: StateFlow<String> = _inputText.asStateFlow()

    private val _errorEvents = MutableSharedFlow<String>()
    val errorEvents: SharedFlow<String> = _errorEvents.asSharedFlow()

    fun onInputChange(value: String) {
        _inputText.value = value
    }

    fun sendMessage() {
        val question = _inputText.value.trim()
        if (question.isBlank() || _isLoading.value) return
        val userMessage = ChatMessage(role = "user", content = question)
        _messages.update { it + userMessage }
        _inputText.value = ""
        _isLoading.value = true
        viewModelScope.launch {
            val historyForApi = _messages.value.dropLast(1)
            repository.sendMessage(question, historyForApi).collect { result ->
                when (result) {
                    is NetworkResult.Success -> {
                        val assistant = ChatMessage(role = "assistant", content = result.data.answer)
                        _messages.update { it + assistant }
                        _isLoading.value = false
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

    fun clearHistory() {
        _messages.value = emptyList()
    }
}
