package com.nyayasetu.ui.screens.summarizer

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nyayasetu.data.repository.SummarizerRepository
import com.nyayasetu.domain.model.CaseSummary
import com.nyayasetu.util.NetworkResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SummarizerViewModel @Inject constructor(
    private val repository: SummarizerRepository,
) : ViewModel() {

    private val _state = MutableStateFlow<NetworkResult<CaseSummary>>(NetworkResult.Idle)
    val state: StateFlow<NetworkResult<CaseSummary>> = _state.asStateFlow()

    private val _selectedFileName = MutableStateFlow<String?>(null)
    val selectedFileName: StateFlow<String?> = _selectedFileName.asStateFlow()

    private val _pastedText = MutableStateFlow("")
    val pastedText: StateFlow<String> = _pastedText.asStateFlow()

    fun onPastedTextChange(value: String) {
        _pastedText.value = value
    }

    fun summarizePdf(context: Context, uri: Uri) {
        viewModelScope.launch {
            _state.value = NetworkResult.Loading
            _selectedFileName.value = uri.lastPathSegment
            try {
                val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                    ?: throw IllegalStateException("Cannot read file")
                val name = uri.lastPathSegment ?: "judgment.pdf"
                repository.summarizePdf(bytes, name).collect { _state.value = it }
            } catch (e: Exception) {
                _state.value = NetworkResult.Error(e.message ?: "Failed to read PDF")
            }
        }
    }

    fun summarizeText() {
        val text = _pastedText.value.trim()
        if (text.length < 100) {
            _state.value = NetworkResult.Error("Please enter at least 100 characters to summarize.")
            return
        }
        viewModelScope.launch {
            repository.summarizeText(text).collect { _state.value = it }
        }
    }

    fun reset() {
        _state.value = NetworkResult.Idle
        _selectedFileName.value = null
    }
}
