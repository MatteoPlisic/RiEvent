package com.example.rievent.models

data class EventRSPV(
    val eventId: String = "",
    val coming_users: List<Pair<String, String>>,
    val maybe_users: List<Pair<String, String>>,
    val not_coming_users: List<Pair<String, String>>,
)