package com.nyayasetu.data

import com.nyayasetu.domain.model.MappingRecord
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MapperResultStore @Inject constructor() {
    private val byId = ConcurrentHashMap<Int, MappingRecord>()

    fun setResults(records: List<MappingRecord>) {
        records.forEach { byId[it.id] = it }
    }

    fun get(recordId: Int): MappingRecord? = byId[recordId]
}
