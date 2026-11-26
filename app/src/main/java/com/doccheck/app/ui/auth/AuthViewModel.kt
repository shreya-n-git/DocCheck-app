package com.doccheck.app.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.doccheck.app.data.repository.AuthRepository
import com.doccheck.app.util.ValidationUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AuthUiState(
    val isSignUpMode: Boolean = false,
    val isLoading: Boolean = false,
    val nameError: String? = null,
    val emailError: String? = null,
    val passwordError: String? = null,
    val confirmPasswordError: String? = null
)

sealed class AuthEvent {
    data object NavigateHome : AuthEvent()
    data class ShowMessage(val message: String) : AuthEvent()
}

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<AuthEvent>()
    val events: SharedFlow<AuthEvent> = _events.asSharedFlow()

    fun setSignUpMode(signUp: Boolean) {
        _uiState.update {
            it.copy(
                isSignUpMode = signUp,
                nameError = null,
                emailError = null,
                passwordError = null,
                confirmPasswordError = null
            )
        }
    }

    fun submit(
        fullName: String,
        email: String,
        password: String,
        confirmPassword: String,
        nameRequiredMsg: String,
        invalidEmailMsg: String,
        passwordShortMsg: String,
        passwordMismatchMsg: String
    ) {
        val isSignUp = _uiState.value.isSignUpMode
        val nameErr = if (isSignUp && fullName.isBlank()) nameRequiredMsg else null
        val emailErr = if (!ValidationUtils.isValidEmail(email)) invalidEmailMsg else null
        val passErr = if (!ValidationUtils.isValidPassword(password)) passwordShortMsg else null
        val confirmErr = if (isSignUp && !ValidationUtils.doPasswordsMatch(password, confirmPassword)) {
            passwordMismatchMsg
        } else null

        if (nameErr != null || emailErr != null || passErr != null || confirmErr != null) {
            _uiState.update {
                it.copy(
                    nameError = nameErr,
                    emailError = emailErr,
                    passwordError = passErr,
                    confirmPasswordError = confirmErr
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    nameError = null,
                    emailError = null,
                    passwordError = null,
                    confirmPasswordError = null
                )
            }
            val result = if (isSignUp) {
                authRepository.signUpWithEmail(fullName, email, password)
            } else {
                authRepository.loginWithEmail(email, password)
            }
            _uiState.update { it.copy(isLoading = false) }
            if (result.success) {
                _events.emit(AuthEvent.NavigateHome)
            } else {
                _events.emit(AuthEvent.ShowMessage(result.errorMessage ?: "Authentication failed"))
            }
        }
    }

    fun signInWithGoogle() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val result = authRepository.signInWithGoogle()
            _uiState.update { it.copy(isLoading = false) }
            if (result.success) {
                _events.emit(AuthEvent.NavigateHome)
            } else {
                _events.emit(AuthEvent.ShowMessage(result.errorMessage ?: "Google Sign-In failed"))
            }
        }
    }

    fun sendPasswordReset(email: String, invalidEmailMsg: String, resetSentMsg: String) {
        if (!ValidationUtils.isValidEmail(email)) {
            _uiState.update { it.copy(emailError = invalidEmailMsg) }
            return
        }
        viewModelScope.launch {
            val sent = authRepository.sendPasswordReset(email)
            if (sent) {
                _events.emit(AuthEvent.ShowMessage(resetSentMsg))
            } else {
                _events.emit(AuthEvent.ShowMessage("Could not send password reset email."))
            }
        }
    }
}

