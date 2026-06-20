package com.casedhara.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.Room
import com.casedhara.data.local.AppDatabase
import com.casedhara.data.local.dao.BookmarkDao
import com.casedhara.data.local.dao.QuizProgressDao
import com.casedhara.data.local.dao.SavedCaseDao
import com.casedhara.data.local.dao.SearchHistoryDao
import com.casedhara.data.local.dao.WrongAnswerDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

// Single declaration of the DataStore delegate for the entire app.
// Keeping it here (next to the @Provides function) ensures no other file
// can accidentally declare a second delegate for the same name.
private val Context.settingsDataStore by preferencesDataStore(name = "settings_prefs")

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "casedhara.db")
            .fallbackToDestructiveMigration()
            .build()

    /**
     * Single, process-wide DataStore instance for settings (dark mode, font scale, etc.).
     * Both [SettingsViewModel] and [CaseDharaTheme] inject / receive this same instance,
     * so the "multiple DataStores active for the same file" crash can never happen.
     */
    @Provides
    @Singleton
    fun provideSettingsDataStore(
        @ApplicationContext context: Context,
    ): DataStore<Preferences> = context.settingsDataStore

    @Provides
    fun provideSearchHistoryDao(db: AppDatabase): SearchHistoryDao = db.searchHistoryDao()

    @Provides
    fun provideBookmarkDao(db: AppDatabase): BookmarkDao = db.bookmarkDao()

    @Provides
    fun provideWrongAnswerDao(db: AppDatabase): WrongAnswerDao = db.wrongAnswerDao()

    @Provides
    fun provideQuizProgressDao(db: AppDatabase): QuizProgressDao = db.quizProgressDao()

    @Provides
    fun provideSavedCaseDao(db: AppDatabase): SavedCaseDao = db.savedCaseDao()
}