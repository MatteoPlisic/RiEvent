package com.example.rievent.ui.updateevent

import Event
import android.net.Uri
import com.google.android.libraries.places.api.model.AutocompletePrediction
import com.google.android.libraries.places.api.model.Place

data class UpdateEventUiState(
    val originalEvent: Event? = null,
    val name: String = "",
    val description: String = "",
    val category: String = "",
    val startDate: String = "",
    val startTime: String = "",
    val endDate: String = "",
    val endTime: String = "",
    val isPublic: Boolean = true,
    val newImageUris: List<Uri> = emptyList(),
    val existingImageUrls: List<String> = emptyList(),
    val addressInput: String = "",
    val addressPredictions: List<AutocompletePrediction> = emptyList(),
    val selectedPlace: Place? = null,
    val showPredictionsList: Boolean = false,
    val isInitialLoading: Boolean = true,
    val isUpdating: Boolean = false,
    val isFetchingPredictions: Boolean = false,
    val isCategoryMenuExpanded: Boolean = false,
    val updateSuccess: Boolean = false,
    val userMessage: String? = null
) {
    val isFormValid: Boolean
        get() = name.isNotBlank() &&
                description.isNotBlank() &&
                category.isNotBlank() &&
                startDate.isNotBlank() &&
                startTime.isNotBlank() &&
                endDate.isNotBlank() &&
                endTime.isNotBlank() &&
                addressInput.isNotBlank()
}