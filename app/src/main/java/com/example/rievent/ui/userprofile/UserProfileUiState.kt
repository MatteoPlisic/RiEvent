package com.example.rievent.ui.userprofile

import Event
import com.example.rievent.models.User


data class UserProfileUiState(

    val user: User? = null,
    val createdEvents: List<Event> = emptyList(),
    val isFollowing: Boolean = false,


    val isLoadingProfile: Boolean = true,
    val isLoadingEvents: Boolean = false,
    val isFollowActionLoading: Boolean = false,
    val errorMessage: String? = null
)