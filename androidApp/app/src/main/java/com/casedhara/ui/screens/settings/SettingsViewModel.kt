package com.casedhara.ui.screens.settings

import android.content.Context
import android.net.Uri
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.casedhara.data.local.dao.BookmarkDao
import com.casedhara.data.local.entity.BookmarkEntity
import com.casedhara.data.local.dao.QuizProgressDao
import com.casedhara.data.local.dao.SavedCaseDao
import com.casedhara.data.local.dao.WrongAnswerDao
import com.casedhara.data.local.entity.QuizProgressEntity
import com.casedhara.data.local.entity.SavedCaseEntity
import com.casedhara.data.local.entity.WrongAnswerEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

private val KEY_DARK_MODE = booleanPreferencesKey("dark_mode")
private val KEY_FONT_SIZE = floatPreferencesKey("font_size_scale")
private val KEY_PROFILE_IMAGE_PATH = stringPreferencesKey("profile_image_path")

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsDataStore: DataStore<Preferences>,
    @ApplicationContext private val context: Context,
    val wrongAnswerDao: WrongAnswerDao,
    val quizProgressDao: QuizProgressDao,
    val savedCaseDao: SavedCaseDao,
    val bookmarkDao: BookmarkDao,
) : ViewModel() {

    val isDarkMode: StateFlow<Boolean> = settingsDataStore.data
        .map { it[KEY_DARK_MODE] ?: false }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val fontSizeScale: StateFlow<Float> = settingsDataStore.data
        .map { it[KEY_FONT_SIZE] ?: 1.0f }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 1.0f)

    val profileImagePath: StateFlow<String?> = settingsDataStore.data
        .map { it[KEY_PROFILE_IMAGE_PATH] }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val wrongAnswers: StateFlow<List<WrongAnswerEntity>> = wrongAnswerDao.getAllFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val quizProgress: StateFlow<List<QuizProgressEntity>> = quizProgressDao.getAllFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val savedCases: StateFlow<List<SavedCaseEntity>> = savedCaseDao.getAllFlow()
        .map { list -> list.filter { it.source == "case_summarizer" } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val bookmarks = bookmarkDao.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setDarkMode(enabled: Boolean) = viewModelScope.launch {
        settingsDataStore.edit { it[KEY_DARK_MODE] = enabled }
    }

    fun setFontSizeScale(scale: Float) = viewModelScope.launch {
        settingsDataStore.edit { it[KEY_FONT_SIZE] = scale }
    }

    fun saveProfileImagePath(path: String) = viewModelScope.launch {
        settingsDataStore.edit { it[KEY_PROFILE_IMAGE_PATH] = path }
    }

    fun clearCache() = viewModelScope.launch {
        context.cacheDir.deleteRecursively()
    }

    fun deleteWrongAnswer(entity: WrongAnswerEntity) = viewModelScope.launch {
        wrongAnswerDao.delete(entity)
    }

    fun deleteQuizProgress(entity: QuizProgressEntity) = viewModelScope.launch {
        quizProgressDao.delete(entity)
    }

    fun deleteSavedCase(entity: SavedCaseEntity) = viewModelScope.launch {
        savedCaseDao.delete(entity)
    }

    suspend fun getSavedCaseById(id: Long): SavedCaseEntity? = savedCaseDao.getById(id)

    suspend fun getWrongAnswerById(id: Long): WrongAnswerEntity? = wrongAnswerDao.getById(id)

    suspend fun getQuizProgressById(id: Long): QuizProgressEntity? = quizProgressDao.getById(id)

    companion object {
        fun saveProfileImage(context: Context, uri: Uri): String {
            val dir = File(context.filesDir, "profile")
            dir.mkdirs()
            val dest = File(dir, "profile_image.jpg")
            context.contentResolver.openInputStream(uri)?.use { input ->
                dest.outputStream().use { output -> input.copyTo(output) }
            }
            return dest.absolutePath
        }
    }
}
