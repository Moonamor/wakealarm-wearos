package com.example.wakealarm

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import kotlin.math.PI
import kotlin.math.sin

/**
 * Synthesised siren played on the ALARM stream at maximum volume.
 * Pure software — it never touches the system SOS / emergency features.
 */
class Siren(private val context: Context) {

    private val audio = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var track: AudioTrack? = null
    private var previousVolume = -1

    fun start() {
        if (track != null) return
        previousVolume = audio.getStreamVolume(AudioManager.STREAM_ALARM)
        runCatching {
            audio.setStreamVolume(AudioManager.STREAM_ALARM, audio.getStreamMaxVolume(AudioManager.STREAM_ALARM), 0)
        }

        val sampleRate = 44_100
        val pcm = buildSweep(sampleRate)
        val t = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(pcm.size * 2)
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()
        t.write(pcm, 0, pcm.size)
        t.setLoopPoints(0, pcm.size, -1)
        t.setVolume(1f)
        t.play()
        track = t
    }

    fun stop() {
        track?.let { runCatching { it.stop() }; it.release() }
        track = null
        if (previousVolume >= 0) {
            runCatching { audio.setStreamVolume(AudioManager.STREAM_ALARM, previousVolume, 0) }
            previousVolume = -1
        }
    }

    /** One siren cycle: 600 Hz → 1500 Hz → 600 Hz over ~1.2 s. */
    private fun buildSweep(sampleRate: Int): ShortArray {
        val seconds = 1.2
        val n = (sampleRate * seconds).toInt()
        val out = ShortArray(n)
        var phase = 0.0
        for (i in 0 until n) {
            val p = i.toDouble() / n                      // 0..1
            val tri = if (p < 0.5) p * 2 else (1 - p) * 2 // 0→1→0
            val freq = 600 + tri * 900
            phase += 2 * PI * freq / sampleRate
            // slight square-ish harmonics so it cuts through
            val s = sin(phase) * 0.8 + sin(3 * phase) * 0.2
            out[i] = (s.coerceIn(-1.0, 1.0) * Short.MAX_VALUE).toInt().toShort()
        }
        return out
    }
}
