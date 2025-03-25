// ui/login/LoginUiState.kt
package com.example.rievent.ui.login

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val loginError: String? = null,
    val success: Boolean = false
)
