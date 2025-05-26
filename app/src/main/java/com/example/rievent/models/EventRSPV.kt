package com.example.rievent.models

data class EventRSPV(
    val eventId: String = "",
    val coming_users: List<RsvpUser> = emptyList(),
    val maybe_users: List<RsvpUser> = emptyList(),
    val not_coming_users: List<RsvpUser> = emptyList(),
)