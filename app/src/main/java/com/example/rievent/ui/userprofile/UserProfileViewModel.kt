package com.example.rievent.ui.userprofile

import Event // Your Event data class
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.util.copy
import com.example.rievent.models.User // Your User data class
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

    private val _userProfile = MutableStateFlow<User?>(null)
    val userProfile: StateFlow<User?> = _userProfile.asStateFlow()

    private val _createdEvents = MutableStateFlow<List<Event>>(emptyList())
    val createdEvents: StateFlow<List<Event>> = _createdEvents.asStateFlow()

    // Optional: Events the user is attending (more complex to fetch, might need a different approach)
    // private val _attendingEvents = MutableStateFlow<List<Event>>(emptyList())
    // val attendingEvents: StateFlow<List<Event>> = _attendingEvents.asStateFlow()

    private val _isLoadingProfile = MutableStateFlow(false)
    val isLoadingProfile: StateFlow<Boolean> = _isLoadingProfile.asStateFlow()

    private val _isLoadingEvents = MutableStateFlow(false)
    val isLoadingEvents: StateFlow<Boolean> = _isLoadingEvents.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private var userProfileListener: ListenerRegistration? = null
    private var createdEventsListener: ListenerRegistration? = null

    fun loadUserProfile(userId: String) {
        if (userId.isBlank()) {
            _error.value = "User ID is invalid."
            _userProfile.value = null
            return
        }

        _isLoadingProfile.value = true
        _error.value = null // Clear previous errors

        userProfileListener?.remove() // Remove previous listener if any
        userProfileListener = db.collection("users").document(userId)
            .addSnapshotListener { snapshot, e ->
                _isLoadingProfile.value = false
                if (e != null) {
                    Log.w("UserProfileVM", "Listen failed for user profile.", e)
                    _error.value = "Failed to load user profile: ${e.localizedMessage}"
                    _userProfile.value = null
                    return@addSnapshotListener
                }

                if (snapshot != null && snapshot.exists()) {
                    try {
                        _userProfile.value = snapshot.toObject<User>()?.copy(uid = snapshot.id)
                    } catch (ex: Exception) {
                        Log.e("UserProfileVM", "Error deserializing user profile: ${snapshot.id}", ex)
                        _error.value = "Error processing user data."
                        _userProfile.value = null
                    }
                } else {
                    Log.d("UserProfileVM", "User profile document does not exist for UID: $userId")
                    _error.value = "User profile not found."
                    _userProfile.value = null
                }
            }
    }

    fun loadCreatedEvents(userId: String) {
        if (userId.isBlank()) {
            _createdEvents.value = emptyList()
            return
        }

        _isLoadingEvents.value = true
        _error.value = null // Clear previous errors for this specific load

        createdEventsListener?.remove()
        createdEventsListener = db.collection("Event") // Assuming your events collection is named "Event"
            .whereEqualTo("ownerId", userId)
            .orderBy("startTime", Query.Direction.DESCENDING) // Show newer events first
            .limit(20) // Limit the number of events for performance
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

    // Optional: Function to save/update user profile (if you allow editing)
    // fun updateUserProfile(user: User, newBio: String?, newPhotoUri: Uri?) { ... }


    override fun onCleared() {
        super.onCleared()
        userProfileListener?.remove()
        createdEventsListener?.remove()
        Log.d("UserProfileVM", "ViewModel cleared, listeners removed.")
    }
}