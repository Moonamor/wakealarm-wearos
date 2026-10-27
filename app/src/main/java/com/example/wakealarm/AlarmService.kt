package com.example.wakealarm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.os.IBinder
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.VibratorManager
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.app.NotificationCompat

/**
 * Foreground service that runs only while the alarm is ringing.
 * Started by [WakeMessagingService] when a push arrives, stopped by the on-screen button
 * or the notification action.
 */
class AlarmService : Service() {

    companion object {
        const val ACTION_RING = "com.example.wakealarm.RING"
        const val ACTION_STOP = "com.example.wakealarm.STOP"
        const val EXTRA_MESSAGE = "msg"
        const val DEFAULT_MESSAGE = "استيقظ!"

        private const val CHANNEL_RING = "ring_v2"
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
            NotificationChannel(CHANNEL_RING, "التنبيه", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "تنبيه الاستيقاظ القادم عن بُعد"
                setBypassDnd(true)
                enableVibration(true)
                setSound(null, AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).build())
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
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

    private fun buildNotification(msg: String): android.app.Notification {
        val openApp = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val stop = PendingIntent.getService(
            this, 1,
            Intent(this, AlarmService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        return NotificationCompat.Builder(this, CHANNEL_RING)
            .setSmallIcon(R.drawable.ic_bell)
            .setContentTitle(msg)
            .setContentText("اضغط لفتح التطبيق أو أوقف التنبيه")
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setFullScreenIntent(openApp, true)
            .setContentIntent(openApp)
            .addAction(R.drawable.ic_bell, "إلغاء التنبيه", stop)
            .setOngoing(true)
            .setAutoCancel(false)
            .setOnlyAlertOnce(false)
            .build()
    }

    private fun startRinging(msg: String) {
        message = msg
        startForeground(NOTIF_ID, buildNotification(msg))
        ListenerService.setStatus(this, "🔔 $msg")

        if (ringing) return
        ringing = true

        wakeLock = (getSystemService(POWER_SERVICE) as PowerManager)
            .newWakeLock(
                PowerManager.FULL_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
                "wakealarm:ring"
            )
            .apply { acquire(30 * 60 * 1000L) }

        val timings = longArrayOf(0, 900, 150, 900, 150, 1800, 400)
        val amps = intArrayOf(0, 255, 0, 255, 0, 255, 0)
        vibrator.vibrate(VibrationEffect.createWaveform(timings, amps, 0))

        // Best-effort direct launch; the full-screen intent above covers the locked/background case.
        runCatching {
            startActivity(
                Intent(this, MainActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            )
        }
    }

    private fun stopRinging() {
        if (!ringing) return
        ringing = false
        vibrator.cancel()
        wakeLock?.let { if (it.isHeld) it.release() }
        wakeLock = null
        stopForeground(STOP_FOREGROUND_REMOVE)
        ListenerService.setStatus(this, "يستمع للتنبيهات")
    }

    override fun onDestroy() {
        stopRinging()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
