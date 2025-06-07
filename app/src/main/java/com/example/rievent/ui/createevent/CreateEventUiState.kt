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
    val imageUri: Uri? = null,

    // Address Search State
    val addressInput: String = "",
    val addressPredictions: List<AutocompletePrediction> = emptyList(),
    val selectedPlace: Place? = null,
    val showPredictionsList: Boolean = false,

    // UI Control State
    val isCategoryMenuExpanded: Boolean = false,
    val isSubmitting: Boolean = false, // The main "Create" button loading state
    val isFetchingPredictions: Boolean = false, // Specific loading for address autocomplete

    // Result State
    val creationSuccess: Boolean = false,
    val userMessage: String? = null // For displaying any error or success message
) {
    // Computed property to determine if the form is valid and the create button can be enabled.
    // This moves the validation logic from the UI into the state itself, making it more reusable.
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