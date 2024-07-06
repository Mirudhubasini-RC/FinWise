package com.finwise.finwise_app

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MyFirebaseMessagingService : FirebaseMessagingService() {
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        // Handle the received notification here
        // You can extract the notification title and body from remoteMessage
        val title = remoteMessage.notification?.title
        val body = remoteMessage.notification?.body
        // Display the notification to the user
        // You can use NotificationCompat.Builder to build and display the notification
    }
}