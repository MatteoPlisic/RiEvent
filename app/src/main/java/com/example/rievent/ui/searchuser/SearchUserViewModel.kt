package com.example.rievent.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.rievent.models.User // Your existing User data class
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class SearchUserViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()

    private val _searchResults = MutableStateFlow<List<User>>(emptyList())
    val searchResults = _searchResults.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    private var searchJob: Job? = null

    /**
     * Searches for users based on their display name.
     * Uses a debounce mechanism to avoid excessive Firestore queries while the user is typing.
     *
     * @param query The text to search for in user display names.
     */
    fun searchUsers(query: String) {
        // Cancel any previous search job to start a new one
        searchJob?.cancel()

        // If the query is blank, clear the results and stop.
        if (query.isBlank()) {
            _searchResults.value = emptyList()
            return
        }

        // Start a new coroutine for the search
        searchJob = viewModelScope.launch {
            // Debounce: Wait for 300ms of inactivity before actually searching
            delay(300)

            _isLoading.value = true
            _error.value = null

            try {
                // Firestore query to find users whose name starts with the query text.
                // This is a case-sensitive prefix search.
                // For case-insensitivity, you'd typically store a lowercased version of the name.
                val result = db.collection("users")
                    .orderBy("displayName") // You must order by the field you are range-querying
                    .startAt(query)
                    .endAt(query + '\uf8ff') // '\uf8ff' is a special char that acts like a wildcard
                    .limit(20) // Always limit your search results to prevent huge bills
                    .get()
                    .await() // Using kotlinx-coroutines-play-services

                val userList = result.documents.mapNotNull { doc ->
                    doc.toObject(User::class.java)?.copy(uid = doc.id) // Assuming your User model has a uid field
                }
                _searchResults.value = userList

            } catch (e: Exception) {
                _error.value = "Failed to search for users: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
}