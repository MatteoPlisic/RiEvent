package com.example.rievent.ui.map

import Event
import java.time.LocalDate

/**
 * Represents the single, comprehensive state for the MapScreen.
 */
data class MapUiState(
    // Data States
    val allMapEvents: List<Event> = emptyList(),      // The date-filtered list of events for the map clusters
    val visibleEvents: List<Event> = emptyList(),     // Events currently visible in the map bounds AND list
    val selectedEventId: String? = null,              // For highlighting the selected event

    // Filter States
    val selectedDate: LocalDate? = null,

    // UI Control States
    val isDatePickerDialogVisible: Boolean = false,
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)