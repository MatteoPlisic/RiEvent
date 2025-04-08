package com.example.rievent.ui.welcome


import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update


class WelcomeViewModel {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val _uiState = MutableStateFlow(WelcomeUiState())
    val uiState: StateFlow<WelcomeUiState> = _uiState

    fun clearNavigationFlag() {
        _uiState.update { it.copy(success = false) }
    }

}