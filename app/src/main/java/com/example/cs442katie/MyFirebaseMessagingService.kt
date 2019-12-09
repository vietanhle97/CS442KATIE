package com.example.cs442katie

import android.app.*
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MyFirebaseMessagingService : FirebaseMessagingService() {

    private val TAG = "FirebaseService"

    /**
     * this method will be triggered every time there is new FCM Message.
     */

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "Refreshed token: $token")

        // If you want to send messages to this application instance or
        // manage this apps subscriptions on the server side, send the
        // Instance ID token to your app server.
        // sendRegistrationToServer(token)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        createNotificationChannel()
        Log.e(TAG, "From: " + remoteMessage.from)

        if(remoteMessage.data != null) {
            Log.e(TAG, "Notification Message Body: ${remoteMessage.data}")
            sendNotification(remoteMessage.data)
        }
    }

    private fun sendNotification(data: Map<String, String>) {
        Log.e(TAG, data.get("courseId"))
        val intent = Intent(this, CourseActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("studentId", data.get("studentId"))
            putExtra("courseId", data.get("courseId"))
            putExtra("courseName", data.get("title"))
            putExtra("isAdmin", data.get("isAdmin"))
        }

        var pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_ONE_SHOT)
        val notificationSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        var notificationBuilder = NotificationCompat.Builder(applicationContext, resources.getString(R.string.notification_channel_id))
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(data.get("title"))
            .setContentText(data.get("message"))
            .setAutoCancel(true)
            .setSound(notificationSound)
            .setContentIntent(pendingIntent)

        var notificationManager: NotificationManager = this.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(0, notificationBuilder.build())
    }

    private fun createNotificationChannel() {
        // Application onCreate
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(Context.NOTIFICATION_SERVICE)
                    as NotificationManager
            val channelId = getString(R.string.notification_channel_id)
            if(manager.getNotificationChannel(channelId)==null) {
                val channel = NotificationChannel(channelId,
                    "name dit me may",
                    NotificationManager.IMPORTANCE_DEFAULT)
                channel.description = "description dmm"
                manager.createNotificationChannel(channel)
            }
        }
    }
}