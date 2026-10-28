package com.example.wakealarm

import android.os.Bundle

enum class Level { SOFT, NORMAL, URGENT;
    companion object {
        fun parse(s: String?) = when (s?.lowercase()) {
            "soft", "low" -> SOFT
            "urgent", "high", "siren" -> URGENT
            else -> NORMAL
        }
    }
}

/** Everything one push can carry. */
data class Alert(
    val message: String = AlarmService.DEFAULT_MESSAGE,
    val level: Level = Level.NORMAL,
    val siren: Boolean = false,
    val link: String? = null,
    val phone: String? = null,
) {
    companion object {
        private val URL_RX = Regex("""https?://\S+""")
        private val PHONE_RX = Regex("""(\+?\d[\d\s-]{7,}\d)""")

        /** Build from the FCM data map; falls back to detecting a link / phone inside the text. */
        fun from(data: Map<String, String>): Alert {
            val msg = data["msg"]?.takeIf { it.isNotBlank() } ?: AlarmService.DEFAULT_MESSAGE
            val level = Level.parse(data["level"])
            val sirenFlag = data["siren"]?.let { it == "1" || it.equals("true", true) }
            return Alert(
                message = msg,
                level = level,
                siren = sirenFlag ?: (level == Level.URGENT),
                link = data["link"]?.takeIf { it.isNotBlank() } ?: URL_RX.find(msg)?.value,
                phone = data["phone"]?.takeIf { it.isNotBlank() }
                    ?: PHONE_RX.find(msg)?.value?.replace(Regex("[\\s-]"), ""),
            )
        }

        fun fromBundle(b: Bundle?): Alert = Alert(
            message = b?.getString("msg") ?: AlarmService.DEFAULT_MESSAGE,
            level = Level.parse(b?.getString("level")),
            siren = b?.getBoolean("siren") ?: false,
            link = b?.getString("link"),
            phone = b?.getString("phone"),
        )
    }

    fun toBundle() = Bundle().apply {
        putString("msg", message); putString("level", level.name)
        putBoolean("siren", siren); putString("link", link); putString("phone", phone)
    }
}
