package com.example.rievent.ui.updateevent

import Event // Assuming this is your data class: data class Event(var id: String? = null, ..., var imageUrl: String? = null)
import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel // Changed to AndroidViewModel for Application context
import androidx.lifecycle.viewModelScope
import com.google.android.gms.common.api.ApiException
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.AutocompletePrediction
import com.google.android.libraries.places.api.model.AutocompleteSessionToken
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.model.TypeFilter
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID


class UpdateEventViewModel(application: Application) : AndroidViewModel(application) { // Changed to AndroidViewModel
    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private var placesClient: PlacesClient = Places.createClient(application.applicationContext)
    private var token: AutocompleteSessionToken = AutocompleteSessionToken.newInstance()

    private val _event = MutableStateFlow<Event?>(null)
    val event = _event.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _isSuccess = MutableStateFlow(false)
    val isSuccess = _isSuccess.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage = _errorMessage.asStateFlow()

    // For Address Autocomplete
    private val _addressPredictions = MutableStateFlow<List<AutocompletePrediction>>(emptyList())
    val addressPredictions = _addressPredictions.asStateFlow()

    private val _isFetchingPredictions = MutableStateFlow(false)
    val isFetchingPredictions = _isFetchingPredictions.asStateFlow()


    fun loadEvent(eventId: String) {
        _isLoading.value = true
        _errorMessage.value = null
        db.collection("Event").document(eventId)
            .get()
            .addOnSuccessListener { doc ->
                val loadedEvent = doc.toObject(Event::class.java)
                if (loadedEvent != null) {
                    _event.value = loadedEvent.copy(id = doc.id) // Ensure ID is set
                } else {
                    _errorMessage.value = "Event not found."
                }
                _isLoading.value = false
            }
            .addOnFailureListener {
                _errorMessage.value = "Failed to load event: ${it.localizedMessage}"
                _isLoading.value = false
            }
    }

    fun updateEventWithOptionalNewImage(
        eventData: Event,
        newImageUri: Uri?,
        removeCurrentImage: Boolean
    ) {
        if (eventData.id.isNullOrBlank()) {
            _errorMessage.value = "Event ID is missing for update."
            return
        }
        _isLoading.value = true
        _isSuccess.value = false
        _errorMessage.value = null

        viewModelScope.launch {
            try {
                var finalImageUrl = eventData.imageUrl // Start with the current image URL

                // 1. Handle image deletion if requested or if new image replaces old
                if (removeCurrentImage || (newImageUri != null && !eventData.imageUrl.isNullOrBlank())) {
                    eventData.imageUrl?.let { oldImageUrl ->
                        if (oldImageUrl.startsWith("gs://")) { // Check if it's a Firebase Storage URL
                            try {
                                storage.getReferenceFromUrl(oldImageUrl).delete().await()
                                finalImageUrl = null // Image removed or will be replaced
                            } catch (e: Exception) {
                                // Log or optionally inform user, but don't block update for this
                                println("Error deleting old image: ${e.message}")
                            }
                        }
                    }
                }

                // 2. Handle new image upload
                if (newImageUri != null) {
                    val imageFileName = "event_images/${UUID.randomUUID()}"
                    val imageRef = storage.reference.child(imageFileName)
                    imageRef.putFile(newImageUri).await() // Upload
                    finalImageUrl = imageRef.downloadUrl.await().toString() // Get new URL
                }

                // 3. Update event data with the final image URL
                val eventToUpdate = eventData.copy(imageUrl = finalImageUrl)

                // 4. Update Firestore document
                db.collection("Event").document(eventData.id!!)
                    .set(eventToUpdate)
                    .await() // Use await for coroutine compatibility

                _isSuccess.value = true
            } catch (e: Exception) {
                _errorMessage.value = "Update failed: ${e.localizedMessage}"
            } finally {
                _isLoading.value = false
            }
        }
    }


    fun resetState() {
        _isSuccess.value = false
        _errorMessage.value = null
        // _event.value = null // Optional: clear event details after successful update if navigating away
        clearAddressSearchStates() // Clear address predictions too
    }

    // --- Address Autocomplete Methods ---
    fun fetchAddressPredictions(query: String) {
        if (query.isBlank()) {
            _addressPredictions.value = emptyList()
            _isFetchingPredictions.value = false
            return
        }
        _isFetchingPredictions.value = true

        val request = FindAutocompletePredictionsRequest.builder()
            .setSessionToken(token)
            .setQuery(query)
            .setTypeFilter(TypeFilter.ADDRESS) // Or other filters as needed
            .build()

        placesClient.findAutocompletePredictions(request)
            .addOnSuccessListener { response ->
                _addressPredictions.value = response.autocompletePredictions
                _isFetchingPredictions.value = false
            }
            .addOnFailureListener { exception ->
                if (exception is ApiException) {
                    _errorMessage.value = "Place API error: ${exception.statusCode}"
                } else {
                    _errorMessage.value = "Failed to fetch address predictions: ${exception.localizedMessage}"
                }
                _addressPredictions.value = emptyList()
                _isFetchingPredictions.value = false
            }
    }

    fun fetchPlaceDetails(prediction: AutocompletePrediction, onResult: (Place) -> Unit) {
        val placeFields = listOf(Place.Field.ID, Place.Field.ADDRESS, Place.Field.LAT_LNG)
        val request = FetchPlaceRequest.builder(prediction.placeId, placeFields)
            .setSessionToken(token) // Reuse the same token
            .build()

        placesClient.fetchPlace(request)
            .addOnSuccessListener { response ->
                onResult(response.place)
                token = AutocompleteSessionToken.newInstance() // Refresh token after use for details
            }
            .addOnFailureListener { exception ->
                if (exception is ApiException) {
                    _errorMessage.value = "Place Details API error: ${exception.statusCode}"
                } else {
                    _errorMessage.value = "Failed to fetch place details: ${exception.localizedMessage}"
                }
            }
    }

    fun clearAddressSearchStates() {
        _addressPredictions.value = emptyList()
        _isFetchingPredictions.value = false
        // Don't reset the token here, it should be valid for a session
    }
}