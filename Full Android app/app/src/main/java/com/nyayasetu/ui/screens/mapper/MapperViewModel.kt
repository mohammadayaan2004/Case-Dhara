package com.nyayasetu.ui.screens.mapper

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nyayasetu.data.MapperResultStore
import com.nyayasetu.data.repository.BookmarkRepository
import com.nyayasetu.data.repository.MapperRepository
import com.nyayasetu.domain.model.MappingRecord
import com.nyayasetu.domain.model.MapperResponse
import com.nyayasetu.util.NetworkResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MapperViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val repository: MapperRepository,
    private val resultStore: MapperResultStore,
    private val bookmarkRepository: BookmarkRepository,
) : ViewModel() {

    private val _state = MutableStateFlow<NetworkResult<MapperResponse>>(NetworkResult.Idle)
    val state: StateFlow<NetworkResult<MapperResponse>> = _state.asStateFlow()

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _lawFilter = MutableStateFlow<String?>(null)
    val lawFilter: StateFlow<String?> = _lawFilter.asStateFlow()

    init {
        val initialQuery = savedStateHandle.get<String>("query")
        if (!initialQuery.isNullOrBlank()) {
            _query.value = initialQuery
            search()
        }
    }

    fun onQueryChange(value: String) {
        _query.value = value
    }

    fun onLawFilterChange(filter: String?) {
        _lawFilter.value = filter
    }

    fun search() {
        val q = _query.value.trim()
        if (q.isBlank()) return
        viewModelScope.launch {
            repository.search(q, topK = 8, law = _lawFilter.value).collect { result ->
                _state.value = result
                if (result is NetworkResult.Success) {
                    resultStore.setResults(result.data.results)
                }
            }
        }
    }

    fun clearState() {
        _state.value = NetworkResult.Idle
    }

    fun addBookmark(record: MappingRecord) {
        viewModelScope.launch {
            bookmarkRepository.addFromMapping(record)
        }
    }
}