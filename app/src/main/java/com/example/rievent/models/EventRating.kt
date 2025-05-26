package com.example.rievent.models

import com.google.firebase.Timestamp

data class EventRating(
    val id: String? = null, // Firestore document ID
    val eventId: String = "",
    val userId: String = "",
    val userName: String = "", // Optional: for displaying who rated
    val rating: Float = 0.0f, // e.g., 1.0 to 5.0
    val createdAt: Timestamp = Timestamp.now()
)