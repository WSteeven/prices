package com.example.pricesapp.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pricesapp.data.AuthRepository
import com.example.pricesapp.data.AuthResult
import com.example.pricesapp.data.Profile
import io.github.jan.supabase.auth.user.UserInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AuthViewModel(
    private val repository: AuthRepository
) : ViewModel() {

    private val _isAuthenticated = MutableStateFlow(false)
    val isAuthenticated: StateFlow<Boolean> = _isAuthenticated.asStateFlow()

    private val _user = MutableStateFlow<UserInfo?>(null)
    val user: StateFlow<UserInfo?> = _user.asStateFlow()

    private val _profile = MutableStateFlow<Profile?>(null)
    val profile: StateFlow<Profile?> = _profile.asStateFlow()

    val isAdmin: StateFlow<Boolean> = _profile
        .map { it?.isAdmin == true }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    init {
        restoreSession()
    }


    private fun restoreSession() {
        val user = repository.auth.currentUserOrNull()
        _user.value = user
        _isAuthenticated.value = user != null
        loadProfile()
    }

    private fun loadProfile() {
        val user = _user.value ?: return
        viewModelScope.launch {
            _profile.value = repository.fetchProfile(user.id, user.email)
        }
    }

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    fun signIn(email: String, password: String) {
        viewModelScope.launch {
            when (val result = repository.signIn(email, password)) {
                is AuthResult.Success -> {
                    restoreSession()
                    _errorMessage.value = null
                }
                is AuthResult.Error -> {
                    _errorMessage.value = result.message
                }
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            repository.signOut()
            _user.value = null
            _profile.value = null
            _isAuthenticated.value = false
        }
    }
}
