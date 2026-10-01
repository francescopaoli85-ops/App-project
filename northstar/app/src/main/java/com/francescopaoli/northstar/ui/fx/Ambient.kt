package com.francescopaoli.northstar.ui.fx

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.unit.IntOffset
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp
import com.francescopaoli.northstar.ui.theme.Neon
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

/*
 * Effetti d'ambiente: blob sfocati, particelle, coriandoli, scintille, anelli che esplodono.
 * Tutti disegnati su Canvas guidati da un unico orologio, così restano leggeri.
 */

/**
 * Secondi trascorsi, aggiornati a ogni frame.
 * Si usa con `val t by rememberClock()` e si legge DENTRO il blocco di disegno:
 * così ogni frame ridisegna soltanto, senza ricomporre la schermata.
 */
@Composable
fun rememberClock(fps: Int = 60): androidx.compose.runtime.State<Float> {
    val t = remember { mutableFloatStateOf(0f) }
    LaunchedEffect(fps) {
        val start = withFrameMillis { it }
        val step = 1000L / fps
        var last = 0L
        while (true) withFrameMillis {
            if (it - last >= step - 2) { last = it; t.floatValue = (it - start) / 1000f }
        }
    }
    return t
}

private val palette = listOf(Neon.Cyan, Neon.Lilac, Neon.Violet)

/** Blob di colore: posizione in frazioni dello schermo, raggio in frazioni della larghezza. */
data class Blob(val x: Float, val y: Float, val radius: Float, val color: Color, val alpha: Float = 0.5f, val periodS: Float = 3.2f)

/**
 * Sfondo di ogni schermata: quasi nero + blob pulsanti + particelle che fluttuano
 * su tutta l'altezza (non solo in alto).
 */
@Composable
fun NeonBackdrop(
    modifier: Modifier = Modifier,
    blobs: List<Blob> = listOf(Blob(1.0f, -0.02f, 0.6f, Neon.Violet, 0.45f)),
    particles: Int = 14,
    seed: Int = 1,
    content: @Composable BoxScope.() -> Unit,
) {
    val fx = LocalFx.current
    val haze = remember { HazeState() }
    val parallax = LocalParallax.current
    Box(modifier.fillMaxSize().background(Neon.Night)) {
        // tutto lo sfondo è la sorgente del "vetro": le card ci sfocano sopra
        Box(Modifier.fillMaxSize().haze(haze)) {
            NebulaLayer(seed, blobs)
            if (fx.full) BlobLayer(blobs.map { it.copy(alpha = it.alpha * 0.55f) })
            StarField(seed, if (fx.full) 70 else 30)
            ParticleField(
                if (fx.full) particles else particles / 2,
                Modifier.fillMaxSize().offset {
                    // particelle = livello più vicino: parallasse più forte
                    IntOffset((parallax.value.x * 28.dp.toPx()).toInt(), (parallax.value.y * 28.dp.toPx()).toInt())
                },
                seed,
            )
        }
        CompositionLocalProvider(LocalHaze provides haze) { content() }
    }
}

@Composable
fun BlobLayer(blobs: List<Blob>, modifier: Modifier = Modifier.fillMaxSize()) {
    val t by rememberClock()
    // dentro un header va passato Modifier.matchParentSize(): fillMaxSize lo allargherebbe a tutto lo schermo
    Canvas(modifier) {
        blobs.forEachIndexed { i, b ->
            // opacità e scala pulsano lentamente (tipo pulseBlob del mockup)
            val k = (sin(2 * PI * t / b.periodS + i).toFloat() + 1f) / 2f
            val r = size.width * b.radius * (1f + 0.3f * k)
            val c = Offset(size.width * b.x, size.height * b.y)
            drawCircle(
                Brush.radialGradient(
                    listOf(b.color.copy(alpha = b.alpha * (0.7f + 0.5f * k)), b.color.copy(alpha = 0f)),
                    center = c, radius = r,
                ),
                radius = r, center = c,
            )
        }
    }
}

private class Particle(val x: Float, val y: Float, val r: Float, val color: Color, val period: Float, val phase: Float)

/** Puntini che salgono/scendono con cambio di opacità. */
@Composable
fun ParticleField(count: Int, modifier: Modifier = Modifier, seed: Int = 1) {
    val ps = remember(count, seed) {
        val rnd = Random(seed)
        List(count) { i ->
            // distribuzione a griglia sfalsata: ben sparsi su tutto lo schermo
            val col = i % 2
            Particle(
                x = if (col == 0) rnd.nextFloat() * 0.22f + 0.03f else 0.75f + rnd.nextFloat() * 0.22f,
                y = (i + rnd.nextFloat()) / count,
                r = 1.5f + rnd.nextFloat() * 1.3f,
                color = palette[rnd.nextInt(palette.size)],
                period = 3.2f + rnd.nextFloat() * 1.5f,
                phase = rnd.nextFloat() * 6f,
            )
        }
    }
    val t by rememberClock()
    Canvas(modifier) {
        ps.forEach { p ->
            val k = (sin(2 * PI * (t + p.phase) / p.period).toFloat() + 1f) / 2f
            drawCircle(
                p.color.copy(alpha = 0.25f + 0.7f * k),
                radius = p.r.dp.toPx(),
                center = Offset(size.width * p.x + 5.dp.toPx() * k, size.height * p.y - 16.dp.toPx() * k),
            )
        }
    }
}

private class Piece(val x: Float, val w: Float, val round: Boolean, val color: Color, val period: Float, val delay: Float, val spin: Float)

/** Coriandoli che cadono in continuazione (schermata Celebrazione). */
@Composable
fun ConfettiRain(modifier: Modifier = Modifier, count: Int = 26) {
    val pieces = remember {
        val rnd = Random(7)
        val colors = listOf(Neon.Cyan, Neon.Lilac, Neon.Violet, Color.White, Color(0xFFFFC107))
        List(count) {
            Piece(rnd.nextFloat(), 5f + rnd.nextFloat() * 3f, rnd.nextBoolean(), colors[rnd.nextInt(colors.size)],
                3f + rnd.nextFloat() * 1.5f, rnd.nextFloat() * 3f, 360f + rnd.nextFloat() * 360f)
        }
    }
    val t by rememberClock()
    Canvas(modifier) {
        pieces.forEach { p ->
            val prog = (((t + p.delay) % p.period) / p.period)
            val y = -60f + (size.height + 120f) * prog
            val x = size.width * p.x + sin(prog * 6f + p.delay).toFloat() * 14.dp.toPx()
            val alpha = if (prog < 0.1f) prog * 10f else 0.95f
            val s = p.w.dp.toPx()
            rotate(p.spin * prog, Offset(x, y)) {
                if (p.round) drawCircle(p.color.copy(alpha = alpha), s / 2, Offset(x, y))
                else drawRect(p.color.copy(alpha = alpha), Offset(x - s / 2, y - s / 2), Size(s, s * 0.6f))
            }
        }
    }
}

/** Scintille che salgono (header dei Traguardi). */
@Composable
fun RisingSparks(modifier: Modifier = Modifier, count: Int = 12) {
    val sparks = remember {
        val rnd = Random(3)
        List(count) { Piece(rnd.nextFloat(), 3f + rnd.nextFloat() * 3f, rnd.nextBoolean(), palette[rnd.nextInt(3)],
            4.5f + rnd.nextFloat() * 1.5f, rnd.nextFloat() * 5f, 0f) }
    }
    val t by rememberClock()
    Canvas(modifier) {
        sparks.forEach { p ->
            val prog = ((t + p.delay) % p.period) / p.period
            val alpha = when { prog < 0.1f -> prog * 10f; prog > 0.85f -> (1f - prog) / 0.15f; else -> 1f }
            drawCircle(
                p.color.copy(alpha = alpha.coerceIn(0f, 1f)), p.w.dp.toPx() / 2,
                Offset(size.width * p.x + 20.dp.toPx() * prog, size.height * (1f - prog)),
            )
        }
    }
}

/** Anelli che esplodono verso l'esterno, in loop. */
@Composable
fun BurstRings(modifier: Modifier = Modifier, colors: List<Color> = listOf(Neon.Violet, Neon.Cyan, Neon.Lilac)) {
    val t by rememberClock()
    Canvas(modifier) {
        colors.forEachIndexed { i, c ->
            val prog = ((t + i * 0.4f) % 1.2f) / 1.2f
            drawCircle(
                c.copy(alpha = 0.9f * (1f - prog)),
                radius = size.minDimension / 2 * (0.4f + 2.2f * prog) * 0.5f,
                style = Stroke(2.dp.toPx()),
            )
        }
    }
}

/** Onde di pulsazione attorno al microfono. */
@Composable
fun PulseRings(active: Boolean, modifier: Modifier = Modifier) {
    val t by rememberClock()
    Canvas(modifier) {
        if (!active) return@Canvas
        listOf(Neon.Violet to 0f, Neon.Cyan to 0.3f).forEach { (c, d) ->
            val prog = ((t + d) % 1.3f) / 1.3f
            val base = size.minDimension * 0.4f
            drawCircle(c.copy(alpha = 0.5f * (1f - prog)), radius = base + prog * size.minDimension * 0.35f)
        }
    }
}

/** Stelle che orbitano lentamente (schermata di apertura). */
@Composable
fun OrbitStars(modifier: Modifier = Modifier) {
    val t by rememberClock()
    Canvas(modifier) {
        val c = center
        orbit(t / 9f, c, size.minDimension * 0.42f, listOf(0f to Neon.Cyan, 2.2f to Neon.Lilac), t)
        orbit(-t / 13f, c, size.minDimension * 0.36f, listOf(1.2f to Neon.Lilac, 4f to Neon.Cyan), t)
    }
}

private fun DrawScope.orbit(turns: Float, c: Offset, radius: Float, stars: List<Pair<Float, Color>>, t: Float) {
    stars.forEachIndexed { i, (a0, color) ->
        val a = a0 + turns * 2 * PI.toFloat()
        val tw = (sin(t * 2.6f + i * 1.7f) + 1f) / 2f
        drawCircle(
            color.copy(alpha = 0.2f + 0.8f * tw), (2.4f + tw).dp.toPx(),
            Offset(c.x + radius * kotlin.math.cos(a), c.y + radius * sin(a)),
        )
    }
}

/** Barre dell'onda vocale: reagiscono al volume della voce. */
@Composable
fun VoiceWave(level: Float, active: Boolean, modifier: Modifier = Modifier) {
    val tr = rememberInfiniteTransition(label = "wave")
    val colors = listOf(Neon.Violet, Color(0xFF9857F0), Color(0xFF5FA8F5), Color(0xFF3EC2EE), Neon.Cyan)
    val bars = colors.indices.map { i ->
        tr.animateFloat(0.25f, 1f, infiniteRepeatable(tween(425, i * 150, FastOutSlowInEasing), RepeatMode.Reverse), label = "b$i")
    }
    Canvas(modifier) {
        val w = 3.dp.toPx()
        val gap = 4.dp.toPx()
        bars.forEachIndexed { i, b ->
            val h = size.height * if (active) (b.value * (0.5f + 0.5f * level)).coerceIn(0.2f, 1f) else 0.25f
            val x = i * (w + gap)
            drawRoundRect(colors[i], Offset(x, (size.height - h) / 2), Size(w, h),
                androidx.compose.ui.geometry.CornerRadius(w / 2))
        }
    }
}
