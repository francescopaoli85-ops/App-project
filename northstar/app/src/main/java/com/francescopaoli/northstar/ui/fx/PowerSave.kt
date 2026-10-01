package com.francescopaoli.northstar.ui.fx

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.PowerManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/** true quando il telefono è in risparmio energetico; si aggiorna se lo attivi o spegni. */
@Composable
fun rememberPowerSaveMode(): State<Boolean> {
    val ctx = LocalContext.current
    val pm = remember { ctx.getSystemService(PowerManager::class.java) }
    val state = remember { mutableStateOf(pm?.isPowerSaveMode == true) }
    DisposableEffect(Unit) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context?, i: Intent?) { state.value = pm?.isPowerSaveMode == true }
        }
        androidx.core.content.ContextCompat.registerReceiver(
            ctx, receiver, IntentFilter(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED),
            androidx.core.content.ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        onDispose { ctx.unregisterReceiver(receiver) }
    }
    return state
}
