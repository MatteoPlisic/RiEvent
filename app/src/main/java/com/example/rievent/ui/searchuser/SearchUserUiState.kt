package com.example.rievent.ui.chat

import com.example.rievent.models.User


data class SearchUserUiState(
    val searchQuery: String = "",
    val searchResults: List<User> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)