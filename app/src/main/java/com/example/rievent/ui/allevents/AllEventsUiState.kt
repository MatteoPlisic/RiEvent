package com.example.rievent.ui.allevents

import Event
import android.location.Location
import com.example.rievent.models.EventRSPV
import java.time.LocalDate

/**
 * Represents the entire state of the AllEventsScreen.
 * This single class holds all the information the UI needs to draw itself,
 * including filter values, dialog visibility, and the list of events to display.
 */
public data class AllEventsUiState(
    // Data lists
    val displayedEvents: List<Event> = emptyList(),
    val eventsRsvpsMap: Map<String, EventRSPV?> = emptyMap(),

    // Filter states
    val searchText: String = "",
    val searchByUser: Boolean = false,
    val selectedCategory: String = "Any",
    val selectedDate: LocalDate? = null,
    val distanceFilterKm: Float = 50f, // 50f represents "Any" distance

    // Location state
    val userLocation: Location? = null,

    // UI control states
    val isCategoryMenuExpanded: Boolean = false,
    val isDatePickerDialogVisible: Boolean = false,

    // Add a loading state for better user experience
    val isLoading: Boolean = true,
    val hasAppliedFilters: Boolean = false // To differentiate between "no events" and "no results"
)

// You can also keep the RsvpStatus enum here or in a more general 'models' file.
enum class RsvpStatus {
    COMING,
    MAYBE,
    NOT_COMING
}