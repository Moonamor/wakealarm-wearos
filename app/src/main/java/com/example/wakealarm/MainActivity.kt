package com.example.wakealarm

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.*
import java.net.NetworkInterface

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        startForegroundService(Intent(this, AlarmService::class.java))

        setContent {
            var ringing by remember { mutableStateOf(AlarmService.ringing) }
            DisposableEffect(Unit) {
                AlarmService.onStateChange = { r -> runOnUiThread { ringing = r } }
                onDispose { AlarmService.onStateChange = null }
            }
            MaterialTheme {
                Column(
                    Modifier.fillMaxSize().padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    if (ringing) {
                        Text("استيقظ!", fontSize = 22.sp, color = Color.Red)
                        Spacer(Modifier.height(10.dp))
                        Button(
                            onClick = {
                                startService(Intent(this@MainActivity, AlarmService::class.java)
                                    .setAction(AlarmService.ACTION_STOP))
                            },
                            modifier = Modifier.size(110.dp),
                            colors = ButtonDefaults.buttonColors(backgroundColor = Color.Red)
                        ) { Text("إيقاف", fontSize = 20.sp) }
                    } else {
                        Text("جاهز ويستمع", fontSize = 16.sp)
                        Spacer(Modifier.height(6.dp))
                        Text("http://${localIp()}:${AlarmService.PORT}/alert?value=${AlarmService.SECRET}",
                            fontSize = 10.sp, color = Color.Gray)
                    }
                }
            }
        }
    }

    private fun localIp(): String = try {
        NetworkInterface.getNetworkInterfaces().toList()
            .flatMap { it.inetAddresses.toList() }
            .firstOrNull { !it.isLoopbackAddress && it.hostAddress?.contains(':') == false }
            ?.hostAddress ?: "?"
    } catch (e: Exception) { "?" }
}
