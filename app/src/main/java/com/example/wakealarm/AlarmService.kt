package com.example.wakealarm

import android.app.*
import android.content.Intent
import android.net.wifi.WifiManager
import android.os.*
import fi.iki.elonen.NanoHTTPD

class AlarmService : Service() {
    companion object {
        const val PORT = 8080
        const val SECRET = "WAKE"
        const val ACTION_STOP = "com.example.wakealarm.STOP"
        @Volatile var ringing = false
        var onStateChange: ((Boolean) -> Unit)? = null
    }

    private var server: NanoHTTPD? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var wifiLock: WifiManager.WifiLock? = null
    private val vibrator by lazy {
        (getSystemService(VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
    }

    override fun onCreate() {
        super.onCreate()
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel("listen", "Listening", NotificationManager.IMPORTANCE_LOW))
        nm.createNotificationChannel(
            NotificationChannel("ring", "Alarm", NotificationManager.IMPORTANCE_HIGH))

        startForeground(1, Notification.Builder(this, "listen")
            .setContentTitle("WakeAlarm يستمع على المنفذ $PORT")
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setOngoing(true).build())

        wifiLock = (applicationContext.getSystemService(WIFI_SERVICE) as WifiManager)
            .createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, "wakealarm:wifi")
            .apply { acquire() }

        server = object : NanoHTTPD(PORT) {
            override fun serve(session: IHTTPSession): Response {
                if (session.method == Method.POST) session.parseBody(HashMap())
                val value = session.parameters["value"]?.firstOrNull()
                return if (session.uri == "/alert" && value == SECRET) {
                    startRinging()
                    newFixedLengthResponse("RINGING")
                } else newFixedLengthResponse(Response.Status.FORBIDDEN, MIME_PLAINTEXT, "NO")
            }
        }
        server?.start(NanoHTTPD.SOCKET_READ_TIMEOUT, false)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) stopRinging()
        return START_STICKY
    }

    private fun startRinging() {
        if (ringing) return
        ringing = true
        wakeLock = (getSystemService(POWER_SERVICE) as PowerManager)
            .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "wakealarm:ring")
            .apply { acquire(30 * 60 * 1000L) }

        val timings = longArrayOf(0, 900, 150, 900, 150, 1800, 400)
        val amps = intArrayOf(0, 255, 0, 255, 0, 255, 0)
        vibrator.vibrate(VibrationEffect.createWaveform(timings, amps, 0))

        val pi = PendingIntent.getActivity(this, 0,
            Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_IMMUTABLE)
        getSystemService(NotificationManager::class.java).notify(2,
            Notification.Builder(this, "ring")
                .setContentTitle("استيقظ!")
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setCategory(Notification.CATEGORY_ALARM)
                .setFullScreenIntent(pi, true)
                .setOngoing(true).build())
        onStateChange?.invoke(true)
    }

    private fun stopRinging() {
        if (!ringing) return
        ringing = false
        vibrator.cancel()
        wakeLock?.let { if (it.isHeld) it.release() }
        getSystemService(NotificationManager::class.java).cancel(2)
        onStateChange?.invoke(false)
    }

    override fun onDestroy() {
        server?.stop()
        stopRinging()
        wifiLock?.let { if (it.isHeld) it.release() }
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
