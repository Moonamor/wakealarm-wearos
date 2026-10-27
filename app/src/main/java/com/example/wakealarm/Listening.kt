package com.example.wakealarm

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.wear.tiles.TileService

/** Single source of truth for "is listening on?" — shared by the app, the tile and the FCM receiver. */
object Listening {
    private const val PREFS = "wake"
    private const val KEY = "listening"

    var enabled by mutableStateOf(true)
        private set

    fun load(context: Context): Boolean {
        enabled = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY, true)
        return enabled
    }

    fun set(context: Context, on: Boolean) {
        enabled = on
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(KEY, on).apply()
        if (on) ListenerService.start(context) else {
            AlarmService.stop(context)
            ListenerService.stop(context)
        }
        runCatching { TileService.getUpdater(context).requestUpdate(WakeTile::class.java) }
    }

    fun toggle(context: Context) = set(context, !load(context))

    /** The request URL shown as QR / text in the app. */
    fun alertUrl(): String =
        BuildConfig.BACKEND_URL.trimEnd('/') + "/alert?key=" + BuildConfig.WAKE_KEY + "&msg="
}
