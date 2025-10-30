package com.example.smarthome

import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.util.Log
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import java.text.SimpleDateFormat
import java.util.*

class AppFirebaseMessagingService : FirebaseMessagingService() {

    companion object {
        private const val TAG = "AppFirebaseMsgService"
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "New FCM token: $token")
        // Save token to Realtime Database under IoTSystem/FCMTokens
        saveTokenToDatabase(token)
    }

    private fun saveTokenToDatabase(token: String) {
        try {
            val db = FirebaseDatabase.getInstance().reference
            val tokensRef = db.child("IoTSystem").child("FCMTokens")
            val key = tokensRef.push().key ?: UUID.randomUUID().toString()
            val now = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.getDefault()).format(Date())
            val entry = mapOf(
                "token" to token,
                "deviceName" to (Build.DEVICE ?: "Android Device"),
                "deviceModel" to (Build.MODEL ?: "Unknown"),
                "lastUpdated" to now,
                "active" to true,
                "createdAt" to now
            )
            tokensRef.child(key).setValue(entry)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save token to database", e)
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        try {
            // Prefer notification payload title/body, fallback to data payload
            val title = remoteMessage.notification?.title
                ?: remoteMessage.data["title"]
                ?: getString(R.string.app_name)
            val body = remoteMessage.notification?.body
                ?: remoteMessage.data["body"]
                ?: remoteMessage.data["message"]
                ?: ""

            // Simpan notifikasi ke database lokal
            val notificationDb = NotificationDatabase(this)
            notificationDb.saveNotification(title, body)

            // Build intent to open NotificationActivity when user taps the notification
            val intent = Intent(this, NotificationActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)

            // minSdk >= 24 so FLAG_IMMUTABLE is available; use both flags
            val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            val pendingIntent = PendingIntent.getActivity(this, 0, intent, flags)

            // Ensure notification channel exists
            com.example.smarthome.NotificationHelper.createNotificationChannel(this)

            // Use current time as id (within int range)
            val id = (System.currentTimeMillis() % Int.MAX_VALUE).toInt()

            // Show persistent, high-priority notification with sound
            com.example.smarthome.NotificationHelper.showNotification(
                context = this,
                notificationId = id,
                title = title,
                body = body,
                pendingIntent = pendingIntent,
                ongoing = true, // keep it visible until app or backend clears
                playSound = true
            )

        } catch (e: Exception) {
            Log.e(TAG, "Error handling incoming message", e)
        }
    }
}
