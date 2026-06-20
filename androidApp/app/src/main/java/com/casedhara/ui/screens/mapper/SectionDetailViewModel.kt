package com.casedhara.ui.screens.mapper

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.casedhara.data.MapperResultStore
import com.casedhara.data.repository.BookmarkAddResult
import com.casedhara.data.repository.BookmarkRepository
import com.casedhara.domain.model.Bookmark
import com.casedhara.domain.model.MappingRecord
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SectionDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val resultStore: MapperResultStore,
    private val bookmarkRepository: BookmarkRepository,
) : ViewModel() {

    private val recordId: Int = checkNotNull(savedStateHandle.get<Int>("recordId"))

    val record: MappingRecord?
        get() = resultStore.get(recordId)

    val bookmarks: StateFlow<List<Bookmark>> = bookmarkRepository.observeBookmarks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val isBookmarked: StateFlow<Boolean> = bookmarks.map { list ->
        record?.let { r -> list.any { it.ipcSection == r.ipcSection && it.bnsSection == r.bnsSection } } ?: false
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    private val _bookmarkEvent = MutableSharedFlow<BookmarkResult>(extraBufferCapacity = 1)
    val bookmarkEvent: SharedFlow<BookmarkResult> = _bookmarkEvent.asSharedFlow()

    fun toggleBookmark() {
        val r = record ?: return
        viewModelScope.launch {
            val result = when (bookmarkRepository.addFromMapping(r)) {
                BookmarkAddResult.Added -> BookmarkResult.Added
                BookmarkAddResult.AlreadyExists -> BookmarkResult.AlreadyExists
            }
            _bookmarkEvent.emit(result)
        }
    }
}
