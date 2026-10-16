package com.MatthewBaerg.multitasked.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.MatthewBaerg.multitasked.data.repository.AuthRepository
import com.MatthewBaerg.multitasked.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel responsible for login/registration logic.
 * Exposes an authState that the UI can observe.
 */
@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _authState = MutableStateFlow<Result<Unit>>(Result.Idle)
    val authState: StateFlow<Result<Unit>> = _authState

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _authState.value = Result.Loading
            try {
                authRepository.login(email, password)
                _authState.value = Result.Success(Unit)
            } catch (e: Exception) {
                _authState.value = Result.Error(e.message ?: "Login failed", e)
            }
        }
    }

    fun register(email: String, password: String) {
        viewModelScope.launch {
            _authState.value = Result.Loading
            try {
                authRepository.register(email, password)
                _authState.value = Result.Success(Unit)
            } catch (e: Exception) {
                _authState.value = Result.Error(e.message ?: "Registration failed", e)
            }
        }
    }

    fun logout() {
        authRepository.logout()
        _authState.value = Result.Idle
    }

    fun isLoggedIn(): Boolean = authRepository.currentUserId() != null
}
