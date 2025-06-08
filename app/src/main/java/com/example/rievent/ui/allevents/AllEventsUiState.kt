package com.example.rievent.ui.allevents

import Event
import android.location.Location
import com.example.rievent.models.EventRSPV
import java.time.LocalDate


public data class AllEventsUiState(

    val displayedEvents: List<Event> = emptyList(),
    val eventsRsvpsMap: Map<String, EventRSPV?> = emptyMap(),


    val searchText: String = "",
    val searchByUser: Boolean = false,
    val selectedCategory: String = "Any",
    val selectedDate: LocalDate? = null,
    val distanceFilterKm: Float = 50f,


    val userLocation: Location? = null,


    val isCategoryMenuExpanded: Boolean = false,
    val isDatePickerDialogVisible: Boolean = false,


    val isLoading: Boolean = true,
    val hasAppliedFilters: Boolean = false
)


enum class RsvpStatus {
    COMING,
    MAYBE,
    NOT_COMING
}