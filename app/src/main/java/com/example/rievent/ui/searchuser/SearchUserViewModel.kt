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


    private val _uiState = MutableStateFlow(SearchUserUiState())
    val uiState = _uiState.asStateFlow()

    private var searchJob: Job? = null


    fun onSearchQueryChanged(query: String) {

        _uiState.update { it.copy(searchQuery = query) }


        searchJob?.cancel()


        if (query.isBlank()) {
            _uiState.update { it.copy(searchResults = emptyList(), isLoading = false) }
            return
        }


        searchJob = viewModelScope.launch {
            delay(300)

            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            try {

                val result = db.collection("users")
                    .orderBy("displayName")
                    .startAt(query)
                    .endAt(query + '\uf8ff')
                    .limit(20)
                    .get()
                    .await()

                val userList = result.documents.mapNotNull { doc ->
                    doc.toObject(User::class.java)?.copy(uid = doc.id)
                }


                _uiState.update { it.copy(searchResults = userList, isLoading = false) }

            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Failed to search for users.", isLoading = false) }
            }
        }
    }
}