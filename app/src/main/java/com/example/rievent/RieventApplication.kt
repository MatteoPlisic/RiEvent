package com.example.rievent // Make sure this matches your app's package name

import android.app.Application
import android.content.pm.PackageManager
import android.util.Log
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.net.PlacesClient

class RieventApplication : Application() {

    // You can make this accessible globally if needed, but be cautious with global state.
    // For now, it's initialized here and the Places SDK itself is a singleton.
    lateinit var placesClient: PlacesClient
        private set // Restrict writing to this property from outside the class

    override fun onCreate() {
        super.onCreate()

        Log.d("RieventApplication", "onCreate called") // For debugging

        // Retrieve API key from manifest's meta-data
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

        if (apiKey.isNullOrBlank()) { // Check for blank too
            Log.e("RieventApplication", "Places API key not found, empty, or blank in manifest. Places SDK will NOT be initialized.")
            // You might want to throw an exception or disable features if the API key is critical
        } else {
            if (!Places.isInitialized()) {
                try {
                    Places.initialize(applicationContext, apiKey)
                    placesClient = Places.createClient(this)
                    Log.i("RieventApplication", "Places SDK Initialized successfully.")
                } catch (e: Exception) {
                    Log.e("RieventApplication", "Error initializing Places SDK: ${e.message}", e)
                    // Handle initialization error, e.g., API key invalid, network issue during init
                }
            } else {
                // If already initialized (e.g., process was recreated), just get the client
                placesClient = Places.createClient(this)
                Log.i("RieventApplication", "Places SDK was already initialized. Got client instance.")
            }
        }
    }
}