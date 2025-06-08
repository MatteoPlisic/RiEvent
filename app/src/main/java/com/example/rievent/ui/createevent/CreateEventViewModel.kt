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
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.UUID

class CreateEventViewModel(application: Application) : ViewModel() {

    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private val placesClient: PlacesClient = Places.createClient(application)
    private val auth = FirebaseAuth.getInstance()

    private val _uiState = MutableStateFlow(CreateEventUiState())
    val uiState = _uiState.asStateFlow()

    private var fetchPredictionsJob: Job? = null

    val primorjeGorskiKotarBounds = RectangularBounds.newInstance(
        LatLng(44.85, 14.15),
        LatLng(45.75, 15.05)
    )

    // --- UI EVENT HANDLERS ---
    fun onNameChange(name: String) { _uiState.update { it.copy(name = name) } }
    fun onDescriptionChange(description: String) { _uiState.update { it.copy(description = description) } }
    fun onCategoryChange(category: String) { _uiState.update { it.copy(category = category, isCategoryMenuExpanded = false) } }
    fun onStartDateChange(date: String) { _uiState.update { it.copy(startDate = date) } }
    fun onStartTimeChange(time: String) { _uiState.update { it.copy(startTime = time) } }
    fun onEndDateChange(date: String) { _uiState.update { it.copy(endDate = date) } }
    fun onEndTimeChange(time: String) { _uiState.update { it.copy(endTime = time) } }
    fun onCategoryMenuToggled(isExpanded: Boolean) { _uiState.update { it.copy(isCategoryMenuExpanded = isExpanded) } }

    // [MODIFIED] Add a new Uri to the list
    fun onImageSelected(uri: Uri) {
        _uiState.update { it.copy(imageUris = it.imageUris + uri) }
    }

    // [NEW] Remove a specific Uri from the list
    fun onImageRemoved(uri: Uri) {
        _uiState.update { it.copy(imageUris = it.imageUris - uri) }
    }

    fun onAddressInputChange(query: String) {
        _uiState.update { it.copy(addressInput = query, selectedPlace = null, showPredictionsList = true) }
        fetchAddressPredictions(query)
    }

    fun onPredictionSelected(prediction: AutocompletePrediction) {
        _uiState.update { it.copy(isSubmitting = true, showPredictionsList = false) }
        val placeId = prediction.placeId
        val placeFields = listOf(Place.Field.ID, Place.Field.NAME, Place.Field.ADDRESS, Place.Field.LAT_LNG)
        val request = FetchPlaceRequest.builder(placeId, placeFields).build()
        viewModelScope.launch {
            try {
                val response = placesClient.fetchPlace(request).await()
                _uiState.update {
                    it.copy(
                        selectedPlace = response.place,
                        addressInput = response.place.address ?: prediction.getPrimaryText(null).toString(),
                        isSubmitting = false,
                        addressPredictions = emptyList()
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(userMessage = "Could not get place details.", isSubmitting = false) }
            }
        }
    }

    fun onAddressFocusChanged(isFocused: Boolean) {
        if (!isFocused) {
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

    fun eventCreationNavigated() {
        _uiState.value = CreateEventUiState()
    }

    // --- MAIN LOGIC ---
    fun createEvent() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, creationSuccess = false, userMessage = null) }
            val currentState = _uiState.value

            // [MODIFIED] Upload multiple images and collect their URLs
            val imageUrls = try {
                uploadImagesToStorage(currentState.imageUris)
            } catch (e: Exception) {
                Log.e("CreateEventVM", "Image upload process failed", e)
                _uiState.update { it.copy(userMessage = "Image upload failed: ${e.message}", isSubmitting = false) }
                return@launch
            }

            val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
            val startTs = runCatching { Timestamp(formatter.parse("${currentState.startDate} ${currentState.startTime}")!!) }.getOrNull()
            val endTs = runCatching { Timestamp(formatter.parse("${currentState.endDate} ${currentState.endTime}")!!) }.getOrNull()
            val geoPt = currentState.selectedPlace?.latLng?.let { GeoPoint(it.latitude, it.longitude) }
            val ownerName = auth.currentUser?.displayName ?: "Anonymous"
            val ownerId = auth.currentUser?.uid ?: ""

            // Create the event with the list of image URLs
            val event = Event(
                name = currentState.name.trim(), description = currentState.description.trim(), category = currentState.category,
                ownerId = ownerId, startTime = startTs, endTime = endTs,
                address = currentState.addressInput.trim(), location = geoPt,
                ownerName = ownerName, createdAt = Timestamp.now(),
                imageUrls = imageUrls // [MODIFIED] Use imageUrls (list)
            )

            try {
                val documentReference: DocumentReference = db.collection("Event").add(event).await()
                val eventId = documentReference.id
                val rsvpDoc = EventRSPV(eventId = eventId)
                db.collection("event_rspv").add(rsvpDoc).await()
                _uiState.update { it.copy(isSubmitting = false, creationSuccess = true) }
            } catch (e: Exception) {
                Log.e("CreateEventVM", "Error creating event document", e)
                _uiState.update { it.copy(userMessage = "Error creating event: ${e.message}", isSubmitting = false) }
            }
        }
    }

    // [NEW] Helper function to upload a list of images concurrently
    private suspend fun uploadImagesToStorage(imageUris: List<Uri>): List<String> {
        if (imageUris.isEmpty()) return emptyList()

        // Use async to start all uploads in parallel
        val uploadJobs = imageUris.map { uri ->
            viewModelScope.async {
                val fileName = "event_images/${UUID.randomUUID()}"
                val imageRef: StorageReference = storage.reference.child(fileName)
                imageRef.putFile(uri).await()
                imageRef.downloadUrl.await().toString()
            }
        }
        // Wait for all parallel jobs to complete and return the list of URLs
        return uploadJobs.awaitAll()
    }

    private fun fetchAddressPredictions(query: String) {
        fetchPredictionsJob?.cancel()
        if (query.length < 2) {
            _uiState.update { it.copy(addressPredictions = emptyList(), isFetchingPredictions = false, showPredictionsList = false) }
            return
        }
        fetchPredictionsJob = viewModelScope.launch {
            delay(300)
            _uiState.update { it.copy(isFetchingPredictions = true, userMessage = null) }
            val request = FindAutocompletePredictionsRequest.builder()
                .setCountries("HR").setLocationRestriction(primorjeGorskiKotarBounds).setQuery(query).build()
            try {
                val response = placesClient.findAutocompletePredictions(request).await()
                _uiState.update {
                    it.copy(
                        addressPredictions = response.autocompletePredictions,
                        isFetchingPredictions = false,
                        showPredictionsList = response.autocompletePredictions.isNotEmpty()
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(addressPredictions = emptyList(), isFetchingPredictions = false, showPredictionsList = false, userMessage = "Address lookup failed.") }
            }
        }
    }
}