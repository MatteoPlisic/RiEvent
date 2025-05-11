package com.example.rievent.ui.createevent

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import Event
import com.example.rievent.models.EventRSPV
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

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

            db.collection("Event")
                .add(event)
                .addOnSuccessListener {
                    _isLoading.value = false
                    _isSuccess.value = true
                }
                .addOnFailureListener { e ->
                    _isLoading.value = false
                    _errorMessage.value = e.localizedMessage
                }

            db.collection("event_rspv").add(EventRSPV(event.id.toString(), EmptyList<String>(), 0, 0))
        }
    }

    fun resetState() {
        _isSuccess.value = false
        _errorMessage.value = null
    }
}
