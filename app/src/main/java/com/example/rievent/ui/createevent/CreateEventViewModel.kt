package com.example.rievent.ui.createevent

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import Event
import com.example.rievent.models.EventRSPV
import com.example.rievent.models.RsvpUser
import com.google.firebase.firestore.DocumentReference
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class CreateEventViewModel : ViewModel() {


    private val db = FirebaseFirestore.getInstance()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _isSuccess = MutableStateFlow(false)
    val isSuccess = _isSuccess.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage = _errorMessage.asStateFlow()

    fun createEvent(event: Event) {
        viewModelScope.launch {
            _isLoading.value = true
            _isSuccess.value = false
            _errorMessage.value = null

            try {
                val documentReference: DocumentReference = db.collection("Event").add(event).await()

                val eventId = documentReference.id
                _isLoading.value = false
                _isSuccess.value = true



                db.collection("event_rspv").add(
                    EventRSPV(
                        eventId,
                        listOf<RsvpUser>(),
                        listOf<RsvpUser>(),
                        listOf<RsvpUser>()
                    )
                )

                _isLoading.value = false
                _isSuccess.value = true
            }catch (e: Exception) {

                _isLoading.value = false
                _isSuccess.value = false
                _errorMessage.value = e.message
            }
        }
    }

    fun resetState() {
        _isSuccess.value = false
        _errorMessage.value = null
    }
}
