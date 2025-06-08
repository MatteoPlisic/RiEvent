package com.example.rievent.models

import com.google.firebase.Timestamp

data class EventRating(
    val id: String? = null,
    val eventId: String = "",
    val userId: String = "",
    val userName: String = "",
    val rating: Float = 0.0f,
    val createdAt: Timestamp = Timestamp.now()
)