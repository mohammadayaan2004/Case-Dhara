package com.casedhara.ui.screens.summarizer

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.casedhara.data.local.dao.SavedCaseDao
import com.casedhara.data.local.entity.SavedCaseEntity
import com.casedhara.data.repository.SummarizerRepository
import com.casedhara.domain.model.CaseSummary
import com.casedhara.util.CaseSummaryJson
import com.casedhara.util.NetworkResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SummarizerViewModel @Inject constructor(
    private val repository: SummarizerRepository,
    private val savedCaseDao: SavedCaseDao,
) : ViewModel() {

    private val _state = MutableStateFlow<NetworkResult<CaseSummary>>(NetworkResult.Idle)
    val state: StateFlow<NetworkResult<CaseSummary>> = _state.asStateFlow()

    private val _selectedFileName = MutableStateFlow<String?>(null)
    val selectedFileName: StateFlow<String?> = _selectedFileName.asStateFlow()

    private val _pastedText = MutableStateFlow("")
    val pastedText: StateFlow<String> = _pastedText.asStateFlow()

    val savedCases: StateFlow<List<SavedCaseEntity>> = savedCaseDao.getAllFlow()
        .map { list -> list.filter { it.source == "case_summarizer" } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun onPastedTextChange(value: String) {
        _pastedText.value = value
    }

    fun summarizePdf(context: Context, uri: Uri) {
        viewModelScope.launch {
            _state.value = NetworkResult.Loading
            val resolvedName: String =
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (idx != -1 && cursor.moveToFirst()) cursor.getString(idx) else null
                } ?: "judgment.pdf"
            val safeName = if (resolvedName.endsWith(".pdf", ignoreCase = true))
                resolvedName else "$resolvedName.pdf"
            _selectedFileName.value = safeName
            try {
                val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                    ?: throw IllegalStateException("Cannot read file")
                repository.summarizePdf(bytes, safeName).collect { _state.value = it }
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
        _state.value = NetworkResult.Loading
        viewModelScope.launch {
            repository.summarizeText(text).collect { _state.value = it }
        }
    }

    fun saveCase(summary: CaseSummary) {
        viewModelScope.launch {
            val title = _selectedFileName.value?.takeIf { it.isNotBlank() }
                ?: summary.caseTitle
            val json = CaseSummaryJson.toJson(summary)
            savedCaseDao.insert(
                SavedCaseEntity(
                    title = title,
                    source = "case_summarizer",
                    summaryJson = json,
                ),
            )
        }
    }

    fun reset() {
        _state.value = NetworkResult.Idle
        _selectedFileName.value = null
    }
}
