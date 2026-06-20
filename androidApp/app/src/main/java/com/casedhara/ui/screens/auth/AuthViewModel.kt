package com.casedhara.ui.screens.auth

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

data class AuthUiState(
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val showPassword: Boolean = false,
    // Only the email is exposed for the focus-triggered snackbar suggestion.
    // Fields are NOT pre-filled automatically anymore.
    val savedEmail: String = "",
    val savedPassword: String = "",
    // Controls the "Sign in as <email>?" suggestion shown on email field focus
    val showLoginSuggestion: Boolean = false,
)

sealed class AuthEvent {
    data object NavigateToHome : AuthEvent()
}

private const val PREFS_NAME = "auth_credentials"
private const val KEY_EMAIL = "saved_email"
private const val KEY_PASSWORD = "saved_password"

@HiltViewModel
class AuthViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
) : ViewModel() {

    private val _state = MutableStateFlow(AuthUiState())
    val state: StateFlow<AuthUiState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<AuthEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<AuthEvent> = _events.asSharedFlow()

    private val encryptedPrefs by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    init {
        loadSavedCredentials()
    }

    private fun loadSavedCredentials() {
        try {
            val savedEmail = encryptedPrefs.getString(KEY_EMAIL, "") ?: ""
            val savedPassword = encryptedPrefs.getString(KEY_PASSWORD, "") ?: ""
            // Store credentials for suggestion only — do NOT pre-fill the fields
            _state.value = _state.value.copy(
                savedEmail = savedEmail,
                savedPassword = savedPassword,
            )
        } catch (_: Exception) { }
    }

    private fun saveCredentials(email: String, password: String) {
        try {
            encryptedPrefs.edit()
                .putString(KEY_EMAIL, email)
                .putString(KEY_PASSWORD, password)
                .apply()
            _state.value = _state.value.copy(savedEmail = email, savedPassword = password)
        } catch (_: Exception) { }
    }

    fun updateEmail(email: String) {
        _state.value = _state.value.copy(email = email, error = null)
    }

    fun updatePassword(password: String) {
        _state.value = _state.value.copy(password = password, error = null)
    }

    fun togglePasswordVisibility() {
        _state.value = _state.value.copy(showPassword = !_state.value.showPassword)
    }

    /** Called when the email field gains focus — shows the suggestion banner if a saved email exists. */
    fun onEmailFocused() {
        if (_state.value.savedEmail.isNotBlank()) {
            _state.value = _state.value.copy(showLoginSuggestion = true)
        }
    }

    /** Dismisses the suggestion banner without filling credentials. */
    fun dismissLoginSuggestion() {
        _state.value = _state.value.copy(showLoginSuggestion = false)
    }

    fun acceptLoginSuggestion() {
        _state.value = _state.value.copy(
            email = _state.value.savedEmail,
            password = _state.value.savedPassword,
            showLoginSuggestion = false,
            error = null,
        )
    }

    fun login() {
        val email = state.value.email.trim()
        val password = state.value.password
        if (email.isBlank() || password.isBlank()) {
            _state.value = _state.value.copy(error = "Email and password are required.")
            return
        }
        _state.value = _state.value.copy(isLoading = true, error = null, showLoginSuggestion = false)
        FirebaseAuth.getInstance()
            .signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                _state.value = _state.value.copy(isLoading = false)
                if (task.isSuccessful) {
                    saveCredentials(email, password)
                    _events.tryEmit(AuthEvent.NavigateToHome)
                } else {
                    _state.value = _state.value.copy(
                        error = task.exception?.message ?: "Login failed."
                    )
                }
            }
    }

    fun signup() {
        val email = state.value.email.trim()
        val password = state.value.password
        if (email.isBlank() || password.isBlank()) {
            _state.value = _state.value.copy(error = "Email and password are required.")
            return
        }
        _state.value = _state.value.copy(isLoading = true, error = null)
        FirebaseAuth.getInstance()
            .createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                _state.value = _state.value.copy(isLoading = false)
                if (task.isSuccessful) {
                    saveCredentials(email, password)
                    _events.tryEmit(AuthEvent.NavigateToHome)
                } else {
                    _state.value = _state.value.copy(
                        error = task.exception?.message ?: "Signup failed."
                    )
                }
            }
    }
}