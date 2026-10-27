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
        val msg = remoteMessage.data["msg"]
            ?: remoteMessage.notification?.body
            ?: AlarmService.DEFAULT_MESSAGE
        AlarmService.ring(this, msg)
    }

    override fun onNewToken(token: String) {
        Backend.register(this, token)
    }
}
