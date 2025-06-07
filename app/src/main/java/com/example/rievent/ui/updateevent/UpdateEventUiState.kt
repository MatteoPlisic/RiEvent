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
    // These are initialized from originalEvent but can be changed by the user.
    val name: String = "",
    val description: String = "",
    val category: String = "",
    val startDate: String = "",
    val startTime: String = "",
    val endDate: String = "",
    val endTime: String = "",


    // --- Image State ---
    val newImageUri: Uri? = null,        // A new image selected by the user
    val displayImageUrl: String? = null, // The URL of the existing image from the loaded event

    // --- Address Search State ---
    val addressInput: String = "",
    val addressPredictions: List<AutocompletePrediction> = emptyList(),
    val selectedPlace: Place? = null,   // Holds the new lat/lng if user selects a new address
    val showPredictionsList: Boolean = false,

    // --- UI Control State ---
    val isInitialLoading: Boolean = true,   // True when first loading the event data
    val isUpdating: Boolean = false,        // True when the "Update" button is clicked
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