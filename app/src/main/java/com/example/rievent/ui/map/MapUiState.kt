package com.example.rievent.ui.map

import Event
import java.time.LocalDate


data class MapUiState(

    val allMapEvents: List<Event> = emptyList(),
    val visibleEvents: List<Event> = emptyList(),
    val selectedEventId: String? = null,


    val selectedDate: LocalDate? = null,


    val isDatePickerDialogVisible: Boolean = false,
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)