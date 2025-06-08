package com.example.rievent.ui.createevent

import android.net.Uri
import com.google.android.libraries.places.api.model.AutocompletePrediction
import com.google.android.libraries.places.api.model.Place


data class CreateEventUiState(

    val name: String = "",
    val description: String = "",
    val category: String = "",
    val startDate: String = "",
    val startTime: String = "",
    val endDate: String = "",
    val endTime: String = "",
    val imageUri: Uri? = null,


    val addressInput: String = "",
    val addressPredictions: List<AutocompletePrediction> = emptyList(),
    val selectedPlace: Place? = null,
    val showPredictionsList: Boolean = false,


    val isCategoryMenuExpanded: Boolean = false,
    val isSubmitting: Boolean = false,
    val isFetchingPredictions: Boolean = false,


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