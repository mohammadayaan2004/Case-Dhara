package com.casedhara.ui.screens.mapper

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.casedhara.data.MapperResultStore
import com.casedhara.data.repository.BookmarkAddResult
import com.casedhara.data.repository.BookmarkRepository
import com.casedhara.data.repository.MapperRepository
import com.casedhara.domain.model.Bookmark
import com.casedhara.domain.model.MappingRecord
import com.casedhara.domain.model.MapperResponse
import com.casedhara.util.NetworkResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class BookmarkResult {
    data object Added : BookmarkResult()
    data object AlreadyExists : BookmarkResult()
}

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

    val detectedLaw: StateFlow<String?> = _query
        .map { detectLawFromQuery(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val bookmarks: StateFlow<List<Bookmark>> = bookmarkRepository.observeBookmarks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _bookmarkEvent = MutableSharedFlow<BookmarkResult>(extraBufferCapacity = 1)
    val bookmarkEvent: SharedFlow<BookmarkResult> = _bookmarkEvent.asSharedFlow()

    val openDetailOnResult: Boolean =
        savedStateHandle.get<Boolean>("openDetail") == true

    private var autoOpenDetailConsumed = false

    fun shouldAutoOpenDetail(): Boolean =
        openDetailOnResult && !autoOpenDetailConsumed

    fun markAutoOpenDetailConsumed() {
        autoOpenDetailConsumed = true
    }

    init {
        val initialQuery = savedStateHandle.get<String>("query")
        if (!initialQuery.isNullOrBlank()) {
            _query.value = initialQuery
            search()
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

    fun onQueryChange(value: String) {
        _query.value = value
    }

    fun onLawFilterChange(filter: String?) {
        _lawFilter.value = filter
    }

    fun search() {
        val q = _query.value.trim()
        if (q.isBlank()) return
        // Law filter chip takes highest priority; fall back to detecting from query text.
        // This prevents e.g. "ipc 44" from being sent as law=ipc to the backend while a
        // BNS chip is selected, and also ensures "ipc 44" consistently sends law=ipc.
        val chipFilter = _lawFilter.value
        val detectedFromQuery = detectLawFromQuery(q)
        val effectiveLaw = chipFilter ?: detectedFromQuery
        viewModelScope.launch {
            repository.search(q, topK = 8, law = effectiveLaw).collect { result ->
                _state.value = when {
                    // Client-side filter: when chip says IPC-only, drop cards that have no IPC section.
                    // When chip says BNS-only, drop cards that have no BNS section.
                    result is NetworkResult.Success && chipFilter != null -> {
                        val filtered = result.data.results.filter { record ->
                            when (chipFilter) {
                                "ipc" -> record.ipcSection.isNotBlank()
                                "bns" -> record.bnsSection.isNotBlank()
                                else  -> true
                            }
                        }
                        NetworkResult.Success(result.data.copy(results = filtered))
                    }
                    else -> result
                }
                val finalState = _state.value
                if (finalState is NetworkResult.Success) {
                    resultStore.setResults(finalState.data.results)
                }
            }
        }
    }

    fun clearState() {
        _state.value = NetworkResult.Idle
    }

    fun isBookmarked(record: MappingRecord): Boolean =
        bookmarks.value.any {
            it.ipcSection == record.ipcSection && it.bnsSection == record.bnsSection
        }

    fun addBookmark(record: MappingRecord) {
        viewModelScope.launch {
            val result = when (bookmarkRepository.addFromMapping(record)) {
                BookmarkAddResult.Added -> BookmarkResult.Added
                BookmarkAddResult.AlreadyExists -> BookmarkResult.AlreadyExists
            }
            _bookmarkEvent.emit(result)
        }
    }
}