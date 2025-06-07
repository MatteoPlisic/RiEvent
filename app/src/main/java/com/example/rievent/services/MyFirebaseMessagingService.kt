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
import com.example.rievent.R // Your R file for resources like icon and strings
import com.google.firebase.Timestamp // For Firestore Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MyFirebaseMessagingService : FirebaseMessagingService() {

    private val TAG = "MyFirebaseMsgService"
    // NEW: Define a separate channel ID for messages for better user control
    private val MESSAGE_CHANNEL_ID = "messages_channel"

    /**
     * Called when message is received.
     */
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d(TAG, "From: ${remoteMessage.from}")

        val notificationTitle = remoteMessage.notification?.title
        val notificationBody = remoteMessage.notification?.body
        val dataPayload = remoteMessage.data

        Log.d(TAG, "Message data payload: $dataPayload")
        if (notificationTitle != null && notificationBody != null) {
            Log.d(TAG, "Message Notification: Title='$notificationTitle', Body='$notificationBody'")
            showNotification(notificationTitle, notificationBody, dataPayload)
        } else {
            Log.d(TAG, "Received message without a notification payload. Data only.")
            // You can optionally handle data-only messages here if needed.
        }
    }

    /**
     * Called if the FCM registration token is updated.
     */
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "Refreshed token: $token")
        sendRegistrationToServer(token)
    }

    /**
     * Persist token to Firestore.
     */
    private fun sendRegistrationToServer(token: String?) {
        if (token == null) {
            Log.w(TAG, "FCM token is null, not sending to server.")
            return
        }
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId != null) {
            val db = FirebaseFirestore.getInstance()
            val tokenData = mapOf(
                "token" to token,
                "timestamp" to Timestamp.now()
            )
            db.collection("users").document(userId)
                .collection("fcmTokens").document(token)
                .set(tokenData)
                .addOnSuccessListener { Log.d(TAG, "FCM token successfully stored/updated for user $userId") }
                .addOnFailureListener { e -> Log.e(TAG, "Error storing FCM token for user $userId", e) }
        } else {
            Log.w(TAG, "User not logged in, cannot send FCM token to server.")
        }
    }

    /**
     * Create and show a notification containing the received FCM message.
     *
     * @param title Notification title.
     * @param body  Notification message body.
     * @param data  Data payload from the FCM message.
     */
    private fun showNotification(title: String, body: String, data: Map<String, String>) {
        val notificationType = data["type"]
        val senderId = data["senderId"] // Get senderId from the data payload
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
        Log.i(TAG, "Attempting to show notification: type='$notificationType', title='$title'")

        if (notificationType == "NEW_MESSAGE" && senderId != null && senderId == currentUserId) {
            Log.d(TAG, "Notification suppressed: Current user is the sender.")
            return
        }
        // MODIFIED: Choose channel ID based on notification type
        val channelId = if (notificationType == "NEW_MESSAGE") {
            MESSAGE_CHANNEL_ID
        } else {
            getString(R.string.default_notification_channel_id)
        }

        // MODIFIED: Extract eventId or chatId from data payload
        val eventId = data["eventId"]
        val chatId = data["chatId"]

        // Create an Intent that will open your app. This is the heart of deep-linking.
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("notificationType", notificationType)
            // MODIFIED: Add relevant ID to the intent
            if (chatId != null) {
                putExtra("chatId", chatId)
            }
            if (eventId != null) {
                putExtra("eventId", eventId)
            }
        }

        val pendingIntentFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }

        // MODIFIED: Use a unique request code for the pending intent.
        // This ensures intents for different chats/events are treated separately.
        val requestCode = (chatId?.hashCode() ?: eventId?.hashCode()) ?: System.currentTimeMillis().toInt()

        val pendingIntent: PendingIntent = PendingIntent.getActivity(
            this, requestCode, intent, pendingIntentFlag
        )

        val builder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.mipmap.ic_launcher) // IMPORTANT: Replace with your actual icon
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body)) // Show full message text

        // Optional: Customize notification based on type
        when (notificationType) {
            "EVENT_DELETED" -> builder.setColor(ContextCompat.getColor(this, R.color.red))
            "EVENT_UPDATED" -> builder.setColor(ContextCompat.getColor(this, R.color.yellow))
            "NEW_EVENT_BY_FOLLOWED_USER" -> builder.setColor(ContextCompat.getColor(this, R.color.green))
            // NEW: Handle the new message type
            "NEW_MESSAGE" -> {
                builder.setColor(ContextCompat.getColor(this, R.color.blue)) // Example color for messages
                // You could also set a different small icon for messages
                // builder.setSmallIcon(R.drawable.ic_message)
                builder.setGroup(chatId) // NEW: Group notifications by chat ID
            }
        }

        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannels(notificationManager) // Create all channels

        // MODIFIED: Use a consistent ID for notifications from the same chat/event
        // so they can update each other instead of stacking up.
        val notificationId = (chatId?.hashCode() ?: eventId?.hashCode()) ?: System.currentTimeMillis().toInt()

        // Check for POST_NOTIFICATIONS permission on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                NotificationManagerCompat.from(this).notify(notificationId, builder.build())
            } else {
                Log.w(TAG, "POST_NOTIFICATIONS permission not granted. Cannot show notification.")
            }
        } else {
            NotificationManagerCompat.from(this).notify(notificationId, builder.build())
        }
    }

    // NEW: Helper function to create all necessary notification channels
    private fun createNotificationChannels(notificationManager: NotificationManager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Channel for Default/Event Notifications
            val defaultChannelName = getString(R.string.default_notification_channel_name)
            val defaultChannelDesc = getString(R.string.default_notification_channel_description)
            val defaultChannel = NotificationChannel(
                getString(R.string.default_notification_channel_id),
                defaultChannelName,
                NotificationManager.IMPORTANCE_HIGH
            ).apply { description = defaultChannelDesc }

            // Channel for Message Notifications
            // Make sure you have these strings in your res/values/strings.xml
            val messageChannelName = getString(R.string.message_notification_channel_name)
            val messageChannelDesc = getString(R.string.message_notification_channel_description)
            val messageChannel = NotificationChannel(
                MESSAGE_CHANNEL_ID,
                messageChannelName,
                NotificationManager.IMPORTANCE_HIGH
            ).apply { description = messageChannelDesc }

            notificationManager.createNotificationChannel(defaultChannel)
            notificationManager.createNotificationChannel(messageChannel)
        }
    }
}