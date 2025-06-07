package com.example.rievent.ui.myevents

import Event

/**
 * Represents the single, comprehensive state for the MyEventsScreen.
 */
data class MyEventsUiState(
    val myEvents: List<Event> = emptyList(),
    val isLoading: Boolean = true,

    // When an event is selected for deletion, this holds its data.
    // If this is null, the confirmation dialog is hidden.
    val eventToDelete: Event? = null
)