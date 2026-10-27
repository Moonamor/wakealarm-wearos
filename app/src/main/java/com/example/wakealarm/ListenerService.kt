package com.example.wakealarm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.wear.ongoing.OngoingActivity
import androidx.wear.ongoing.Status

/**
 * Always-on foreground service whose only job is to be visible:
 * it publishes an Ongoing Activity so the watch face / Now Bar shows "WakeAlarm يستمع".
 * (FCM delivery itself does not depend on this service.)
 */
class ListenerService : Service() {

    companion object {
        const val CHANNEL = "listen"
        const val NOTIF_ID = 1

        fun start(context: Context) {
            context.startForegroundService(Intent(context, ListenerService::class.java))
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, ListenerService::class.java))
        }

        /** Update the Now Bar text while ringing / idle. */
        fun setStatus(context: Context, text: String) {
            OngoingActivity.recoverOngoingActivity(context)
                ?.update(context, Status.forPart(Status.TextPart(text)))
        }
    }

    override fun onCreate() {
        super.onCreate()
        getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(CHANNEL, "الاستماع في الخلفية", NotificationManager.IMPORTANCE_LOW)
        )
        val open = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val builder = NotificationCompat.Builder(this, CHANNEL)
            .setSmallIcon(R.drawable.ic_bell)
            .setContentTitle("WakeAlarm")
            .setContentText("يستمع للتنبيهات")
            .setContentIntent(open)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setOngoing(true)
            .setSilent(true)

        OngoingActivity.Builder(this, NOTIF_ID, builder)
            .setStaticIcon(R.drawable.ic_bell)
            .setTouchIntent(open)
            .setStatus(Status.forPart(Status.TextPart("يستمع للتنبيهات")))
            .build()
            .apply(this)

        startForeground(NOTIF_ID, builder.build())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onDestroy() {
        NotificationManagerCompat.from(this).cancel(NOTIF_ID)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
