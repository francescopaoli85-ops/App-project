package com.francescopaoli.northstar.ui.fx

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.francescopaoli.northstar.ui.theme.Neon
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/** Un'esplosione di scintille: piccola al tocco, grande quando spunti un passo. */
class Burst(val at: Offset, val big: Boolean, val seed: Int) {
    var start = -1L
    val life get() = if (big) 950L else 600L
}

/** Chi vuole far esplodere scintille (es. la spunta di un passo) usa questo emettitore. */
class SparkEmitter {
    internal val bursts = mutableStateListOf<Burst>()
    private var n = 0
    fun emit(at: Offset, big: Boolean = false) {
        if (bursts.size > 12) bursts.removeAt(0)
        bursts += Burst(at, big, n++)
    }
}

val LocalSparks = staticCompositionLocalOf<SparkEmitter?> { null }

/**
 * Livello in cima a tutto: ogni tocco fa partire un'onda di luce con scintille.
 * Non "ruba" i tocchi: bottoni e liste sotto funzionano come prima.
 */
@Composable
fun SparkHost(enabled: Boolean, content: @Composable BoxScope.() -> Unit) {
    val emitter = remember { SparkEmitter() }
    Box(
        Modifier.fillMaxSize().then(
            if (!enabled) Modifier else Modifier.pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val ev = awaitPointerEvent(PointerEventPass.Initial)
                        ev.changes.forEach { if (it.pressed && !it.previousPressed) emitter.emit(it.position) }
                    }
                }
            },
        ),
    ) {
        CompositionLocalProvider(LocalSparks provides (if (enabled) emitter else null)) { content() }
        if (enabled) SparkCanvas(emitter)
    }
}

@Composable
private fun SparkCanvas(emitter: SparkEmitter) {
    var now by remember { mutableLongStateOf(0L) }
    val active = emitter.bursts.isNotEmpty()
    // l'orologio gira solo mentre ci sono scintille in volo
    LaunchedEffect(active) {
        while (emitter.bursts.isNotEmpty()) {
            withFrameMillis { ms ->
                now = ms
                emitter.bursts.forEach { if (it.start < 0) it.start = ms }
                emitter.bursts.removeAll { ms - it.start > it.life }
            }
        }
    }
    if (!active) return
    Canvas(Modifier.fillMaxSize()) {
        emitter.bursts.forEach { b ->
            if (b.start < 0) return@forEach
            val p = ((now - b.start).toFloat() / b.life).coerceIn(0f, 1f)
            val ease = 1f - (1f - p) * (1f - p) * (1f - p)
            val fade = 1f - p
            // anello di luce
            val ringR = (if (b.big) 46 else 26).dp.toPx() * ease
            drawCircle(Neon.Cyan.copy(alpha = 0.55f * fade), ringR, b.at, style = Stroke((if (b.big) 3 else 2).dp.toPx() * fade + 0.5f))
            if (b.big) drawCircle(
                Brush.radialGradient(listOf(Neon.Violet.copy(alpha = 0.45f * fade), Color.Transparent), b.at, ringR * 1.4f + 1f),
                ringR * 1.4f + 1f, b.at,
            )
            // scintille radiali con piccola gravità
            val rnd = Random(b.seed)
            val count = if (b.big) 18 else 8
            val colors = if (b.big) listOf(Neon.Cyan, Neon.Lilac, Color(0xFFFFC857), Color.White) else listOf(Neon.Cyan, Neon.Lilac)
            repeat(count) { i ->
                val a = (i.toFloat() / count) * 6.283f + rnd.nextFloat() * 0.5f
                val dist = (if (b.big) 70 else 34).dp.toPx() * (0.6f + rnd.nextFloat() * 0.6f) * ease
                val gravity = 18.dp.toPx() * p * p
                val head = Offset(b.at.x + cos(a) * dist, b.at.y + sin(a) * dist + gravity)
                val tail = Offset(b.at.x + cos(a) * dist * 0.72f, b.at.y + sin(a) * dist * 0.72f + gravity * 0.7f)
                drawLine(colors[i % colors.size].copy(alpha = fade), tail, head, (if (b.big) 2.5f else 1.8f).dp.toPx(), StrokeCap.Round)
            }
        }
    }
}
