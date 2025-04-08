package com.example.rievent.ui.login

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class LoginViewModel : ViewModel() {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState

    private val _navigateToHome = MutableSharedFlow<Unit>(replay = 1)
    val navigateToHome: SharedFlow<Unit> = _navigateToHome

    fun onEmailChange(email: String) {
        _uiState.update { it.copy(email = email) }
    }

    fun onPasswordChange(password: String) {
        _uiState.update { it.copy(password = password) }
    }

    fun onLoginClick() {
        val email = _uiState.value.email
        val password = _uiState.value.password

        auth.signInWithEmailAndPassword(email, password).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                // ✅ Login success
                Log.d("FirebaseLogin", "Logged in as: ${auth.currentUser?.email}")
                _uiState.update { it.copy(success = true) }

                viewModelScope.launch {
                    _navigateToHome.emit(Unit)
                }
            } else {
                // ❌ Login failed
                Log.e("FirebaseLogin", "Login failed", task.exception)
                Log.e("FirebaseLogin", email + " " + password,task.exception)
                _uiState.update { it.copy(loginError = "Invalid credentials.") }
            }

        }
    }

    fun onForgotPasswordClick() {
        // Handle forgot password
        println("Forgot password clicked")
    }

    fun clearNavigationFlag() {
        _uiState.update { it.copy(success = false) }
    }
}
