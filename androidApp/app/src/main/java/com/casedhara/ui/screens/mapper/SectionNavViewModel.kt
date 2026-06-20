package com.casedhara.ui.screens.mapper

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.casedhara.data.MapperResultStore
import com.casedhara.data.repository.MapperRepository
import com.casedhara.domain.model.SectionReference
import com.casedhara.domain.model.mapperQuery
import com.casedhara.util.NetworkResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SectionNavViewModel @Inject constructor(
    private val mapperRepository: MapperRepository,
    private val resultStore: MapperResultStore,
) : ViewModel() {

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _navigateToRecord = MutableSharedFlow<Int>(extraBufferCapacity = 1)
    val navigateToRecord: SharedFlow<Int> = _navigateToRecord.asSharedFlow()

    private val _error = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val error: SharedFlow<String> = _error.asSharedFlow()

    fun openSection(section: SectionReference) {
        val query = section.mapperQuery().trim()
        if (query.isBlank()) {
            viewModelScope.launch { _error.emit("No section reference to look up") }
            return
        }
        viewModelScope.launch {
            _loading.value = true
            mapperRepository.search(query, topK = 8, law = detectLawFromQuery(query)).collect { result ->
                when (result) {
                    is NetworkResult.Loading -> _loading.value = true
                    is NetworkResult.Success -> {
                        _loading.value = false
                        val results = result.data.results
                        if (results.isNotEmpty()) {
                            resultStore.setResults(results)
                            _navigateToRecord.emit(results.first().id)
                        } else {
                            _error.emit("No matching section found for \"$query\"")
                        }
                        return@collect
                    }
                    is NetworkResult.Error -> {
                        _loading.value = false
                        _error.emit(result.message)
                        return@collect
                    }
                    is NetworkResult.Idle -> Unit
                }
            }
        }
    }

    private fun detectLawFromQuery(query: String): String? {
        val q = query.trim().uppercase()
        return when {
            q.contains("\\bIPC\\b".toRegex()) -> "ipc"
            q.contains("\\bBNS\\b".toRegex()) -> "bns"
            else -> null
        }
    }
}
