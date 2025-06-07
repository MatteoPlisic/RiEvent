package com.example.rievent.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.rievent.models.User
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class SearchUserViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()

    // Single source of truth for the screen's entire state.
    private val _uiState = MutableStateFlow(SearchUserUiState())
    val uiState = _uiState.asStateFlow()

    private var searchJob: Job? = null

    /**
     * Called by the UI whenever the search text changes.
     * It updates the state and triggers the debounced search logic.
     */
    fun onSearchQueryChanged(query: String) {
        // Immediately update the text field in the UI
        _uiState.update { it.copy(searchQuery = query) }

        // Cancel any previous search job to start a new one
        searchJob?.cancel()

        // If the query is blank, clear the results and loading state, then stop.
        if (query.isBlank()) {
            _uiState.update { it.copy(searchResults = emptyList(), isLoading = false) }
            return
        }

        // Start a new coroutine for the search with a debounce
        searchJob = viewModelScope.launch {
            delay(300) // Wait for 300ms of inactivity

            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            try {
                // Firestore query to find users whose name starts with the query text.
                // '\uf8ff' is a special char that acts like a wildcard for prefix matching.
                val result = db.collection("users")
                    .orderBy("displayName")
                    .startAt(query)
                    .endAt(query + '\uf8ff')
                    .limit(20) // Always limit search results
                    .get()
                    .await()

                val userList = result.documents.mapNotNull { doc ->
                    doc.toObject(User::class.java)?.copy(uid = doc.id)
                }

                // Update the state with the results
                _uiState.update { it.copy(searchResults = userList, isLoading = false) }

            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Failed to search for users.", isLoading = false) }
            }
        }
    }
}