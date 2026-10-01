package com.francescopaoli.northstar.ui.fx

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import com.francescopaoli.northstar.ui.theme.Neon
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/*
 * Fisica semplice ma vera: velocità, gravità, attrito dell'aria.
 * Le simulazioni girano a frame e disegnano su Canvas (funzionano su tutti gli Android).
 */

private val festive: List<Color> get() = listOf(Neon.Cyan, Neon.Lilac, Neon.Violet, Color(0xFFFFC857), Color(0xFFFF6BAA), Color.White)

private class Spark(var x: Float, var y: Float, var vx: Float, var vy: Float, val color: Color, var life: Float, val maxLife: Float)
private class Rocket(var x: Float, var y: Float, var vy: Float, val vx: Float, val color: Color)

/** Fuochi d'artificio: razzi che salgono e esplodono in scintille che ricadono con la gravità. */
@Composable
fun Fireworks(modifier: Modifier = Modifier, intensity: Float = 1f) {
    var size by remember { mutableLongStateOf(0L) }
    var tick by remember { mutableLongStateOf(0L) }
    val rockets = remember { mutableListOf<Rocket>() }
    val sparks = remember { mutableListOf<Spark>() }
    val rnd = remember { Random(42) }

    LaunchedEffect(Unit) {
        var last = 0L
        var spawn = 0.2f
        while (true) withFrameNanos { ns ->
            val dt = if (last == 0L) 0.016f else ((ns - last) / 1e9f).coerceAtMost(0.05f)
            last = ns
            val w = (size shr 32).toFloat()
            val h = (size and 0xffffffffL).toFloat()
            if (w <= 0f) return@withFrameNanos
            val g = h * 0.9f
            spawn -= dt
            if (spawn <= 0f) {
                spawn = (0.55f + rnd.nextFloat() * 0.6f) / intensity
                // spinta calcolata per esplodere tra il 12% e il 42% dell'altezza (v = √(2·g·salita))
                val apex = h * (0.12f + rnd.nextFloat() * 0.3f)
                rockets += Rocket(w * (0.15f + rnd.nextFloat() * 0.7f), h, -kotlin.math.sqrt(2f * g * (h - apex)),
                    (rnd.nextFloat() - 0.5f) * w * 0.15f, festive[rnd.nextInt(festive.size)])
            }
            val it = rockets.iterator()
            while (it.hasNext()) {
                val r = it.next()
                r.vy += g * dt; r.y += r.vy * dt; r.x += r.vx * dt
                // all'apice esplode
                if (r.vy >= 0f) {
                    it.remove()
                    val n = 46
                    val speed = h * (0.22f + rnd.nextFloat() * 0.12f)
                    repeat(n) { i ->
                        val a = i * 6.283f / n + rnd.nextFloat() * 0.12f
                        val s = speed * (0.55f + rnd.nextFloat() * 0.45f)
                        val c = if (rnd.nextFloat() < 0.75f) r.color else festive[rnd.nextInt(festive.size)]
                        val life = 1.1f + rnd.nextFloat() * 0.7f
                        sparks += Spark(r.x, r.y, cos(a) * s, sin(a) * s, c, life, life)
                    }
                }
            }
            val si = sparks.iterator()
            while (si.hasNext()) {
                val s = si.next()
                s.vx *= 0.985f; s.vy = s.vy * 0.985f + g * 0.3f * dt
                s.x += s.vx * dt; s.y += s.vy * dt
                s.life -= dt
                if (s.life <= 0f) si.remove()
            }
            tick = ns
        }
    }

    Canvas(modifier.onSizeChanged { s: IntSize -> size = (s.width.toLong() shl 32) or s.height.toLong() }) {
        tick // legge il tick per ridisegnare a ogni frame
        rockets.forEach { r ->
            drawLine(r.color.copy(alpha = 0.9f), Offset(r.x, r.y), Offset(r.x - r.vx * 0.05f, r.y - r.vy * 0.05f), 3f, StrokeCap.Round)
            drawCircle(Color.White, 3.5f, Offset(r.x, r.y))
        }
        sparks.forEach { s ->
            val a = (s.life / s.maxLife).coerceIn(0f, 1f)
            // scia: segmento nella direzione del moto, tremola a fine vita (scintillio)
            val flick = if (a < 0.3f && ((s.x + s.y).toInt() + (tick / 50_000_000L).toInt()) % 2 == 0) 0.3f else 1f
            drawLine(s.color.copy(alpha = a * flick), Offset(s.x, s.y), Offset(s.x - s.vx * 0.04f, s.y - s.vy * 0.04f), 3.2f, StrokeCap.Round)
        }
    }
}

private class Confetto(
    var x: Float, var y: Float, var vx: Float, var vy: Float,
    var angle: Float, val spin: Float, var flip: Float, val flipSpeed: Float,
    val w: Float, val color: Color, val round: Boolean,
)

/**
 * Coriandoli fisici: un'esplosione iniziale dal centro, poi una pioggia continua.
 * Ogni pezzo ruota e "si gira" (effetto 3D) mentre ondeggia nell'aria.
 */
@Composable
fun PhysicsConfetti(modifier: Modifier = Modifier, burstCenter: Offset? = Offset(0.5f, 0.38f)) {
    var size by remember { mutableLongStateOf(0L) }
    var tick by remember { mutableLongStateOf(0L) }
    val pieces = remember { mutableListOf<Confetto>() }
    val rnd = remember { Random(9) }

    fun spawn(w: Float, h: Float, x: Float, y: Float, vx: Float, vy: Float) {
        pieces += Confetto(
            x, y, vx, vy, rnd.nextFloat() * 360f, (rnd.nextFloat() - 0.5f) * 540f, rnd.nextFloat() * 6f,
            3f + rnd.nextFloat() * 5f, w * (0.012f + rnd.nextFloat() * 0.01f), festive[rnd.nextInt(festive.size)], rnd.nextBoolean(),
        )
    }

    LaunchedEffect(Unit) {
        var last = 0L
        var burstDone = false
        var rain = 0f
        while (true) withFrameNanos { ns ->
            val dt = if (last == 0L) 0.016f else ((ns - last) / 1e9f).coerceAtMost(0.05f)
            last = ns
            val w = (size shr 32).toFloat()
            val h = (size and 0xffffffffL).toFloat()
            if (w <= 0f) return@withFrameNanos
            if (!burstDone && burstCenter != null) {
                burstDone = true
                repeat(90) {
                    val a = -1.5708f + (rnd.nextFloat() - 0.5f) * 2.6f
                    val s = h * (0.6f + rnd.nextFloat() * 0.7f)
                    spawn(w, h, w * burstCenter.x, h * burstCenter.y, cos(a) * s, sin(a) * s)
                }
            }
            rain -= dt
            if (rain <= 0f && pieces.size < 160) {
                rain = 0.06f
                spawn(w, h, rnd.nextFloat() * w, -20f, (rnd.nextFloat() - 0.5f) * w * 0.1f, h * 0.1f)
            }
            val it = pieces.iterator()
            while (it.hasNext()) {
                val c = it.next()
                // gravità + resistenza dell'aria forte (i coriandoli "galleggiano") + ondeggiamento
                c.vy += h * 0.9f * dt
                c.vx *= 0.96f; c.vy *= 0.96f
                c.x += (c.vx + sin(c.flip) * w * 0.05f) * dt
                c.y += c.vy * dt
                c.angle += c.spin * dt
                c.flip += c.flipSpeed * dt
                if (c.y > h + 40f) it.remove()
            }
            tick = ns
        }
    }

    Canvas(modifier.onSizeChanged { s -> size = (s.width.toLong() shl 32) or s.height.toLong() }) {
        tick
        pieces.forEach { c ->
            val face = cos(c.flip) // -1..1: il pezzo si gira
            rotate(c.angle, Offset(c.x, c.y)) {
                scale(scaleX = face.coerceIn(-1f, 1f).let { if (kotlin.math.abs(it) < 0.15f) 0.15f else it }, scaleY = 1f, pivot = Offset(c.x, c.y)) {
                    val shade = if (face < 0) 0.7f else 1f
                    val col = c.color.copy(red = c.color.red * shade, green = c.color.green * shade, blue = c.color.blue * shade)
                    if (c.round) drawCircle(col, c.w / 2, Offset(c.x, c.y))
                    else drawRect(col, Offset(c.x - c.w / 2, c.y - c.w * 0.3f), Size(c.w, c.w * 0.6f))
                }
            }
        }
    }
}
