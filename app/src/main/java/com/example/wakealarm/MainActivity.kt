package com.example.wakealarm

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
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
import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.QrCode
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.wear.compose.material.ChipDefaults.chipColors
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
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
private val Yellow = Color(0xFFFFD600)

class MainActivity : ComponentActivity() {

    private val askNotifications =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            if (Listening.load(this)) ListenerService.start(this)
        }

    fun askNotificationsPublic() = askNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setShowWhenLocked(true)
        setTurnScreenOn(true)
        ensureNotificationsThenListen()
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

private fun MainActivity.ensureNotificationsThenListen() {
    val needsAsk = Build.VERSION.SDK_INT >= 33 &&
        checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
    if (needsAsk) askNotificationsPublic() else if (Listening.load(this)) ListenerService.start(this)
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
        else HomeScreen()
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

/* ───────────────────────── Home ───────────────────────── */

@Composable
private fun HomeScreen() {
    var showQr by remember { mutableStateOf(false) }
    if (showQr) QrScreen(onBack = { showQr = false }) else IdleScreen(onShowQr = { showQr = true })
}

@Composable
private fun IdleScreen(onShowQr: () -> Unit) {
    val ctx = LocalContext.current
    val on = Listening.enabled
    LaunchedEffect(Unit) { Listening.load(ctx) }

    Scaffold(
        timeText = { TimeText() },
        vignette = { Vignette(vignettePosition = VignettePosition.TopAndBottom) }
    ) {
        val (dotColor, title, hint) = when {
            !on -> Triple(Muted, "الاستماع متوقف", "لن تصل التنبيهات حتى تشغّله")
            Backend.status == Backend.Status.READY -> Triple(Teal, "جاهز للاستقبال", "يعمل في الخلفية — يظهر في Now Bar")
            Backend.status == Backend.Status.REGISTERING -> Triple(Amber, "جارٍ التسجيل…", "ربط الساعة بالباك إند")
            Backend.status == Backend.Status.NO_FIREBASE -> Triple(Red, "Firebase غير مضبوط", Backend.detail)
            Backend.status == Backend.Status.NOT_CONFIGURED -> Triple(Red, "الباك إند غير مضبوط", Backend.detail)
            else -> Triple(Red, "تعذر التسجيل", Backend.detail)
        }
        val blink = rememberInfiniteTransition(label = "blink")
        val a by blink.animateFloat(
            0.4f, 1f, infiniteRepeatable(tween(1200), RepeatMode.Reverse), label = "a"
        )

        Column(
            Modifier.fillMaxSize().background(Ink).verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp, vertical = 30.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                Modifier.size(50.dp).background(Color(0xFF161B22), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.Notifications, contentDescription = null,
                    tint = if (on) Color.White else Muted, modifier = Modifier.size(26.dp))
                Box(
                    Modifier.align(Alignment.BottomEnd).padding(2.dp).size(12.dp)
                        .alpha(if (on && Backend.status == Backend.Status.REGISTERING) a else 1f)
                        .background(dotColor, CircleShape)
                )
            }
            Spacer(Modifier.height(6.dp))
            Text("WakeAlarm", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Text(title, fontSize = 13.sp, color = dotColor, textAlign = TextAlign.Center)
            Text(hint, fontSize = 10.sp, color = Muted, textAlign = TextAlign.Center,
                maxLines = 2, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(10.dp))

            Chip(
                onClick = { Listening.set(ctx, !on) },
                modifier = Modifier.fillMaxWidth().height(46.dp),
                colors = if (on) chipColors(backgroundColor = Color(0xFF2A1215), contentColor = Red)
                         else chipColors(backgroundColor = Yellow, contentColor = Ink),
                icon = {
                    Icon(if (on) Icons.Rounded.Close else Icons.Rounded.PlayArrow,
                        contentDescription = null, modifier = Modifier.size(22.dp))
                },
                label = {
                    Text(if (on) "إيقاف الاستماع" else "تشغيل الاستماع",
                        fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                }
            )
            Spacer(Modifier.height(8.dp))
            Chip(
                onClick = onShowQr,
                modifier = Modifier.fillMaxWidth().height(46.dp),
                colors = chipColors(backgroundColor = Color(0xFF161B22), contentColor = Color.White),
                icon = { Icon(Icons.Rounded.QrCode, contentDescription = null, modifier = Modifier.size(22.dp)) },
                label = { Text("رابط الطلب (QR)", fontSize = 14.sp, maxLines = 1) }
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}

/* ───────────────────────── QR ───────────────────────── */

@Composable
private fun QrScreen(onBack: () -> Unit) {
    val url = remember { Listening.alertUrl() }
    val bitmap = remember(url) { qrBitmap(url, 360) }
    Column(
        Modifier.fillMaxSize().background(Color.White).clickable(onClick = onBack)
            .padding(horizontal = 26.dp, vertical = 22.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(bitmap.asImageBitmap(), contentDescription = "QR", modifier = Modifier.fillMaxWidth().wrapContentHeight())
        Spacer(Modifier.height(4.dp))
        Text("امسحه بالجوال — أضف الرسالة بعد msg=", fontSize = 9.sp, color = Color(0xFF444444),
            textAlign = TextAlign.Center, maxLines = 2)
        Text("اضغط للرجوع", fontSize = 9.sp, color = Color(0xFF888888))
    }
}

private fun qrBitmap(text: String, size: Int): Bitmap {
    val hints = mapOf(EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M, EncodeHintType.MARGIN to 0)
    val m = QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, size, size, hints)
    val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565)
    for (x in 0 until size) for (y in 0 until size)
        bmp.setPixel(x, y, if (m[x, y]) 0xFF000000.toInt() else 0xFFFFFFFF.toInt())
    return bmp
}
