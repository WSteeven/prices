package com.example.pricesapp.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pricesapp.data.AuthRepository
import com.example.pricesapp.data.AuthResult
import com.example.pricesapp.data.Profile
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.auth.user.UserInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class AuthState { LOADING, AUTHENTICATED, NOT_AUTHENTICATED }

class AuthViewModel(
    private val repository: AuthRepository
) : ViewModel() {

    private val _authState = MutableStateFlow(AuthState.LOADING)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val _user = MutableStateFlow<UserInfo?>(null)
    val user: StateFlow<UserInfo?> = _user.asStateFlow()

    private val _profile = MutableStateFlow<Profile?>(null)
    val profile: StateFlow<Profile?> = _profile.asStateFlow()

    val isAdmin: StateFlow<Boolean> = _profile
        .map { it?.isAdmin == true }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _isSigningIn = MutableStateFlow(false)
    val isSigningIn: StateFlow<Boolean> = _isSigningIn.asStateFlow()

    init {
        // La sesión guardada se carga de forma asíncrona: escuchamos su estado
        // en vez de consultar currentUserOrNull() al arrancar.
        viewModelScope.launch {
            repository.auth.sessionStatus.collect { status ->
                when (status) {
                    is SessionStatus.Initializing -> _authState.value = AuthState.LOADING
                    is SessionStatus.Authenticated -> onSignedIn(status.session.user)
                    // Sin internet no se puede refrescar el token, pero la sesión sigue guardada
                    is SessionStatus.RefreshFailure -> onSignedIn(repository.auth.currentUserOrNull())
                    is SessionStatus.NotAuthenticated -> onSignedOut()
                }
            }
        }
    }

    private fun onSignedIn(user: UserInfo?) {
        if (user == null) {
            onSignedOut()
            return
        }
        val userChanged = _user.value?.id != user.id
        _user.value = user
        _authState.value = AuthState.AUTHENTICATED
        if (userChanged || _profile.value == null) loadProfile(user)
    }

    private fun onSignedOut() {
        _user.value = null
        _profile.value = null
        _authState.value = AuthState.NOT_AUTHENTICATED
    }

    private fun loadProfile(user: UserInfo) {
        viewModelScope.launch {
            _profile.value = repository.fetchProfile(user.id, user.email)
        }
    }

    fun signIn(email: String, password: String) {
        if (_isSigningIn.value) return
        viewModelScope.launch {
            _isSigningIn.value = true
            when (val result = repository.signIn(email.trim(), password)) {
                // sessionStatus emite Authenticated y actualiza el estado
                is AuthResult.Success -> _errorMessage.value = null
                is AuthResult.Error -> _errorMessage.value = result.message
            }
            _isSigningIn.value = false
        }
    }

    private val _passwordMessage = MutableStateFlow<String?>(null)
    val passwordMessage: StateFlow<String?> = _passwordMessage.asStateFlow()

    private val _isChangingPassword = MutableStateFlow(false)
    val isChangingPassword: StateFlow<Boolean> = _isChangingPassword.asStateFlow()

    fun changePassword(newPassword: String, onSuccess: () -> Unit) {
        if (_isChangingPassword.value) return
        viewModelScope.launch {
            _isChangingPassword.value = true
            _passwordMessage.value = when (val result = repository.changePassword(newPassword)) {
                is AuthResult.Success -> {
                    onSuccess()
                    "Contraseña actualizada"
                }
                is AuthResult.Error -> result.message
            }
            _isChangingPassword.value = false
        }
    }

    fun clearPasswordMessage() {
        _passwordMessage.value = null
    }

    fun signOut() {
        viewModelScope.launch {
            repository.signOut()
        }
    }
}
