package com.example.rievent.ui.singleevent

import Event
import com.example.rievent.models.EventComment
import com.example.rievent.models.EventRSPV


data class SingleEventUiState(

    val event: Event? = null,
    val rsvp: EventRSPV? = null,
    val comments: List<EventComment> = emptyList(),


    val averageRating: Float = 0.0f,
    val totalRatings: Int = 0,
    val userRating: Float? = null,


    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val isRatingEnabled: Boolean = false,
    val isRatingDialogVisible: Boolean = false,
    val newCommentText: String = ""
)


