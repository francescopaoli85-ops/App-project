package com.francescopaoli.northstar.ui.fx

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LifecycleResumeEffect

/**
 * Parallasse dal giroscopio: inclinando il telefono stelle e nebulosa si spostano a strati.
 * Il sensore è attivo solo con l'app in primo piano.
 * La "posizione neutra" segue lentamente il telefono, così se lo tieni storto non resta tutto spostato.
 */
@Composable
fun rememberParallax(enabled: Boolean): State<Offset> {
    val ctx = LocalContext.current
    val state = remember { mutableStateOf(Offset.Zero) }
    LifecycleResumeEffect(enabled) {
        val sm = ctx.getSystemService(SensorManager::class.java)
        val sensor = sm?.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR)
            ?: sm?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        if (!enabled || sm == null || sensor == null) {
            state.value = Offset.Zero
            return@LifecycleResumeEffect onPauseOrDispose { }
        }
        val rot = FloatArray(9)
        val ori = FloatArray(3)
        var basePitch = Float.NaN
        var baseRoll = 0f
        var x = 0f
        var y = 0f
        val listener = object : SensorEventListener {
            override fun onSensorChanged(e: SensorEvent) {
                SensorManager.getRotationMatrixFromVector(rot, e.values)
                SensorManager.getOrientation(rot, ori)
                val pitch = ori[1]
                val roll = ori[2]
                if (basePitch.isNaN()) { basePitch = pitch; baseRoll = roll }
                basePitch += (pitch - basePitch) * 0.01f
                baseRoll += (roll - baseRoll) * 0.01f
                // circa 20° di inclinazione = spostamento massimo; filtro per togliere i tremolii
                val tx = ((roll - baseRoll) / 0.35f).coerceIn(-1f, 1f)
                val ty = ((pitch - basePitch) / 0.35f).coerceIn(-1f, 1f)
                x += (tx - x) * 0.15f
                y += (ty - y) * 0.15f
                state.value = Offset(x, y)
            }
            override fun onAccuracyChanged(s: Sensor?, accuracy: Int) {}
        }
        sm.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_GAME)
        onPauseOrDispose { sm.unregisterListener(listener) }
    }
    return state
}
