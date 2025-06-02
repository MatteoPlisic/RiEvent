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
import com.example.rievent.MainActivity // Your main activity
import com.example.rievent.R // Your R file for resources like icon
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MyFirebaseMessagingService : FirebaseMessagingService() {

    private val TAG = "MyFirebaseMsgService"

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d(TAG, "From: ${remoteMessage.from}")

        // Check if message contains a data payload.
        remoteMessage.data.isNotEmpty().let {
            Log.d(TAG, "Message data payload: " + remoteMessage.data)
            // Handle data payload here (e.g., for deep linking when notification is tapped)
            val eventId = remoteMessage.data["eventId"]
            val type = remoteMessage.data["type"]

            // If you want to show a notification even if the app is in the foreground
            // you need to build it manually here.
        }

        // Check if message contains a notification payload.
        // This is typically handled by the system when the app is in the background or killed.
        // If the app is in the foreground, this callback fires, and you might want to show a custom in-app UI.
        remoteMessage.notification?.let {
            Log.d(TAG, "Message Notification Body: ${it.body}")
            showNotification(it.title, it.body, remoteMessage.data)
        }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "Refreshed token: $token")
        sendRegistrationToServer(token)
    }

    private fun sendRegistrationToServer(token: String?) {
        if (token == null) {
            Log.w(TAG, "FCM token is null, not sending to server.")
            return
        }
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId != null) {
            val db = FirebaseFirestore.getInstance()
            // Store token under /users/{userId}/fcmTokens/{tokenValue}
            // Using the token itself as the document ID for simplicity and uniqueness
            db.collection("users").document(userId)
                .collection("fcmTokens").document(token)
                .set(mapOf("token" to token, "timestamp" to com.google.firebase.Timestamp.now())) // Store token and timestamp
                .addOnSuccessListener { Log.d(TAG, "FCM token successfully stored for user $userId") }
                .addOnFailureListener { e -> Log.e(TAG, "Error storing FCM token for user $userId", e) }
        } else {
            Log.w(TAG, "User not logged in, cannot send FCM token to server.")
        }
    }

    private fun showNotification(title: String?, body: String?, data: Map<String, String>) {
        val channelId = getString(R.string.default_notification_channel_id) // Define this in strings.xml
        val eventId = data["eventId"]

        // Create an Intent for when the notification is tapped
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            // Add eventId to intent extras for deep linking
            if (eventId != null) {
                putExtra("eventId", eventId)
                putExtra("notificationType", data["type"]) // e.g., "NEW_EVENT_BY_FOLLOWED_USER"
            }
        }
        val pendingIntent: PendingIntent = PendingIntent.getActivity(
            this, 0 /* Request code */, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.mipmap.ic_launcher) // Replace with your notification icon
            .setContentTitle(title ?: "New Notification")
            .setContentText(body ?: "You have a new notification.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true) // Dismiss notification when tapped

        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = getString(R.string.default_notification_channel_name) // Define in strings.xml
            val descriptionText = getString(R.string.default_notification_channel_description) // Define in strings.xml
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(channelId, name, importance).apply {
                description = descriptionText
            }
            notificationManager.createNotificationChannel(channel)
        }

        // Check for POST_NOTIFICATIONS permission on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                NotificationManagerCompat.from(this).notify(System.currentTimeMillis().toInt(), builder.build())
            } else {
                Log.w(TAG, "POST_NOTIFICATIONS permission not granted. Notification not shown.")
                // You should request this permission from the user at an appropriate time in your app.
            }
        } else {
            NotificationManagerCompat.from(this).notify(System.currentTimeMillis().toInt(), builder.build()) // Use a unique ID for each notification
        }
    }
}