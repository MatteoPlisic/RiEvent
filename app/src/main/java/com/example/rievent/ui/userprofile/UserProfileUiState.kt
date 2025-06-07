package com.example.rievent.ui.userprofile

import Event
import com.example.rievent.models.User

/**
 * Represents the single, comprehensive state for the UserProfileScreen.
 */
data class UserProfileUiState(
    // Data states
    val user: User? = null,
    val createdEvents: List<Event> = emptyList(),
    val isFollowing: Boolean = false,

    // UI Control states
    val isLoadingProfile: Boolean = true, // Start with profile loading
    val isLoadingEvents: Boolean = false, // Events can load after profile
    val isFollowActionLoading: Boolean = false, // For the follow/unfollow button
    val errorMessage: String? = null
)