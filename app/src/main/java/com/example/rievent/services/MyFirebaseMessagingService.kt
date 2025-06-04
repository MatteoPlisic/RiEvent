package com.example.rievent.services // Or your preferred package

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.rievent.MainActivity // Your main activity
import com.example.rievent.R // Your R file for resources like icon (e.g., R.mipmap.ic_launcher, R.string.channel_name)
import com.google.firebase.Timestamp // For Firestore Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MyFirebaseMessagingService : FirebaseMessagingService() {

    private val TAG = "MyFirebaseMsgService"

    /**
     * Called when message is received.
     *
     * @param remoteMessage Object representing the message received from Firebase Cloud Messaging.
     */
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d(TAG, "From: ${remoteMessage.from}")

        // Extract notification and data payloads
        val notificationTitle = remoteMessage.notification?.title
        val notificationBody = remoteMessage.notification?.body
        val dataPayload = remoteMessage.data // This is a Map<String, String>

        Log.d(TAG, "Message data payload: $dataPayload")
        if (notificationTitle != null) {
            Log.d(TAG, "Message Notification Title: $notificationTitle")
        }
        if (notificationBody != null) {
            Log.d(TAG, "Message Notification Body: $notificationBody")
        }

        // Show a notification if we have a title and body from the notification payload.
        // Your Cloud Functions should always send a notification payload.
        if (notificationTitle != null && notificationBody != null) {
            showNotification(notificationTitle, notificationBody, dataPayload)
        } else if (dataPayload.isNotEmpty()) {
            // This block is for handling data-only messages if you choose to send them
            // and want to generate a notification manually from the data.
            // For your current Cloud Functions, this block might not be strictly necessary
            // as they send both 'notification' and 'data' payloads.
            Log.d(TAG, "Received data-only message. Constructing notification from data if needed.")
            // Example: if your cloud function ONLY sent data payload
            // val titleFromData = dataPayload["title"] ?: "App Update"
            // val bodyFromData = dataPayload["body"] ?: "Check the app for new information."
            // showNotification(titleFromData, bodyFromData, dataPayload)
        }
    }

    /**
     * Called if the FCM registration token is updated. This may occur if the previous token had
     * expired, app reinstallation, or other reasons.
     * Called when a new token for the default Firebase project is generated.
     *
     * @param token The new token.
     */
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "Refreshed token: $token")
        sendRegistrationToServer(token)
    }

    /**
     * Persist token to third-party servers.
     * Modify this method to associate the user's FCM registration token with any server-side account
     * maintained by your application.
     *
     * @param token The new token.
     */
    private fun sendRegistrationToServer(token: String?) {
        if (token == null) {
            Log.w(TAG, "FCM token is null, not sending to server.")
            return
        }
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId != null) {
            val db = FirebaseFirestore.getInstance()
            // Store token under /users/{userId}/fcmTokens/{tokenValue}
            // Using the token itself as the document ID for simplicity and uniqueness.
            // The map also includes the token and a timestamp.
            val tokenData = mapOf(
                "token" to token, // Storing the token as a field can be useful for queries if needed
                "timestamp" to Timestamp.now() // Good for tracking token freshness
            )
            db.collection("users").document(userId)
                .collection("fcmTokens").document(token) // Token as document ID
                .set(tokenData)
                .addOnSuccessListener { Log.d(TAG, "FCM token successfully stored/updated for user $userId") }
                .addOnFailureListener { e -> Log.e(TAG, "Error storing FCM token for user $userId", e) }
        } else {
            Log.w(TAG, "User not logged in, cannot send FCM token to server.")
        }
    }

    /**
     * Create and show a simple notification containing the received FCM message.
     *
     * @param title Notification title.
     * @param body  Notification message body.
     * @param data  Data payload from the FCM message.
     */
    private fun showNotification(title: String, body: String, data: Map<String, String>) {
        // Ensure you have these string resources defined in res/values/strings.xml
        val channelId = getString(R.string.default_notification_channel_id)
        val eventId = data["eventId"]
        val notificationType = data["type"]

        Log.i(TAG, "Attempting to show notification: type='$notificationType', eventId='$eventId', title='$title'")

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("notificationType", notificationType) // Pass the type for routing in MainActivity
            if (eventId != null) {
                putExtra("eventId", eventId) // Pass eventId for deep linking
            }
            // You can add more data from the `data` map to the intent if MainActivity needs it
        }

        val pendingIntentFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }

        val pendingIntent: PendingIntent = PendingIntent.getActivity(
            this,
            eventId?.hashCode() ?: 0, // Use eventId based request code for potentially updating, or 0
            intent,
            pendingIntentFlag
        )

        val builder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.mipmap.ic_launcher) // IMPORTANT: Replace with your app's actual notification icon
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH) // Higher priority for event notifications
            .setContentIntent(pendingIntent)
            .setAutoCancel(true) // Notification dismissed when user taps it
            .setDefaults(NotificationCompat.DEFAULT_ALL) // Use default sound, vibrate, lights

        // Optional: Customize notification based on type
        when (notificationType) {
            "EVENT_DELETED" -> {
                builder.setColor(ContextCompat.getColor(this, R.color.red)) // Example
                // builder.setOngoing(false) // Ensure it's not ongoing
            }
            "EVENT_UPDATED" -> {
                builder.setColor(ContextCompat.getColor(this, R.color.yellow)) // Example
            }
            "NEW_EVENT_BY_FOLLOWED_USER" -> {
                builder.setColor(ContextCompat.getColor(this, R.color.green)) // Example
            }
        }

        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        // Since Android Oreo (API 26), notification channels are required.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelName = getString(R.string.default_notification_channel_name)
            val channelDescription = getString(R.string.default_notification_channel_description)
            val importance = NotificationManager.IMPORTANCE_HIGH // Set importance for the channel
            val channel = NotificationChannel(channelId, channelName, importance).apply {
                description = channelDescription
                // Configure other channel properties here (e.g., lights, vibration)
                // enableLights(true)
                // lightColor = Color.RED
                // enableVibration(true)
                // vibrationPattern = longArrayOf(100, 200, 300, 400, 500)
            }
            notificationManager.createNotificationChannel(channel)
        }

        // Generate a unique ID for each notification, or a consistent one to update existing notifications.
        // Using eventId's hashcode allows updating notifications for the same event.
        // Fallback to current time for uniqueness if no eventId.
        val notificationId = eventId?.hashCode() ?: System.currentTimeMillis().toInt()

        // Check for POST_NOTIFICATIONS permission on Android 13 (API 33) and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                NotificationManagerCompat.from(this).notify(notificationId, builder.build())
            } else {
                Log.w(TAG, "POST_NOTIFICATIONS permission not granted. Cannot show notification.")
                // Note: You should have a mechanism in your app to request this permission from the user.
            }
        } else {
            NotificationManagerCompat.from(this).notify(notificationId, builder.build())
        }
    }
}