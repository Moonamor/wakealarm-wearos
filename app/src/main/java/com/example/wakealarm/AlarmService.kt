package com.example.wakealarm

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.VibratorManager
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Foreground service that runs only while the alarm is ringing.
 * Started by [WakeMessagingService] when a push arrives, stopped by the on-screen button.
 */
class AlarmService : Service() {

    companion object {
        const val ACTION_RING = "com.example.wakealarm.RING"
        const val ACTION_STOP = "com.example.wakealarm.STOP"
        const val EXTRA_MESSAGE = "msg"
        const val DEFAULT_MESSAGE = "استيقظ!"

        private const val CHANNEL_RING = "ring"
        private const val NOTIF_ID = 2

        /** Observable state for the Compose UI. */
        var ringing by mutableStateOf(false)
            private set
        var message by mutableStateOf(DEFAULT_MESSAGE)
            private set

        fun ring(context: Context, msg: String?) {
            val i = Intent(context, AlarmService::class.java)
                .setAction(ACTION_RING)
                .putExtra(EXTRA_MESSAGE, msg?.takeIf { it.isNotBlank() } ?: DEFAULT_MESSAGE)
            context.startForegroundService(i)
        }

        fun stop(context: Context) {
            context.startService(Intent(context, AlarmService::class.java).setAction(ACTION_STOP))
        }
    }

    private var wakeLock: PowerManager.WakeLock? = null
    private val vibrator by lazy {
        (getSystemService(VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
    }

    override fun onCreate() {
        super.onCreate()
        getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(CHANNEL_RING, "Alarm", NotificationManager.IMPORTANCE_HIGH).apply {
                setBypassDnd(true)
            }
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_RING -> startRinging(intent.getStringExtra(EXTRA_MESSAGE) ?: DEFAULT_MESSAGE)
            ACTION_STOP -> { stopRinging(); stopSelf() }
            else -> stopSelf()
        }
        return START_NOT_STICKY
    }

    private fun startRinging(msg: String) {
        message = msg

        val fullScreen = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val notification = Notification.Builder(this, CHANNEL_RING)
            .setContentTitle(msg)
            .setContentText("اضغط لإيقاف التنبيه")
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setCategory(Notification.CATEGORY_ALARM)
            .setFullScreenIntent(fullScreen, true)
            .setContentIntent(fullScreen)
            .setOngoing(true)
            .build()
        startForeground(NOTIF_ID, notification)

        if (ringing) return
        ringing = true

        wakeLock = (getSystemService(POWER_SERVICE) as PowerManager)
            .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "wakealarm:ring")
            .apply { acquire(30 * 60 * 1000L) }

        val timings = longArrayOf(0, 900, 150, 900, 150, 1800, 400)
        val amps = intArrayOf(0, 255, 0, 255, 0, 255, 0)
        vibrator.vibrate(VibrationEffect.createWaveform(timings, amps, 0))

        // Bring the alarm screen up immediately as well (full-screen intent handles the locked case).
        startActivity(
            Intent(this, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        )
    }

    private fun stopRinging() {
        if (!ringing) return
        ringing = false
        vibrator.cancel()
        wakeLock?.let { if (it.isHeld) it.release() }
        wakeLock = null
        stopForeground(STOP_FOREGROUND_REMOVE)
    }

    override fun onDestroy() {
        stopRinging()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
