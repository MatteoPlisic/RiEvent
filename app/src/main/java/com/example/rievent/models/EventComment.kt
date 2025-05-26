package com.example.rievent.models

import com.google.firebase.Timestamp

data class EventComment(
    val id: String? = null, // Firestore document ID
    val eventId: String = "",
    val userId: String = "",
    val userName: String = "", // For displaying who commented
    val commentText: String = "",
    val createdAt: Timestamp = Timestamp.now(),
    val profileImageUrl: String? = null // Optional: user's profile image
)