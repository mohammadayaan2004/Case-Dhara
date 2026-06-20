package com.casedhara.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.casedhara.data.local.dao.BookmarkDao
import com.casedhara.data.local.dao.QuizProgressDao
import com.casedhara.data.local.dao.SavedCaseDao
import com.casedhara.data.local.dao.SearchHistoryDao
import com.casedhara.data.local.dao.WrongAnswerDao
import com.casedhara.data.local.entity.BookmarkEntity
import com.casedhara.data.local.entity.QuizProgressEntity
import com.casedhara.data.local.entity.SavedCaseEntity
import com.casedhara.data.local.entity.SearchHistoryEntity
import com.casedhara.data.local.entity.WrongAnswerEntity

@Database(
    entities = [
        SearchHistoryEntity::class,
        BookmarkEntity::class,
        WrongAnswerEntity::class,
        QuizProgressEntity::class,
        SavedCaseEntity::class,
    ],
    version = 3,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun searchHistoryDao(): SearchHistoryDao
    abstract fun bookmarkDao(): BookmarkDao
    abstract fun wrongAnswerDao(): WrongAnswerDao
    abstract fun quizProgressDao(): QuizProgressDao
    abstract fun savedCaseDao(): SavedCaseDao
}
