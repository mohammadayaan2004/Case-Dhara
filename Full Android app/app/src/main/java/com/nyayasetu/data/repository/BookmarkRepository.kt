package com.nyayasetu.data.repository

import com.nyayasetu.data.local.dao.BookmarkDao
import com.nyayasetu.data.local.entity.BookmarkEntity
import com.nyayasetu.domain.model.Bookmark
import com.nyayasetu.domain.model.MappingRecord
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BookmarkRepository @Inject constructor(
    private val dao: BookmarkDao,
) {

    fun observeBookmarks(): Flow<List<Bookmark>> =
        dao.observeAll().map { list ->
            list.map { e ->
                Bookmark(
                    id = e.id,
                    ipcSection = e.ipcSection,
                    bnsSection = e.bnsSection,
                    ipcHeading = e.ipcHeading,
                    bnsHeading = e.bnsHeading,
                    status = e.status,
                    savedAt = e.savedAt,
                )
            }
        }

    suspend fun addFromMapping(record: MappingRecord) {
        val existing = dao.findBySections(record.ipcSection, record.bnsSection)
        if (existing != null) return
        dao.insert(
            BookmarkEntity(
                ipcSection = record.ipcSection,
                bnsSection = record.bnsSection,
                ipcHeading = record.ipcHeading,
                bnsHeading = record.bnsHeading,
                status = record.status,
                savedAt = System.currentTimeMillis(),
            ),
        )
    }

    suspend fun remove(bookmark: Bookmark) {
        dao.delete(
            BookmarkEntity(
                id = bookmark.id,
                ipcSection = bookmark.ipcSection,
                bnsSection = bookmark.bnsSection,
                ipcHeading = bookmark.ipcHeading,
                bnsHeading = bookmark.bnsHeading,
                status = bookmark.status,
                savedAt = bookmark.savedAt,
            ),
        )
    }
}
