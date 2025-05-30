package com.example.rievent.models

import com.google.firebase.Timestamp

data class EventComment(
    val id: String? = null,
    val eventId: String = "",
    val userId: String = "",
    val userName: String = "",
    val commentText: String = "",
    val createdAt: Timestamp = Timestamp.now(),
    val profileImageUrl: String? = null
)