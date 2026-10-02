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
 * Usa il sensore di rotazione; se il telefono non ce l'ha, ripiega sull'accelerometro (c'è sempre).
 * Il sensore è attivo solo con l'app in primo piano.
 * La "posizione neutra" segue piano il telefono, così se lo tieni storto non resta tutto spostato.
 */
@Composable
fun rememberParallax(enabled: Boolean): State<Offset> {
    val ctx = LocalContext.current
    val state = remember { mutableStateOf(Offset.Zero) }
    LifecycleResumeEffect(enabled) {
        val sm = ctx.getSystemService(SensorManager::class.java)
        val rotation = sm?.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR)
            ?: sm?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        val gravity = sm?.getDefaultSensor(Sensor.TYPE_GRAVITY) ?: sm?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val sensor = rotation ?: gravity
        if (!enabled || sm == null || sensor == null) {
            state.value = Offset.Zero
            return@LifecycleResumeEffect onPauseOrDispose { }
        }
        val tracker = TiltTracker()
        val rot = FloatArray(9)
        val ori = FloatArray(3)
        val listener = object : SensorEventListener {
            override fun onSensorChanged(e: SensorEvent) {
                val (roll, pitch) = if (sensor === rotation) {
                    SensorManager.getRotationMatrixFromVector(rot, e.values)
                    SensorManager.getOrientation(rot, ori)
                    ori[2] to ori[1]
                } else {
                    // accelerometro: la gravità sugli assi x/y dice quanto è inclinato (radianti circa)
                    val g = SensorManager.GRAVITY_EARTH
                    (-e.values[0] / g).coerceIn(-1f, 1f) to (e.values[1] / g).coerceIn(-1f, 1f)
                }
                state.value = tracker.update(roll, pitch)
            }
            override fun onAccuracyChanged(s: Sensor?, accuracy: Int) {}
        }
        sm.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_GAME)
        onPauseOrDispose { sm.unregisterListener(listener) }
    }
    return state
}

/**
 * Da inclinazione (radianti) a spostamento -1..1.
 * Circa 15° = spostamento massimo; la posizione neutra rientra in ~8 secondi.
 */
class TiltTracker(private val range: Float = 0.26f, private val recenter: Float = 0.0025f, private val smooth: Float = 0.18f) {
    private var baseRoll = Float.NaN
    private var basePitch = 0f
    private var x = 0f
    private var y = 0f

    fun update(roll: Float, pitch: Float): Offset {
        if (baseRoll.isNaN()) { baseRoll = roll; basePitch = pitch }
        baseRoll += (roll - baseRoll) * recenter
        basePitch += (pitch - basePitch) * recenter
        val tx = ((roll - baseRoll) / range).coerceIn(-1f, 1f)
        val ty = ((pitch - basePitch) / range).coerceIn(-1f, 1f)
        x += (tx - x) * smooth
        y += (ty - y) * smooth
        return Offset(x, y)
    }
}
