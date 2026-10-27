package com.example.wakealarm

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

/** Registers this watch's FCM token with the Netlify backend so /alert knows where to push. */
object Backend {

    enum class Status { NOT_CONFIGURED, NO_FIREBASE, REGISTERING, READY, ERROR }

    var status by mutableStateOf(Status.REGISTERING)
        private set
    var detail by mutableStateOf("")
        private set

    private const val PREFS = "wake"
    private const val KEY_LAST_TOKEN = "last_token"

    fun refresh(context: Context) {
        if (FirebaseApp.getApps(context).isEmpty()) {
            status = Status.NO_FIREBASE; detail = "google-services.json مفقود"; return
        }
        if (BuildConfig.BACKEND_URL.isBlank() || BuildConfig.WAKE_KEY.isBlank()) {
            status = Status.NOT_CONFIGURED; detail = "لم يُضبط رابط الباك إند"; return
        }
        status = Status.REGISTERING; detail = ""
        FirebaseMessaging.getInstance().token
            .addOnSuccessListener { register(context, it) }
            .addOnFailureListener { status = Status.ERROR; detail = it.message ?: "تعذر الحصول على التوكن" }
    }

    fun register(context: Context, token: String) {
        if (BuildConfig.BACKEND_URL.isBlank() || BuildConfig.WAKE_KEY.isBlank()) {
            status = Status.NOT_CONFIGURED; return
        }
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        thread(name = "wake-register") {
            try {
                val url = URL(BuildConfig.BACKEND_URL.trimEnd('/') + "/register")
                val conn = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    connectTimeout = 10_000; readTimeout = 10_000
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json")
                    setRequestProperty("x-key", BuildConfig.WAKE_KEY)
                }
                conn.outputStream.use { it.write(JSONObject().put("token", token).toString().toByteArray()) }
                val code = conn.responseCode
                conn.disconnect()
                if (code in 200..299) {
                    prefs.edit().putString(KEY_LAST_TOKEN, token).apply()
                    status = Status.READY; detail = ""
                } else {
                    status = Status.ERROR; detail = "HTTP $code"
                }
            } catch (e: Exception) {
                status = Status.ERROR; detail = e.javaClass.simpleName
            }
        }
    }
}
