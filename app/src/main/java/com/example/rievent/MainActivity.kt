package com.example.rievent // Your package

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class MainActivity : ComponentActivity() {

    // StateFlow for navigating to a specific EVENT
    private val _deepLinkNavigateToEventId = MutableStateFlow<String?>(null)
    val deepLinkNavigateToEventId: StateFlow<String?> = _deepLinkNavigateToEventId.asStateFlow()

    // NEW: StateFlow for navigating to a specific CHAT
    private val _deepLinkNavigateToChatId = MutableStateFlow<String?>(null)
    val deepLinkNavigateToChatId: StateFlow<String?> = _deepLinkNavigateToChatId.asStateFlow()

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                Log.d("Permission", "POST_NOTIFICATIONS permission granted.")
                getAndSendCurrentToken()
            } else {
                Log.d("Permission", "POST_NOTIFICATIONS permission denied by user.")
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        Log.d("MainActivityLifecycle", "onCreate called")

        askNotificationPermission()

        // MODIFIED: Update the call to RiEventAppUI with the new signature
        setContent {
            RiEventAppUI(
                deepLinkEventIdFlow = deepLinkNavigateToEventId,
                deepLinkChatIdFlow = deepLinkNavigateToChatId, // NEW: Pass the chat flow
                onEventDeepLinkHandled = {
                    Log.d("DeepLink", "Event deep link handled, resetting flow.")
                    _deepLinkNavigateToEventId.value = null
                },
                onChatDeepLinkHandled = { // NEW: Add the handler for chat deep links
                    Log.d("DeepLink", "Chat deep link handled, resetting flow.")
                    _deepLinkNavigateToChatId.value = null
                }
            )
        }

        // Handle the intent that started this activity instance
        Log.d("MainActivityLifecycle", "Calling handleIntentExtras from onCreate")
        handleIntentExtras(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        Log.d("MainActivityLifecycle", "onNewIntent called")

        setIntent(intent)
        handleIntentExtras(intent)
    }


    private fun handleIntentExtras(intent: Intent?) {
        Log.d("DeepLink", "handleIntentExtras called with intent: $intent")
        if (intent == null || intent.extras == null) {
            Log.d("DeepLink", "Intent or extras are null, no deep link data to process.")
            return
        }

        val extras = intent.extras
        val notificationType = extras?.getString("notificationType")
        Log.d("DeepLink", "Extracted notificationType: $notificationType")

        when (notificationType) {
            "NEW_MESSAGE" -> {
                val chatId = extras?.getString("chatId")
                if (chatId != null) {
                    Log.i("DeepLink", "Processing deep link for NEW_MESSAGE, chatId: $chatId")
                    _deepLinkNavigateToChatId.value = chatId
                } else {
                    Log.w("DeepLink", "NEW_MESSAGE notification received without a chatId.")
                }
            }
            // Add other event types here as needed
            "EVENT_DELETED", "EVENT_UPDATED", "NEW_EVENT_BY_FOLLOWED_USER" -> {
                val eventId = extras?.getString("eventId")
                if (eventId != null) {
                    Log.i("DeepLink", "Processing deep link for Event ($notificationType), eventId: $eventId")
                    _deepLinkNavigateToEventId.value = eventId
                } else {
                    Log.w("DeepLink", "Event notification received without an eventId.")
                }
            }
            else -> {
                Log.d("DeepLink", "Intent contained an unknown or unhandled notificationType: '$notificationType'.")
            }
        }
    }

    // --- The functions below are unchanged but necessary for context ---

    private fun askNotificationPermission() {
        Log.d("Permission", "askNotificationPermission called")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    Log.d("Permission", "POST_NOTIFICATIONS permission already granted.")
                    getAndSendCurrentToken()
                }
                shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) -> {
                    Log.d("Permission", "Showing rationale for POST_NOTIFICATIONS.")
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
                else -> {
                    Log.d("Permission", "Requesting POST_NOTIFICATIONS permission directly.")
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        } else {
            Log.d("Permission", "Pre-Tiramisu, notification permission is implicit.")
            getAndSendCurrentToken()
        }
    }

    private fun getAndSendCurrentToken() {
        Log.d("FCMToken", "getAndSendCurrentToken called")
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w("FCMToken", "Fetching FCM registration token failed", task.exception)
                return@addOnCompleteListener
            }
            val token = task.result
            Log.d("FCMToken", "Current FCM token from MainActivity: $token")
            sendTokenToServer(token)
        }
    }

    private fun sendTokenToServer(token: String?) {
        if (token == null) {
            Log.w("FCMToken", "Token is null, not sending to server.")
            return
        }
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId != null) {
            Log.d("FCMToken", "Sending token $token for user $userId to server.")
            val tokenData = mapOf("token" to token, "timestamp" to com.google.firebase.Timestamp.now())
            FirebaseFirestore.getInstance().collection("users").document(userId)
                .collection("fcmTokens").document(token)
                .set(tokenData)
                .addOnSuccessListener { Log.d("FCMToken", "Token successfully sent to server from MainActivity for user $userId") }
                .addOnFailureListener { e -> Log.e("FCMToken", "Error sending token to server from MainActivity for user $userId", e) }
        } else {
            Log.w("FCMToken", "User not logged in, token not sent to server.")
        }
    }
}