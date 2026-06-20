package com.casedhara.ui.screens.quiz

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.casedhara.BuildConfig
import com.casedhara.data.local.dao.QuizProgressDao
import com.casedhara.data.local.dao.WrongAnswerDao
import com.casedhara.data.local.entity.QuizProgressEntity
import com.casedhara.data.local.entity.WrongAnswerEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

private val Context.quizDataStore by preferencesDataStore(name = "quiz_prefs")
private val KEY_STREAK    = intPreferencesKey("streak")
private val KEY_LAST_DATE = longPreferencesKey("last_quiz_date_ms")

data class QuizQuestion(
    val question: String,
    val options: List<String>,
    val correctIndex: Int,
    val explanation: String,
)

@HiltViewModel
class QuizViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val wrongAnswerDao: WrongAnswerDao,
    private val quizProgressDao: QuizProgressDao,
) : ViewModel() {

    sealed interface QuizUiState {
        data class Setup(
            val questionCount: Int = 10,
            val difficulty: String = "Medium",
            val topic: String = "Indian Constitution",
            val streak: Int = 0,
        ) : QuizUiState

        object Loading : QuizUiState

        data class Active(
            val questions: List<QuizQuestion>,
            val questionIndex: Int,
            val selectedAnswer: Int?,
            val score: Int,
            val answeredMap: Map<Int, Int> = emptyMap(),
            val startTimeMs: Long = System.currentTimeMillis(),
        ) : QuizUiState {
            val currentQuestion get() = questions[questionIndex]
            val totalQuestions  get() = questions.size
        }

        data class Review(
            val questions: List<QuizQuestion>,
            val answeredMap: Map<Int, Int>,
            val score: Int,
        ) : QuizUiState

        data class Result(
            val score: Int,
            val totalQuestions: Int,
            val streak: Int,
            val questions: List<QuizQuestion>,
            val answeredMap: Map<Int, Int>,
        ) : QuizUiState

        data class Error(val message: String) : QuizUiState
    }

    private val _uiState = MutableStateFlow<QuizUiState>(QuizUiState.Setup())
    val uiState: StateFlow<QuizUiState> = _uiState.asStateFlow()

    private var savedSetup = QuizUiState.Setup()
    private var currentStreak = 0
    private var lastStreakDate = ""

    init {
        viewModelScope.launch {
            context.quizDataStore.data.first().let { prefs ->
                val streak     = prefs[KEY_STREAK] ?: 0
                val lastDateMs = prefs[KEY_LAST_DATE] ?: 0L
                currentStreak = calculateStreak(streak, lastDateMs)
                lastStreakDate = if (lastDateMs > 0L)
                    SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date(lastDateMs))
                else ""
                savedSetup = QuizUiState.Setup(streak = currentStreak)
                _uiState.value = savedSetup
            }
        }
    }

    fun onQuestionCountChange(count: Int) {
        val s = _uiState.value as? QuizUiState.Setup ?: return
        savedSetup = s.copy(questionCount = count); _uiState.value = savedSetup
    }
    fun onDifficultyChange(difficulty: String) {
        val s = _uiState.value as? QuizUiState.Setup ?: return
        savedSetup = s.copy(difficulty = difficulty); _uiState.value = savedSetup
    }
    fun onTopicChange(topic: String) {
        val s = _uiState.value as? QuizUiState.Setup ?: return
        savedSetup = s.copy(topic = topic); _uiState.value = savedSetup
    }

    fun startQuiz() {
        val setup = _uiState.value as? QuizUiState.Setup ?: return
        _uiState.value = QuizUiState.Loading
        viewModelScope.launch {
            try {
                val questions = generateQuizViaGemini(
                    count = setup.questionCount,
                    difficulty = setup.difficulty,
                    topic = setup.topic,
                )
                _uiState.value = QuizUiState.Active(
                    questions = questions,
                    questionIndex = 0,
                    selectedAnswer = null,
                    score = 0,
                    startTimeMs = System.currentTimeMillis(),
                )
            } catch (e: GeminiException) {
                _uiState.value = QuizUiState.Error(e.message ?: "Gemini API error")
            } catch (e: Exception) {
                val msg = when {
                    e.message?.contains("Unable to resolve host") == true ->
                        "No internet connection. Please check your network and try again."
                    e.message?.contains("timeout", ignoreCase = true) == true ->
                        "Request timed out. Please try again."
                    else ->
                        "Failed to generate quiz. Please try again.\n(${e.javaClass.simpleName})"
                }
                _uiState.value = QuizUiState.Error(msg)
            }
        }
    }

    fun onAnswerSelected(answerIndex: Int) {
        val active = _uiState.value as? QuizUiState.Active ?: return
        if (active.selectedAnswer != null) return
        val correct = answerIndex == active.currentQuestion.correctIndex
        val newAnsweredMap = active.answeredMap + (active.questionIndex to answerIndex)

        // Save wrong answers to Room immediately
        if (!correct) {
            val setup = savedSetup
            val q = active.currentQuestion
            viewModelScope.launch {
                wrongAnswerDao.insert(
                    WrongAnswerEntity(
                        topic = setup.topic,
                        difficulty = setup.difficulty,
                        question = q.question,
                        optionA = q.options.getOrElse(0) { "" },
                        optionB = q.options.getOrElse(1) { "" },
                        optionC = q.options.getOrElse(2) { "" },
                        optionD = q.options.getOrElse(3) { "" },
                        correctIndex = q.correctIndex,
                        userAnswerIndex = answerIndex,
                        explanation = q.explanation,
                    )
                )
            }
        }

        _uiState.value = active.copy(
            selectedAnswer = answerIndex,
            score = if (correct) active.score + 1 else active.score,
            answeredMap = newAnsweredMap,
        )
    }

    fun onTimeUp() {
        val active = _uiState.value as? QuizUiState.Active ?: return
        if (active.selectedAnswer != null) return
        val newAnsweredMap = active.answeredMap + (active.questionIndex to -1)
        _uiState.value = active.copy(
            selectedAnswer = -1,
            answeredMap = newAnsweredMap,
        )
    }

    fun nextQuestion() {
        val active = _uiState.value as? QuizUiState.Active ?: return
        val nextIndex = active.questionIndex + 1
        if (nextIndex >= active.questions.size) {
            viewModelScope.launch {
                val fmt   = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
                val today = fmt.format(Date())
                val newStreak = if (lastStreakDate == today) currentStreak else currentStreak + 1
                context.quizDataStore.edit { prefs ->
                    prefs[KEY_STREAK]    = newStreak
                    prefs[KEY_LAST_DATE] = System.currentTimeMillis()
                }
                currentStreak = newStreak
                lastStreakDate = today

                // ── Save quiz progress to Room ────────────────────────────
                val timeTaken = ((System.currentTimeMillis() - active.startTimeMs) / 1000).toInt()
                val questionsJson = buildQuestionsJson(active.questions, active.answeredMap)
                quizProgressDao.insert(
                    QuizProgressEntity(
                        topic = savedSetup.topic,
                        difficulty = savedSetup.difficulty,
                        totalQuestions = active.questions.size,
                        correctAnswers = active.score,
                        timeTakenSeconds = timeTaken,
                        questionsJson = questionsJson,
                    )
                )

                _uiState.value = QuizUiState.Result(
                    score = active.score,
                    totalQuestions = active.questions.size,
                    streak = newStreak,
                    questions = active.questions,
                    answeredMap = active.answeredMap,
                )
            }
        } else {
            _uiState.value = active.copy(questionIndex = nextIndex, selectedAnswer = null)
        }
    }

    private fun buildQuestionsJson(questions: List<QuizQuestion>, answeredMap: Map<Int, Int>): String {
        val arr = JSONArray()
        questions.forEachIndexed { i, q ->
            arr.put(JSONObject().apply {
                put("question", q.question)
                put("options", JSONArray(q.options))
                put("correctIndex", q.correctIndex)
                put("userAnswer", answeredMap[i] ?: -1)
                put("explanation", q.explanation)
            })
        }
        return arr.toString()
    }

    fun showReview() {
        val result = _uiState.value as? QuizUiState.Result ?: return
        _uiState.value = QuizUiState.Review(
            questions = result.questions,
            answeredMap = result.answeredMap,
            score = result.score,
        )
    }

    fun backToResult() {
        val review = _uiState.value as? QuizUiState.Review ?: return
        _uiState.value = QuizUiState.Result(
            score = review.score,
            totalQuestions = review.questions.size,
            streak = currentStreak,
            questions = review.questions,
            answeredMap = review.answeredMap,
        )
    }

    fun resetToSetup() {
        _uiState.value = savedSetup.copy(streak = currentStreak)
    }

    // ── Gemini API ────────────────────────────────────────────────────────

    private class GeminiException(message: String) : Exception(message)

    private suspend fun generateQuizViaGemini(
        count: Int,
        difficulty: String,
        topic: String,
    ): List<QuizQuestion> = withContext(Dispatchers.IO) {

        val apiKey = BuildConfig.GEMINI_API_KEY

        if (apiKey.isBlank() || apiKey == "YOUR_GEMINI_API_KEY") {
            throw GeminiException("Gemini API key missing.")
        }

        val endpoint =
            "https://generativelanguage.googleapis.com/v1beta/models/" +
                    "gemini-2.5-flash-lite:generateContent?key=$apiKey"

        val prompt = """
Generate exactly $count Indian law MCQ quiz questions for law students.

Topic: $topic
Difficulty: $difficulty

IMPORTANT:
Return ONLY valid JSON. No markdown. No ```json. No explanations outside JSON.

Required format:
[
  {
    "question": "Question text",
    "options": ["Option A","Option B","Option C","Option D"],
    "correctIndex": 2,
    "explanation": "Short explanation"
  }
]

Rules:
- Exactly 4 options per question
- correctIndex must be 0, 1, 2, or 3 — distribute evenly across questions; do NOT default to 0
- Each question must have a DIFFERENT correctIndex from the previous one where possible
- Return ONLY a JSON array
- Questions must be relevant to $topic and suitable for AIBE/Judiciary exam preparation
""".trimIndent()

        val requestBody = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply {
                            put("text", prompt)
                        })
                    })
                })
            })
            put("generationConfig", JSONObject().apply {
                put("temperature", 0.2)
                put("maxOutputTokens", 2048)
                put("responseMimeType", "application/json")
            })
        }.toString()

        val url = URL(endpoint)
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Accept", "application/json")
            connectTimeout = 30000
            readTimeout = 60000
            doOutput = true
        }

        try {
            OutputStreamWriter(conn.outputStream, "UTF-8").use {
                it.write(requestBody)
            }

            val responseCode = conn.responseCode
            val responseText = if (responseCode in 200..299) {
                BufferedReader(InputStreamReader(conn.inputStream, "UTF-8")).readText()
            } else {
                val errorText = conn.errorStream?.let {
                    BufferedReader(InputStreamReader(it, "UTF-8")).readText()
                }
                throw GeminiException("Gemini API Error $responseCode\n$errorText")
            }

            val root = JSONObject(responseText)
            if (root.has("error")) {
                val errorObj = root.getJSONObject("error")
                throw GeminiException(errorObj.optString("message", "Unknown Gemini error"))
            }

            val candidates = root.optJSONArray("candidates")
                ?: throw GeminiException("No candidates returned")
            if (candidates.length() == 0) throw GeminiException("Empty response from Gemini")

            var cleaned = candidates
                .getJSONObject(0)
                .getJSONObject("content")
                .getJSONArray("parts")
                .getJSONObject(0)
                .optString("text", "")
                .trim()
                .replace("```json", "")
                .replace("```", "")
                .trim()

            val startIndex = cleaned.indexOf("[")
            val endIndex = cleaned.lastIndexOf("]")
            if (startIndex == -1 || endIndex == -1) throw GeminiException("Invalid JSON response")
            cleaned = cleaned.substring(startIndex, endIndex + 1)

            val jsonArray = JSONArray(cleaned)
            val questions = mutableListOf<QuizQuestion>()
            // Collect all questions with their correct answer text, then apply a
            // global shuffled-slot distribution to ensure correct answer positions
            // are spread evenly and non-sequentially across all questions.
            data class RawQ(val text: String, val opts: MutableList<String>, val correctAnswer: String, val explanation: String)
            val rawQuestions = mutableListOf<RawQ>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val optionsArray = obj.getJSONArray("options")
                val opts = mutableListOf<String>()
                for (j in 0 until optionsArray.length()) opts.add(optionsArray.getString(j))
                val correctIndex = obj.optInt("correctIndex", 0).coerceIn(0, 3)
                rawQuestions.add(RawQ(
                    text = obj.optString("question", "Question unavailable"),
                    opts = opts,
                    correctAnswer = opts.getOrElse(correctIndex) { opts[0] },
                    explanation = obj.optString("explanation", "No explanation available"),
                ))
            }

            // Build a shuffled target-slot list cycling through 0-3 so no two
            // consecutive questions share the same correct-answer position.
            val totalQ = rawQuestions.size
            val slotPool = mutableListOf<Int>().apply {
                val base = listOf(0, 1, 2, 3)
                repeat((totalQ / 4) + 2) { addAll(base) }
                shuffle(kotlin.random.Random(System.nanoTime()))
            }
            for (idx in 1 until minOf(slotPool.size, totalQ)) {
                if (slotPool[idx] == slotPool[idx - 1]) {
                    slotPool[idx] = ((slotPool[idx] + 1 + (idx % 3)) % 4)
                }
            }

            for (i in rawQuestions.indices) {
                val rq = rawQuestions[i]
                val targetSlot = slotPool[i].coerceIn(0, 3)
                // Shuffle options with a unique per-question seed
                rq.opts.shuffle(kotlin.random.Random(System.nanoTime() + i.toLong() * 31L))
                // Place the correct answer at the target slot
                val currentIdx = rq.opts.indexOf(rq.correctAnswer).takeIf { it >= 0 } ?: 0
                if (currentIdx != targetSlot) {
                    val displaced = rq.opts[targetSlot]
                    rq.opts[targetSlot] = rq.opts[currentIdx]
                    rq.opts[currentIdx] = displaced
                }
                questions.add(
                    QuizQuestion(
                        question = rq.text,
                        options = rq.opts,
                        correctIndex = rq.opts.indexOf(rq.correctAnswer).coerceIn(0, 3),
                        explanation = rq.explanation,
                    )
                )
            }

            if (questions.isEmpty()) throw GeminiException("No quiz questions generated")
            questions

        } catch (e: Exception) {
            e.printStackTrace()
            throw GeminiException(e.message ?: "Failed to generate quiz")
        } finally {
            conn.disconnect()
        }
    }

    private fun calculateStreak(savedStreak: Int, lastDateMs: Long): Int {
        if (lastDateMs == 0L) return 0
        val fmt       = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
        val today     = fmt.format(Date())
        val lastDate  = fmt.format(Date(lastDateMs))
        val yesterday = fmt.format(Date(System.currentTimeMillis() - 86_400_000L))
        return when (lastDate) {
            today     -> savedStreak
            yesterday -> savedStreak
            else      -> 0
        }
    }
}
