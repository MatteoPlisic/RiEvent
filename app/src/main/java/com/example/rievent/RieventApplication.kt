package com.example.rievent

import android.app.Application
import android.content.pm.PackageManager
import android.util.Log
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.net.PlacesClient

class RieventApplication : Application() {


    lateinit var placesClient: PlacesClient
        private set

    override fun onCreate() {
        super.onCreate()

        Log.d("RieventApplication", "onCreate called")


        val apiKey = try {
            val appInfo = packageManager.getApplicationInfo(packageName, PackageManager.GET_META_DATA)
            appInfo.metaData.getString("com.google.android.geo.API_KEY")
        } catch (e: PackageManager.NameNotFoundException) {
            Log.e("RieventApplication", "Failed to load meta-data, NameNotFound: " + e.message)
            null
        } catch (e: NullPointerException) {
            Log.e("RieventApplication", "Failed to load meta-data, NullPointer: " + e.message)
            null
        }

        if (apiKey.isNullOrBlank()) {
            Log.e("RieventApplication", "Places API key not found, empty, or blank in manifest. Places SDK will NOT be initialized.")

        } else {
            if (!Places.isInitialized()) {
                try {
                    Places.initialize(applicationContext, apiKey)
                    placesClient = Places.createClient(this)
                    Log.i("RieventApplication", "Places SDK Initialized successfully.")
                } catch (e: Exception) {
                    Log.e("RieventApplication", "Error initializing Places SDK: ${e.message}", e)

                }
            } else {

                placesClient = Places.createClient(this)
                Log.i("RieventApplication", "Places SDK was already initialized. Got client instance.")
            }
        }
    }
}