package com.example.rievent.ui.myevents

import Event


data class MyEventsUiState(
    val myEvents: List<Event> = emptyList(),
    val isLoading: Boolean = true,


    val eventToDelete: Event? = null
)