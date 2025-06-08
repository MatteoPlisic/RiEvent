package com.example.rievent

import android.app.Application
import com.google.firebase.FirebaseApp

class RiEventApp : Application() {
    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
    }
}
