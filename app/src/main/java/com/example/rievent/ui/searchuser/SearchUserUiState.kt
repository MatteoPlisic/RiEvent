package com.example.rievent.ui.chat

import com.example.rievent.models.User

/**
 * Represents the single, comprehensive state for the SearchUserScreen.
 */
data class SearchUserUiState(
    val searchQuery: String = "",
    val searchResults: List<User> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)