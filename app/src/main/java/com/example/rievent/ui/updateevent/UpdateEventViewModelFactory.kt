package com.example.rievent.ui.updateevent

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider


class UpdateEventViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(UpdateEventViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return UpdateEventViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}