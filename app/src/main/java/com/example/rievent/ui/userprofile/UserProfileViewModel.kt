package com.example.rievent.ui.userprofile

import Event // Make sure this is your correct Event model import
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
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class UserProfileViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val _userProfile = MutableStateFlow<User?>(null)
    val userProfile: StateFlow<User?> = _userProfile.asStateFlow()

    private val _createdEvents = MutableStateFlow<List<Event>>(emptyList())
    val createdEvents: StateFlow<List<Event>> = _createdEvents.asStateFlow()

    // State to know if the current user is following the profile user
    private val _isFollowing = MutableStateFlow(false)
    val isFollowing: StateFlow<Boolean> = _isFollowing.asStateFlow()

    // State to manage loading during follow/unfollow action
    private val _isFollowActionLoading = MutableStateFlow(false)
    val isFollowActionLoading: StateFlow<Boolean> = _isFollowActionLoading.asStateFlow()

    private val _isLoadingProfile = MutableStateFlow(false)
    val isLoadingProfile: StateFlow<Boolean> = _isLoadingProfile.asStateFlow()

    private val _isLoadingEvents = MutableStateFlow(false)
    val isLoadingEvents: StateFlow<Boolean> = _isLoadingEvents.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private var userProfileListener: ListenerRegistration? = null
    private var createdEventsListener: ListenerRegistration? = null
    private var followingStatusListener: ListenerRegistration? = null // Listener for follow status

    fun loadUserProfile(profileUserId: String) {
        if (profileUserId.isBlank()) {
            _error.value = "User ID is invalid."
            _userProfile.value = null
            _isFollowing.value = false
            followingStatusListener?.remove() // Clean up listener
            return
        }

        _isLoadingProfile.value = true
        _error.value = null // Reset error

        // Detach previous listeners
        userProfileListener?.remove()
        followingStatusListener?.remove()
        _isFollowing.value = false // Reset follow state

        val currentAuthUserId = auth.currentUser?.uid

        // If it's not the current user's profile, set up follow status listener
        if (currentAuthUserId != null && profileUserId != currentAuthUserId) {
            listenToFollowingStatus(profileUserId, currentAuthUserId)
        } else {
            _isFollowing.value = false // Cannot follow self or not logged in
        }

        userProfileListener = db.collection("users").document(profileUserId)
            .addSnapshotListener { snapshot, e ->
                _isLoadingProfile.value = false
                if (e != null) {
                    Log.w("UserProfileVM", "Listen failed for user profile.", e)
                    _error.value = "Failed to load user profile: ${e.localizedMessage}"
                    _userProfile.value = null
                    // If profile fails to load, also reset follow status related to this profile
                    _isFollowing.value = false
                    followingStatusListener?.remove()
                    return@addSnapshotListener
                }

                if (snapshot != null && snapshot.exists()) {
                    try {
                        _userProfile.value = snapshot.toObject<User>()?.copy(uid = snapshot.id)
                    } catch (ex: Exception) {
                        Log.e("UserProfileVM", "Error deserializing user profile: ${snapshot.id}", ex)
                        _error.value = "Error processing user data."
                        _userProfile.value = null
                        _isFollowing.value = false
                        followingStatusListener?.remove()
                    }
                } else {
                    Log.d("UserProfileVM", "User profile document does not exist for UID: $profileUserId")
                    _error.value = "User profile not found."
                    _userProfile.value = null
                    _isFollowing.value = false
                    followingStatusListener?.remove()
                }
            }
    }

    private fun listenToFollowingStatus(profileUserId: String, currentLoggedInUserId: String) {
        followingStatusListener?.remove() // Ensure only one listener is active
        followingStatusListener = db.collection("followers")
            .whereEqualTo("userId", profileUserId) // The user being followed
            .whereEqualTo("followerID", currentLoggedInUserId) // The current logged-in user who is the follower
            .limit(1)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.w("UserProfileVM", "Listen failed for following status.", e)
                    _isFollowing.value = false // Default to not following on error
                    // Optionally set an error message specific to follow status
                    // _error.value = "Could not check follow status: ${e.localizedMessage}"
                    return@addSnapshotListener
                }
                _isFollowing.value = snapshot != null && !snapshot.isEmpty
                Log.d("UserProfileVM", "Following status for $profileUserId by $currentLoggedInUserId: ${_isFollowing.value}")
            }
    }

    fun loadCreatedEvents(userId: String) {
        if (userId.isBlank()) {
            _createdEvents.value = emptyList()
            return
        }

        _isLoadingEvents.value = true
        // _error.value = null // Consider if resetting general error here is wise, or have specific event error

        createdEventsListener?.remove()
        createdEventsListener = db.collection("Event") // Make sure "Event" is your correct collection name
            .whereEqualTo("ownerId", userId)
            .orderBy("startTime", Query.Direction.DESCENDING)
            .limit(20)
            .addSnapshotListener { snapshot, e ->
                _isLoadingEvents.value = false
                if (e != null) {
                    Log.w("UserProfileVM", "Listen failed for created events.", e)
                    _error.value = "Failed to load created events: ${e.localizedMessage}"
                    _createdEvents.value = emptyList()
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    try {
                        _createdEvents.value = snapshot.documents.mapNotNull { doc ->
                            doc.toObject<Event>()?.copy(id = doc.id)
                        }
                    } catch (ex: Exception) {
                        Log.e("UserProfileVM", "Error deserializing created events list", ex)
                        _error.value = "Error processing created events."
                        _createdEvents.value = emptyList()
                    }
                } else {
                    _createdEvents.value = emptyList()
                }
            }
    }

    fun toggleFollowUser(profileUserIdToFollow: String) {
        val currentAuthUserId = auth.currentUser?.uid
        if (currentAuthUserId == null) {
            _error.value = "You must be logged in to follow users."
            return
        }
        if (profileUserIdToFollow == currentAuthUserId) {
            _error.value = "You cannot follow yourself."
            return
        }

        _isFollowActionLoading.value = true
        viewModelScope.launch {
            try {
                val followersCollection = db.collection("followers")
                // Query to see if the follow relationship already exists
                val existingFollowQuery = followersCollection
                    .whereEqualTo("userId", profileUserIdToFollow)
                    .whereEqualTo("followerID", currentAuthUserId)
                    .limit(1)
                    .get()
                    .await()

                if (existingFollowQuery.isEmpty) {
                    // Not following: Create a new follow document
                    val following = Following(
                        userId = profileUserIdToFollow,
                        followerID = currentAuthUserId
                    )
                    followersCollection.add(following).await()
                    // _isFollowing will be updated by the listener
                    Log.d("UserProfileVM", "User $currentAuthUserId started following $profileUserIdToFollow")
                } else {
                    // Already following: Delete the follow document(s)
                    existingFollowQuery.documents.forEach { document ->
                        document.reference.delete().await()
                    }
                    // _isFollowing will be updated by the listener
                    Log.d("UserProfileVM", "User $currentAuthUserId unfollowed $profileUserIdToFollow")
                }
                _error.value = null // Clear previous follow/unfollow errors
            } catch (e: Exception) {
                Log.e("UserProfileVM", "Error toggling follow state for $profileUserIdToFollow", e)
                _error.value = "Failed to update follow status: ${e.localizedMessage}"
            } finally {
                _isFollowActionLoading.value = false
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        userProfileListener?.remove()
        createdEventsListener?.remove()
        followingStatusListener?.remove() // Important: remove this listener too
        Log.d("UserProfileVM", "ViewModel cleared, listeners removed.")
    }
}