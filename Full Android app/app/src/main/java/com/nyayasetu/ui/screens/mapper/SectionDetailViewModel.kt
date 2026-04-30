package com.nyayasetu.ui.screens.mapper

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.nyayasetu.data.MapperResultStore
import com.nyayasetu.domain.model.MappingRecord
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SectionDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val resultStore: MapperResultStore,
) : ViewModel() {

    private val recordId: Int = checkNotNull(savedStateHandle.get<Int>("recordId"))

    val record: MappingRecord?
        get() = resultStore.get(recordId)
}
