package com.example.rievent.ui.userprofile

import Event
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.rievent.models.Following
import com.example.rievent.models.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.toObject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class UserProfileViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val currentAuthUserId = auth.currentUser?.uid

    // Single source of truth for the screen's state
    private val _uiState = MutableStateFlow(UserProfileUiState())
    val uiState = _uiState.asStateFlow()

    private var userProfileListener: ListenerRegistration? = null
    private var createdEventsListener: ListenerRegistration? = null
    private var followingStatusListener: ListenerRegistration? = null

    fun loadDataFor(profileUserId: String) {
        if (profileUserId.isBlank()) {
            _uiState.update { it.copy(errorMessage = "User ID is invalid.", isLoadingProfile = false) }
            return
        }

        // Reset state for new profile load
        _uiState.value = UserProfileUiState()

        // Detach all previous listeners to prevent leaks
        clearAllListeners()

        loadUserProfile(profileUserId)
        loadCreatedEvents(profileUserId)
        if (currentAuthUserId != null && profileUserId != currentAuthUserId) {
            listenToFollowingStatus(profileUserId, currentAuthUserId)
        }
    }

    private fun loadUserProfile(profileUserId: String) {
        _uiState.update { it.copy(isLoadingProfile = true) }
        userProfileListener = db.collection("users").document(profileUserId)
            .addSnapshotListener { snapshot, e ->
                if (e != null || snapshot == null || !snapshot.exists()) {
                    _uiState.update { it.copy(errorMessage = "User profile not found.", isLoadingProfile = false, user = null) }
                    return@addSnapshotListener
                }
                val user = snapshot.toObject<User>()?.copy(uid = snapshot.id)
                _uiState.update { it.copy(user = user, isLoadingProfile = false, errorMessage = null) }
            }
    }

    private fun loadCreatedEvents(userId: String) {
        _uiState.update { it.copy(isLoadingEvents = true) }
        createdEventsListener = db.collection("Event")
            .whereEqualTo("ownerId", userId)
            .orderBy("startTime", Query.Direction.DESCENDING).limit(20)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    _uiState.update { it.copy(errorMessage = "Failed to load events.", isLoadingEvents = false) }
                    return@addSnapshotListener
                }
                val events = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject<Event>()?.copy(id = doc.id)
                } ?: emptyList()
                _uiState.update { it.copy(createdEvents = events, isLoadingEvents = false) }
            }
    }

    private fun listenToFollowingStatus(profileUserId: String, currentLoggedInUserId: String) {
        followingStatusListener = db.collection("followers")
            .whereEqualTo("userId", profileUserId)
            .whereEqualTo("followerID", currentLoggedInUserId)
            .limit(1)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.w("UserProfileVM", "Follow status listen failed.", e)
                    return@addSnapshotListener
                }
                _uiState.update { it.copy(isFollowing = snapshot != null && !snapshot.isEmpty) }
            }
    }

    fun toggleFollowUser(profileUserIdToFollow: String) {
        if (currentAuthUserId == null) {
            _uiState.update { it.copy(errorMessage = "You must be logged in to follow.") }
            return
        }
        _uiState.update { it.copy(isFollowActionLoading = true) }
        viewModelScope.launch {
            try {
                val followersCollection = db.collection("followers")
                val existingFollowQuery = followersCollection
                    .whereEqualTo("userId", profileUserIdToFollow)
                    .whereEqualTo("followerID", currentAuthUserId)
                    .limit(1).get().await()

                if (existingFollowQuery.isEmpty) {
                    val following = Following(userId = profileUserIdToFollow, followerID = currentAuthUserId)
                    followersCollection.add(following).await()
                } else {
                    existingFollowQuery.documents.forEach { it.reference.delete().await() }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Failed to update follow status.") }
            } finally {
                _uiState.update { it.copy(isFollowActionLoading = false) }
            }
        }
    }

    private fun clearAllListeners() {
        userProfileListener?.remove()
        createdEventsListener?.remove()
        followingStatusListener?.remove()
    }

    override fun onCleared() {
        super.onCleared()
        clearAllListeners()
        Log.d("UserProfileVM", "ViewModel cleared, listeners removed.")
    }
}