package com.example.rievent.ui.createevent

import android.net.Uri
import com.google.android.libraries.places.api.model.AutocompletePrediction
import com.google.android.libraries.places.api.model.Place

/**
 * Represents the single, comprehensive state for the CreateEventScreen.
 */
data class CreateEventUiState(
    // User Input Fields
    val name: String = "",
    val description: String = "",
    val category: String = "",
    val startDate: String = "",
    val startTime: String = "",
    val endDate: String = "",
    val endTime: String = "",
    val isPublic: Boolean = true, // Added isPublic state

    // [MODIFIED] Changed from a single Uri to a list of Uris for multiple images
    val imageUris: List<Uri> = emptyList(),

    // Address Search State
    val addressInput: String = "",
    val addressPredictions: List<AutocompletePrediction> = emptyList(),
    val selectedPlace: Place? = null,
    val showPredictionsList: Boolean = false,

    // UI Control State
    val isCategoryMenuExpanded: Boolean = false,
    val isSubmitting: Boolean = false,
    val isFetchingPredictions: Boolean = false,

    // Result State
    val creationSuccess: Boolean = false,
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