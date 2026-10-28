package com.example.wakealarm

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

/**
 * Receives FCM pushes sent by the Netlify function.
 * Data payload: { "msg": "<text shown on the watch>" }
 */
class WakeMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        if (!Listening.load(this)) return   // listening switched off by the user
        val data = remoteMessage.data.toMutableMap()
        if (data["msg"].isNullOrBlank()) remoteMessage.notification?.body?.let { data["msg"] = it }
        AlarmService.ring(this, Alert.from(data))
    }

    override fun onNewToken(token: String) {
        Backend.register(this, token)
    }
}
