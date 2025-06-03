package com.example.rievent.ui.createevent

import Event
import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.rievent.models.EventRSPV
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.AutocompletePrediction
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.model.RectangularBounds
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID

class CreateEventViewModel(application: Application) : ViewModel() {

    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private val placesClient: PlacesClient = Places.createClient(application)

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isSuccess = MutableStateFlow(false)
    val isSuccess: StateFlow<Boolean> = _isSuccess.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // --- Autocomplete States ---
    private val _addressPredictions = MutableStateFlow<List<AutocompletePrediction>>(emptyList())
    val addressPredictions: StateFlow<List<AutocompletePrediction>> = _addressPredictions.asStateFlow()

    private val _isFetchingPredictions = MutableStateFlow(false)
    val isFetchingPredictions: StateFlow<Boolean> = _isFetchingPredictions.asStateFlow()

    private val _selectedPlace = MutableStateFlow<Place?>(null)
    val selectedPlace: StateFlow<Place?> = _selectedPlace.asStateFlow()

    private var fetchPredictionsJob: Job? = null

    val primorjeGorskiKotarBounds = RectangularBounds.newInstance(
        LatLng(44.85, 14.15), // Approximate Southwest corner (e.g., south of Cres/Lošinj)
        LatLng(45.75, 15.05)  // Approximate Northeast corner (e.g., northeast of Čabar)
    )

    fun createEventWithImage(event: Event, imageUri: Uri?) {
        viewModelScope.launch {
            _isLoading.value = true
            _isSuccess.value = false
            _errorMessage.value = null // Clear previous general errors
            var finalEvent = event

            try {
                if (imageUri != null) {
                    val imageUrl = uploadImageToStorage(imageUri)
                    if (imageUrl == null) { // uploadImageToStorage sets its own _errorMessage
                        _isLoading.value = false
                        return@launch
                    }
                    finalEvent = event.copy(imageUrl = imageUrl)
                }

                val documentReference: DocumentReference = db.collection("Event").add(finalEvent).await()
                val eventId = documentReference.id

                // Create an RSVP document for the new event
                db.collection("event_rspv").add(
                    EventRSPV(
                        eventId = eventId,
                        coming_users = emptyList(),
                        maybe_users = emptyList(),
                        not_coming_users = emptyList()
                    )
                ).await()

                _isLoading.value = false
                _isSuccess.value = true
                clearAddressSearchStates() // Clear address related states on success
            } catch (e: Exception) {
                _isLoading.value = false
                _isSuccess.value = false
                _errorMessage.value = "Error creating event: ${e.message}"
                Log.e("CreateEventVM", "Error creating event", e)
            }
        }
    }

    private suspend fun uploadImageToStorage(imageUri: Uri): String? {
        return try {
            val fileName = "event_images/${UUID.randomUUID()}" // Unique name for the image
            val imageRef: StorageReference = storage.reference.child(fileName)
            imageRef.putFile(imageUri).await() // Upload the file
            imageRef.downloadUrl.await().toString() // Get the download URL
        } catch (e: Exception) {
            _errorMessage.value = "Image upload failed: ${e.message}"
            Log.e("CreateEventVM", "Image upload failed", e)
            null
        }
    }

    fun fetchAddressPredictions(query: String) {
        fetchPredictionsJob?.cancel() // Cancel any existing job

        if (query.isBlank() || query.length < 2) { // Start search after 2 characters
            _addressPredictions.value = emptyList()
            _isFetchingPredictions.value = false
            return
        }

        fetchPredictionsJob = viewModelScope.launch {
            delay(300) // Debounce: wait 300ms after user stops typing
            _isFetchingPredictions.value = true
            _errorMessage.value = null // Clear previous address-specific errors


            val request = FindAutocompletePredictionsRequest.builder()
                // Optional: Bias results to specific countries for better relevance.
                // Example: .setCountries("US", "CA")
                .setCountries("HR")
                .setLocationRestriction(primorjeGorskiKotarBounds)
                .setQuery(query)
                .build()

            try {
                val response = placesClient.findAutocompletePredictions(request).await()
                _addressPredictions.value = response.autocompletePredictions
            } catch (e: Exception) {
                if (e is CancellationException) throw e // Re-throw cancellation
                Log.e("CreateEventVM", "Autocomplete fetch failed for query: $query", e)
                // Avoid showing generic error for no results, as an empty list is the indicator
                if (e.message?.contains("REQUEST_DENIED", ignoreCase = true) == true) {
                    _errorMessage.value = "Address lookup error (API Key issue?)."
                } else {
                    // Could be network or other issue
                    _errorMessage.value = "Address lookup failed."
                }
                _addressPredictions.value = emptyList()
            } finally {
                _isFetchingPredictions.value = false
            }
        }
    }

    fun fetchPlaceDetails(prediction: AutocompletePrediction, onDetailsFetched: (place: Place) -> Unit) {
        val placeId = prediction.placeId
        val placeFields = listOf(
            Place.Field.ID,
            Place.Field.NAME,
            Place.Field.ADDRESS,
            Place.Field.LAT_LNG
            // Add Place.Field.ADDRESS_COMPONENTS if you need to break down address parts
        )

        val request = FetchPlaceRequest.builder(placeId, placeFields).build()
        _isLoading.value = true // Show general loading for this direct action

        viewModelScope.launch {
            try {
                val response = placesClient.fetchPlace(request).await()
                val place = response.place
                _selectedPlace.value = place
                onDetailsFetched(place)
                _addressPredictions.value = emptyList() // Clear predictions after selection
                _errorMessage.value = null // Clear error on success
            } catch (e: Exception) {
                Log.e("CreateEventVM", "Place details fetch failed for ID: $placeId", e)
                _errorMessage.value = "Could not get place details."
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearAddressSearchStates() {
        _addressPredictions.value = emptyList()
        _selectedPlace.value = null
        _isFetchingPredictions.value = false
        fetchPredictionsJob?.cancel() // Cancel any pending prediction job
        // Don't clear _errorMessage here as it might be a general error
    }

    fun resetState() {
        _isSuccess.value = false
        _errorMessage.value = null // Clear any lingering error messages
        clearAddressSearchStates()
        // _isLoading is controlled by specific operations, typically set to false when an operation ends.
    }
}