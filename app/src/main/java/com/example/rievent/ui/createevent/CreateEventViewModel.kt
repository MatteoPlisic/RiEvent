package com.example.rievent.ui.createevent

import android.net.Uri // Import Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import Event
import com.example.rievent.models.EventRSPV
import com.example.rievent.models.RsvpUser
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.storage.FirebaseStorage // Import Firebase Storage
import com.google.firebase.storage.StorageReference // Import StorageReference
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID // For unique image names

class CreateEventViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance() // Initialize Firebase Storage

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _isSuccess = MutableStateFlow(false)
    val isSuccess = _isSuccess.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage = _errorMessage.asStateFlow()



    fun createEventWithImage(event: Event, imageUri: Uri?) { // Modified function
        viewModelScope.launch {
            _isLoading.value = true
            _isSuccess.value = false
            _errorMessage.value = null
            var finalEvent = event

            try {
                if (imageUri != null) {

                    val imageUrl = uploadImageToStorage(imageUri)
                    if (imageUrl == null) {
                        _errorMessage.value = "Failed to upload image."
                        _isLoading.value = false
                        return@launch
                    }

                    finalEvent = event.copy(imageUrl = imageUrl)
                }


                val documentReference: DocumentReference = db.collection("Event").add(finalEvent).await()
                val eventId = documentReference.id


                db.collection("event_rspv").add(
                    EventRSPV(
                        eventId = eventId, // Use the correct eventId
                        coming_users = emptyList(),
                        maybe_users = emptyList(),
                        not_coming_users = emptyList()
                    )
                ).await()

                _isLoading.value = false
                _isSuccess.value = true

            } catch (e: Exception) {
                _isLoading.value = false
                _isSuccess.value = false
                _errorMessage.value = "Error creating event: ${e.message}"
            }
        }
    }

    private suspend fun uploadImageToStorage(imageUri: Uri): String? {
        return try {

            val fileName = "event_images/${UUID.randomUUID()}"
            val imageRef: StorageReference = storage.reference.child(fileName)


            imageRef.putFile(imageUri).await()


            imageRef.downloadUrl.await().toString()
        } catch (e: Exception) {

            _errorMessage.value = "Image upload failed: ${e.message}"
            null
        }
    }

    fun resetState() {
        _isSuccess.value = false
        _errorMessage.value = null
    }
}