package com.example.rievent.ui.singleevent

import Event
import com.example.rievent.models.EventComment
import com.example.rievent.models.EventRSPV

/**
 * Represents the single, comprehensive state for the SingleEventScreen.
 */
data class SingleEventUiState(
    // Main data for the screen
    val event: Event? = null,
    val rsvp: EventRSPV? = null,
    val comments: List<EventComment> = emptyList(),

    // Calculated data for display
    val averageRating: Float = 0.0f,
    val totalRatings: Int = 0,
    val userRating: Float? = null, // Just the float value is needed for the UI

    // UI Control State
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val isRatingEnabled: Boolean = false, // To enable/disable the rating button
    val isRatingDialogVisible: Boolean = false,
    val newCommentText: String = ""
)

// The RsvpStatus enum can live here or in a shared file if used elsewhere.
