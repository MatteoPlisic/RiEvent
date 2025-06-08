package com.example.rievent.services
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
import com.example.rievent.MainActivity
import com.example.rievent.R
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MyFirebaseMessagingService : FirebaseMessagingService() {

    private val TAG = "MyFirebaseMsgService"

    private val MESSAGE_CHANNEL_ID = "messages_channel"


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


    private fun showNotification(title: String, body: String, data: Map<String, String>) {
        val notificationType = data["type"]
        val senderId = data["senderId"]
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
        Log.i(TAG, "Attempting to show notification: type='$notificationType', title='$title'")

        if (notificationType == "NEW_MESSAGE" && senderId != null && senderId == currentUserId) {
            Log.d(TAG, "Notification suppressed: Current user is the sender.")
            return
        }

        val channelId = if (notificationType == "NEW_MESSAGE") {
            MESSAGE_CHANNEL_ID
        } else {
            getString(R.string.default_notification_channel_id)
        }


        val eventId = data["eventId"]
        val chatId = data["chatId"]


        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("notificationType", notificationType)

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


        val requestCode = (chatId?.hashCode() ?: eventId?.hashCode()) ?: System.currentTimeMillis().toInt()

        val pendingIntent: PendingIntent = PendingIntent.getActivity(
            this, requestCode, intent, pendingIntentFlag
        )

        val builder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))


        when (notificationType) {
            "EVENT_DELETED" -> builder.setColor(ContextCompat.getColor(this, R.color.red))
            "EVENT_UPDATED" -> builder.setColor(ContextCompat.getColor(this, R.color.yellow))
            "NEW_EVENT_BY_FOLLOWED_USER" -> builder.setColor(ContextCompat.getColor(this, R.color.green))

            "NEW_MESSAGE" -> {
                builder.setColor(ContextCompat.getColor(this, R.color.blue))

                builder.setGroup(chatId)
            }
        }

        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannels(notificationManager)


        val notificationId = (chatId?.hashCode() ?: eventId?.hashCode()) ?: System.currentTimeMillis().toInt()


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


    private fun createNotificationChannels(notificationManager: NotificationManager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            val defaultChannelName = getString(R.string.default_notification_channel_name)
            val defaultChannelDesc = getString(R.string.default_notification_channel_description)
            val defaultChannel = NotificationChannel(
                getString(R.string.default_notification_channel_id),
                defaultChannelName,
                NotificationManager.IMPORTANCE_HIGH
            ).apply { description = defaultChannelDesc }


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