package com.example.rievent.ui.updateevent

import Event
import android.net.Uri
import com.google.android.libraries.places.api.model.AutocompletePrediction
import com.google.android.libraries.places.api.model.Place

/**
 * Represents the single, comprehensive state for the UpdateEventScreen.
 */
data class UpdateEventUiState(
    // The original event loaded from Firestore. Null until loaded.
    val originalEvent: Event? = null,

    // --- Form Input Fields ---
    val name: String = "",
    val description: String = "",
    val category: String = "",
    val startDate: String = "",
    val startTime: String = "",
    val endDate: String = "",
    val endTime: String = "",
    val isPublic: Boolean = true,

    // [MODIFIED] State to handle multiple images
    val newImageUris: List<Uri> = emptyList(),          // New images selected by the user
    val existingImageUrls: List<String> = emptyList(), // URLs of images already in storage

    // --- Address Search State ---
    val addressInput: String = "",
    val addressPredictions: List<AutocompletePrediction> = emptyList(),
    val selectedPlace: Place? = null,
    val showPredictionsList: Boolean = false,

    // --- UI Control State ---
    val isInitialLoading: Boolean = true,
    val isUpdating: Boolean = false,
    val isFetchingPredictions: Boolean = false,
    val isCategoryMenuExpanded: Boolean = false,

    // --- Result State ---
    val updateSuccess: Boolean = false,
    val userMessage: String? = null
) {
    // Computed property to check if the form is valid for enabling the update button.
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