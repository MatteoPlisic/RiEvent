package com.example.rievent.ui.updateevent

import Event
import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.AutocompletePrediction
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.model.RectangularBounds
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.UUID

class UpdateEventViewModel(application: Application) : AndroidViewModel(application) {
    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private val placesClient: PlacesClient = Places.createClient(application)

    private val _uiState = MutableStateFlow(UpdateEventUiState())
    val uiState = _uiState.asStateFlow()

    private var fetchPredictionsJob: Job? = null

    // Bounding box for Places API to bias results (optional but recommended)
    private val primorjeGorskiKotarBounds = RectangularBounds.newInstance(
        LatLng(44.85, 14.15),
        LatLng(45.75, 15.05)
    )

    // --- DATA LOADING ---
    fun loadEvent(eventId: String) {
        if (eventId.isBlank()) {
            _uiState.update { it.copy(userMessage = "Event ID is missing.", isInitialLoading = false) }
            return
        }
        _uiState.update { it.copy(isInitialLoading = true) }
        viewModelScope.launch {
            try {
                val doc = db.collection("Event").document(eventId).get().await()
                val loadedEvent = doc.toObject(Event::class.java)?.copy(id = doc.id)
                if (loadedEvent != null) {
                    populateStateFromEvent(loadedEvent)
                } else {
                    _uiState.update { it.copy(userMessage = "Event not found.", isInitialLoading = false) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(userMessage = "Failed to load event.", isInitialLoading = false) }
            }
        }
    }

    private fun populateStateFromEvent(event: Event) {
        val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val timeFormatter = SimpleDateFormat("HH:mm", Locale.getDefault())

        _uiState.update {
            it.copy(
                originalEvent = event,
                name = event.name,
                description = event.description,
                category = event.category,
                startDate = event.startTime?.toDate()?.let { d -> dateFormatter.format(d) } ?: "",
                startTime = event.startTime?.toDate()?.let { d -> timeFormatter.format(d) } ?: "",
                endDate = event.endTime?.toDate()?.let { d -> dateFormatter.format(d) } ?: "",
                endTime = event.endTime?.toDate()?.let { d -> timeFormatter.format(d) } ?: "",
                displayImageUrl = event.imageUrl,
                addressInput = event.address,
                isInitialLoading = false // Loading is complete
            )
        }
    }

    // --- UI EVENT HANDLERS ---
    fun onNameChange(name: String) { _uiState.update { it.copy(name = name) } }
    fun onDescriptionChange(description: String) { _uiState.update { it.copy(description = description) } }
    fun onCategoryChange(category: String) { _uiState.update { it.copy(category = category, isCategoryMenuExpanded = false) } }
    fun onStartDateChange(date: String) { _uiState.update { it.copy(startDate = date) } }
    fun onStartTimeChange(time: String) { _uiState.update { it.copy(startTime = time) } }
    fun onEndDateChange(date: String) { _uiState.update { it.copy(endDate = date) } }
    fun onEndTimeChange(time: String) { _uiState.update { it.copy(endTime = time) } }
    fun onCategoryMenuToggled(isExpanded: Boolean) { _uiState.update { it.copy(isCategoryMenuExpanded = isExpanded) } }

    fun onImageSelected(uri: Uri?) {
        _uiState.update { it.copy(newImageUri = uri) }
    }

    fun onRemoveImage() {
        _uiState.update { it.copy(newImageUri = null, displayImageUrl = null) }
    }

    fun onAddressInputChange(query: String) {
        _uiState.update { it.copy(addressInput = query, selectedPlace = null) }
        fetchAddressPredictions(query)
    }

    fun onPredictionSelected(prediction: AutocompletePrediction) {
        _uiState.update { it.copy(isUpdating = true, showPredictionsList = false) }
        val placeId = prediction.placeId
        val placeFields = listOf(Place.Field.ID, Place.Field.NAME, Place.Field.ADDRESS, Place.Field.LAT_LNG)
        val request = FetchPlaceRequest.builder(placeId, placeFields).build()

        viewModelScope.launch {
            try {
                val response = placesClient.fetchPlace(request).await()
                val place = response.place
                _uiState.update {
                    it.copy(
                        selectedPlace = place,
                        addressInput = place.address ?: prediction.getPrimaryText(null).toString(),
                        isUpdating = false,
                        addressPredictions = emptyList()
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(userMessage = "Could not get place details.", isUpdating = false) }
            }
        }
    }

    fun onAddressFocusChanged(isFocused: Boolean) {
        val currentState = _uiState.value
        if (isFocused && currentState.addressInput.isNotBlank() && currentState.addressPredictions.isNotEmpty()) {
            _uiState.update { it.copy(showPredictionsList = true) }
        } else if (!isFocused) {
            viewModelScope.launch {
                delay(200)
                _uiState.update { it.copy(showPredictionsList = false) }
            }
        }
    }

    fun onClearAddress() {
        _uiState.update { it.copy(addressInput = "", selectedPlace = null, addressPredictions = emptyList(), showPredictionsList = false) }
        fetchPredictionsJob?.cancel()
    }

    fun onUpdateNavigated() {
        _uiState.update { it.copy(updateSuccess = false) }
    }

    // --- MAIN UPDATE LOGIC ---
    fun updateEvent() {
        val currentState = _uiState.value
        val originalEvent = currentState.originalEvent ?: return Unit.also {
            _uiState.update { it.copy(userMessage = "Original event data is missing.") }
        }
        _uiState.update { it.copy(isUpdating = true, userMessage = null) }
        viewModelScope.launch {
            try {
                var finalImageUrl = originalEvent.imageUrl
                val imageRemoved = currentState.displayImageUrl == null && currentState.newImageUri == null && originalEvent.imageUrl != null

                if (imageRemoved || (currentState.newImageUri != null && !originalEvent.imageUrl.isNullOrBlank())) {
                    originalEvent.imageUrl?.let { oldUrl ->
                        try { storage.getReferenceFromUrl(oldUrl).delete().await() }
                        catch (e: Exception) { Log.w("UpdateEventVM", "Failed to delete old image, proceeding anyway.", e) }
                    }
                    finalImageUrl = null
                }

                // --- THIS IS THE CORRECTED BLOCK ---
                if (currentState.newImageUri != null) {
                    val fileName = "event_images/${UUID.randomUUID()}"

                    // [THE FIX] Define imageRef using your storage instance and a child path.
                    val imageRef = storage.reference.child(fileName)

                    imageRef.putFile(currentState.newImageUri).await()
                    finalImageUrl = imageRef.downloadUrl.await().toString()
                }
                // --- END OF CORRECTION ---

                val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                val startTs = runCatching { Timestamp(formatter.parse("${currentState.startDate} ${currentState.startTime}")!!) }.getOrNull()
                val endTs = runCatching { Timestamp(formatter.parse("${currentState.endDate} ${currentState.endTime}")!!) }.getOrNull()
                val geoPt = currentState.selectedPlace?.latLng?.let { GeoPoint(it.latitude, it.longitude) } ?: originalEvent.location

                val updatedEvent = originalEvent.copy(
                    name = currentState.name, description = currentState.description, category = currentState.category,
                    startTime = startTs, endTime = endTs, address = currentState.addressInput,
                    location = geoPt, imageUrl = finalImageUrl
                )

                db.collection("Event").document(originalEvent.id!!).set(updatedEvent).await()
                _uiState.update { it.copy(isUpdating = false, updateSuccess = true) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isUpdating = false, userMessage = "Update failed: ${e.message}") }
            }
        }
    }

    private fun fetchAddressPredictions(query: String) {
        fetchPredictionsJob?.cancel()
        if (query.isBlank() || query.length < 2) {
            _uiState.update { it.copy(addressPredictions = emptyList(), isFetchingPredictions = false, showPredictionsList = false) }
            return
        }
        fetchPredictionsJob = viewModelScope.launch {
            delay(300)
            _uiState.update { it.copy(isFetchingPredictions = true, userMessage = null) }
            val request = FindAutocompletePredictionsRequest.builder()
                .setCountries("HR")
                .setLocationRestriction(primorjeGorskiKotarBounds)
                .setQuery(query)
                .build()
            try {
                val response = placesClient.findAutocompletePredictions(request).await()
                // [THE FIX] After getting predictions, set the flag to show them.
                _uiState.update {
                    it.copy(
                        addressPredictions = response.autocompletePredictions,
                        isFetchingPredictions = false,
                        // Only show the list if predictions were actually found
                        showPredictionsList = response.autocompletePredictions.isNotEmpty()
                    )
                }
            } catch (e: Exception) {
                Log.e("CreateEventVM", "Autocomplete fetch failed", e)
                _uiState.update { it.copy(addressPredictions = emptyList(), isFetchingPredictions = false, showPredictionsList = false, userMessage = "Address lookup failed.") }
            }
        }
    }
}