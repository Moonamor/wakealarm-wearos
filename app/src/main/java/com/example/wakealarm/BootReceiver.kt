package com.example.wakealarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Re-publish the listening activity after the watch restarts. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) ListenerService.start(context)
    }
}
