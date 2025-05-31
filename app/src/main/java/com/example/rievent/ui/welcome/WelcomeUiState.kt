package com.example.rievent.ui.welcome

data class WelcomeUiState (
    val success: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null
)