package com.example.rievent.models

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId


data class User(
    @DocumentId val uid: String = "",
    val displayName: String? = null,
    val email: String? = null,
    val photoUrl: String? = null,
    val bio: String? = null,
    val createdAt: Timestamp? = Timestamp.now(),
    val id: String = ""
)