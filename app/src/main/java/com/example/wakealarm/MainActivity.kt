package com.example.wakealarm

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.unit.LayoutDirection
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.Colors
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText
import androidx.wear.compose.material.Vignette
import androidx.wear.compose.material.VignettePosition

private val Red = Color(0xFFE53935)
private val RedDeep = Color(0xFF7F0000)
private val Teal = Color(0xFF26A69A)
private val Amber = Color(0xFFFFB300)
private val Ink = Color(0xFF0B0F14)
private val Muted = Color(0xFF9AA4AE)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LaunchedEffect(Unit) { Backend.refresh(this@MainActivity) }
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                MaterialTheme(
                    colors = Colors(primary = Red, onPrimary = Color.White, background = Ink,
                        onBackground = Color.White, surface = Color(0xFF161B22), onSurface = Color.White)
                ) {
                    WakeApp(onStop = { AlarmService.stop(this@MainActivity) })
                }
            }
        }
    }
}

@Composable
private fun WakeApp(onStop: () -> Unit) {
    val ringing = AlarmService.ringing
    AnimatedContent(
        targetState = ringing,
        transitionSpec = { fadeIn(tween(250)) togetherWith fadeOut(tween(250)) },
        label = "screen"
    ) { isRinging ->
        if (isRinging) RingingScreen(message = AlarmService.message, onStop = onStop)
        else IdleScreen()
    }
}

/* ───────────────────────── Ringing ───────────────────────── */

@Composable
private fun RingingScreen(message: String, onStop: () -> Unit) {
    val pulse = rememberInfiniteTransition(label = "pulse")
    val scale by pulse.animateFloat(
        initialValue = 0.92f, targetValue = 1.08f,
        animationSpec = infiniteRepeatable(tween(550, easing = LinearEasing), RepeatMode.Reverse),
        label = "scale"
    )
    val glow by pulse.animateFloat(
        initialValue = 0.35f, targetValue = 0.9f,
        animationSpec = infiniteRepeatable(tween(550, easing = LinearEasing), RepeatMode.Reverse),
        label = "glow"
    )

    Box(
        Modifier.fillMaxSize().background(
            Brush.radialGradient(listOf(RedDeep.copy(alpha = glow), Ink), radius = 420f)
        ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            Modifier.fillMaxSize().padding(horizontal = 22.dp, vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                Modifier.size(44.dp).scale(scale).background(Red.copy(alpha = 0.25f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.Notifications, contentDescription = null, tint = Red, modifier = Modifier.size(28.dp))
            }
            Spacer(Modifier.height(10.dp))
            Text(
                text = message,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 26.sp,
                textAlign = TextAlign.Center,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                color = Color.White
            )
            Spacer(Modifier.height(16.dp))
            Chip(
                onClick = onStop,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                colors = ChipDefaults.chipColors(backgroundColor = Red, contentColor = Color.White),
                icon = { Icon(Icons.Rounded.Close, contentDescription = null, modifier = Modifier.size(24.dp)) },
                label = {
                    Text("إلغاء التنبيه", fontSize = 17.sp, fontWeight = FontWeight.SemiBold,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            )
        }
    }
}

/* ───────────────────────── Idle ───────────────────────── */

@Composable
private fun IdleScreen() {
    Scaffold(
        timeText = { TimeText() },
        vignette = { Vignette(vignettePosition = VignettePosition.TopAndBottom) }
    ) {
        val (dotColor, title, hint) = when (Backend.status) {
            Backend.Status.READY -> Triple(Teal, "جاهز للاستقبال", "الساعة مسجّلة في الباك إند")
            Backend.Status.REGISTERING -> Triple(Amber, "جارٍ التسجيل…", "ربط الساعة بالباك إند")
            Backend.Status.NO_FIREBASE -> Triple(Red, "Firebase غير مضبوط", Backend.detail)
            Backend.Status.NOT_CONFIGURED -> Triple(Red, "الباك إند غير مضبوط", Backend.detail)
            Backend.Status.ERROR -> Triple(Red, "تعذر التسجيل", Backend.detail)
        }
        val blink = rememberInfiniteTransition(label = "blink")
        val a by blink.animateFloat(
            0.4f, 1f, infiniteRepeatable(tween(1200), RepeatMode.Reverse), label = "a"
        )

        Column(
            Modifier.fillMaxSize().background(Ink).padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                Modifier.size(54.dp).background(Color(0xFF161B22), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.Notifications, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
                Box(
                    Modifier.align(Alignment.BottomEnd).padding(2.dp).size(13.dp)
                        .alpha(if (Backend.status == Backend.Status.REGISTERING) a else 1f)
                        .background(dotColor, CircleShape)
                )
            }
            Spacer(Modifier.height(10.dp))
            Text("WakeAlarm", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Spacer(Modifier.height(4.dp))
            Text(title, fontSize = 14.sp, color = dotColor, textAlign = TextAlign.Center)
            Spacer(Modifier.height(2.dp))
            Text(hint, fontSize = 11.sp, color = Muted, textAlign = TextAlign.Center,
                maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}
