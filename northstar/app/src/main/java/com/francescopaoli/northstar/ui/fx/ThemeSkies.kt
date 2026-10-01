package com.francescopaoli.northstar.ui.fx

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp
import com.francescopaoli.northstar.ui.theme.Neon
import com.francescopaoli.northstar.ui.theme.SkyStyle
import kotlin.math.sin
import kotlin.random.Random

/*
 * Cieli dei temi. Disegnati su Canvas (niente shader): girano su tutti i telefoni
 * e si vedono anche negli screenshot dei test. Animazione lenta a 30 fps.
 */

/** Sceglie il cielo del tema attivo. Notte Neon usa la nebulosa (shader). */
@Composable
fun ThemeSky(seed: Int, fallback: List<Blob>) {
    when (Neon.Style) {
        SkyStyle.NEON -> NebulaLayer(seed, fallback)
        SkyStyle.SUNSET -> SunsetSky()
        SkyStyle.GOLD -> GoldMarbleSky(seed)
        SkyStyle.AURORA -> AuroraSky(seed)
        SkyStyle.OCEAN -> OceanSky(seed)
    }
}

/** Tramonto: cielo a fasce, sole enorme che pulsa all'orizzonte, nuvole lente, aria che tremola. */
@Composable
private fun SunsetSky() {
    val t by rememberClock(30)
    Canvas(Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        drawRect(
            Brush.verticalGradient(
                0f to Neon.Night, 0.5f to Color(0xFF2E1036), 0.76f to Color(0xFF6B1F4C),
                0.9f to Color(0xFFB8433E), 1f to Color(0xFFE07A3C),
            ),
        )
        val pulse = 1f + 0.05f * sin(t * 0.9f)
        val sun = Offset(w * 0.5f, h * 0.9f)
        drawCircle(
            Brush.radialGradient(
                listOf(Color(0xFFFFD08A).copy(alpha = 0.85f), Color(0xFFFF7A45).copy(alpha = 0.45f), Color.Transparent),
                sun, w * 0.6f * pulse,
            ),
            w * 0.6f * pulse, sun,
        )
        // nuvole: fasce morbide che scorrono a velocità diverse
        repeat(4) { i ->
            val y = h * (0.58f + i * 0.07f)
            val bw = w * (0.9f + i * 0.15f)
            val x = ((t * (6f + i * 4f)) % (w + bw)) - bw
            drawRoundRect(
                Brush.horizontalGradient(listOf(Color.Transparent, Neon.Violet.copy(alpha = 0.16f), Color.Transparent), x, x + bw),
                Offset(x, y), Size(bw, 10.dp.toPx() + i * 3.dp.toPx()), androidx.compose.ui.geometry.CornerRadius(40f, 40f),
            )
        }
        // aria calda che tremola sopra l'orizzonte
        repeat(6) { i ->
            val y = h * (0.86f + i * 0.022f)
            val path = Path()
            var x = 0f
            path.moveTo(0f, y)
            while (x <= w) {
                path.lineTo(x, y + sin(x / 26f + t * 2.4f + i) * 2.dp.toPx())
                x += 12f
            }
            drawPath(path, Color(0xFFFFC08A).copy(alpha = 0.07f), style = Stroke(1.5.dp.toPx()))
        }
    }
}

/** Oro e nero: marmo nero con venature d'oro che scorrono piano, e un riflesso che lo attraversa. */
@Composable
private fun GoldMarbleSky(seed: Int) {
    val t by rememberClock(30)
    val veins = remember(seed) {
        val rnd = Random(seed + 11)
        List(7) { floatArrayOf(rnd.nextFloat(), rnd.nextFloat() * 0.3f + 0.1f, rnd.nextFloat() * 6f, 0.5f + rnd.nextFloat()) }
    }
    Canvas(Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        drawRect(Brush.verticalGradient(listOf(Neon.Night, Color(0xFF16130E), Neon.Night)))
        veins.forEach { (y0, amp, phase, speed) ->
            val path = Path()
            val y = h * y0
            path.moveTo(-20f, y)
            path.cubicTo(
                w * 0.3f, y + h * amp * sin(t * 0.15f * speed + phase),
                w * 0.6f, y - h * amp * sin(t * 0.11f * speed + phase * 1.3f),
                w + 20f, y + h * amp * 0.6f * sin(t * 0.13f * speed + phase * 0.7f),
            )
            // alone dorato largo + filo sottile brillante
            drawPath(path, Neon.Violet.copy(alpha = 0.07f), style = Stroke(14.dp.toPx(), cap = StrokeCap.Round))
            drawPath(path, Neon.Cyan.copy(alpha = 0.35f), style = Stroke(1.2.dp.toPx(), cap = StrokeCap.Round))
        }
        // riflesso diagonale che passa ogni ~9 secondi
        val p = (t % 9f) / 9f
        val x = -w + p * 3f * w
        drawRect(
            Brush.linearGradient(
                listOf(Color.Transparent, Neon.Cyan.copy(alpha = 0.06f), Color.Transparent),
                Offset(x, 0f), Offset(x + w * 0.5f, h * 0.5f),
            ),
        )
    }
}

/** Aurora boreale: tende di luce che ondeggiano in alto, montagne scure in basso. */
@Composable
fun AuroraSky(seed: Int, intensity: Float = 1f, modifier: Modifier = Modifier.fillMaxSize(), overlay: Boolean = false) {
    val t by rememberClock(30)
    val peaks = remember(seed) {
        val rnd = Random(seed + 5)
        List(9) { rnd.nextFloat() }
    }
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        // in modalità "overlay" (festa) solo le tende di luce, sopra il cielo già presente
        if (!overlay) drawRect(Brush.verticalGradient(listOf(Neon.Night, Color(0xFF0A1A2C), Color(0xFF0E2236))))
        val step = 6.dp.toPx()
        repeat(3) { c ->
            var x = 0f
            while (x <= w) {
                val u = x / w
                val base = h * (0.12f + c * 0.07f) + h * 0.05f * sin(u * 5f + t * 0.35f + c * 2f)
                val len = h * (0.1f + 0.08f * (sin(u * 9f - t * 0.5f + c) + 1f) / 2f) * intensity.coerceAtMost(1.6f)
                val col = lerp(Neon.Violet, Neon.Cyan, ((sin(u * 3f + t * 0.2f + c) + 1f) / 2f))
                val a = (0.10f + 0.12f * (sin(u * 13f + t * 0.9f + c * 3f) + 1f) / 2f) * intensity
                drawLine(
                    Brush.verticalGradient(listOf(Color.Transparent, col.copy(alpha = a.coerceAtMost(1f)), Color.Transparent), base, base + len),
                    Offset(x, base), Offset(x, base + len), step * 1.1f,
                )
                x += step
            }
        }
        if (overlay) return@Canvas
        // montagne in silhouette
        val m = Path()
        m.moveTo(0f, h)
        peaks.forEachIndexed { i, r ->
            val px = w * i / (peaks.size - 1)
            m.lineTo(px, h * (0.82f + r * 0.08f))
        }
        m.lineTo(w, h)
        m.close()
        drawPath(m, Brush.verticalGradient(listOf(Color(0xFF07111C), Color(0xFF020509)), h * 0.8f, h))
    }
}

/** Oceano: blu che scurisce verso il fondo, raggi di luce dall'alto che ondeggiano. */
@Composable
private fun OceanSky(seed: Int) {
    val t by rememberClock(30)
    val beams = remember(seed) {
        val rnd = Random(seed + 3)
        List(6) { floatArrayOf(rnd.nextFloat(), 14f + rnd.nextFloat() * 40f, rnd.nextFloat() * 6f) }
    }
    Canvas(Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        drawRect(Brush.verticalGradient(0f to Color(0xFF0B3D66), 0.45f to Color(0xFF072845), 1f to Neon.Night))
        beams.forEach { (x0, wd, ph) ->
            val sway = sin(t * 0.4f + ph) * 30.dp.toPx()
            val top = w * x0
            val bw = wd.dp.toPx()
            val a = 0.06f + 0.05f * (sin(t * 0.7f + ph * 2f) + 1f) / 2f
            val path = Path()
            path.moveTo(top - bw / 2, 0f)
            path.lineTo(top + bw / 2, 0f)
            path.lineTo(top + sway + bw * 1.8f, h * 0.75f)
            path.lineTo(top + sway - bw * 1.8f, h * 0.75f)
            path.close()
            drawPath(path, Brush.verticalGradient(listOf(Neon.Cyan.copy(alpha = a), Color.Transparent), 0f, h * 0.75f))
        }
        // superficie dell'acqua: riflessi ondulati in alto
        repeat(3) { i ->
            val path = Path()
            var x = 0f
            val y = (8 + i * 9).dp.toPx()
            path.moveTo(0f, y)
            while (x <= w) {
                path.lineTo(x, y + sin(x / 40f + t * 1.6f + i * 2f) * 3.dp.toPx())
                x += 10f
            }
            drawPath(path, Color.White.copy(alpha = 0.06f), style = Stroke(2.dp.toPx()))
        }
    }
}

/* -------------------------------- particelle -------------------------------- */

/** Particelle del tema attivo (Notte Neon: puntini; gli altri: il loro elemento). */
@Composable
fun ThemeParticles(count: Int, modifier: Modifier, seed: Int) {
    when (Neon.Style) {
        SkyStyle.NEON -> ParticleField(count, modifier, seed)
        else -> StyledParticles(Neon.Style, count + 8, modifier, seed)
    }
}

@Composable
private fun StyledParticles(style: SkyStyle, count: Int, modifier: Modifier, seed: Int) {
    val ps = remember(seed, count, style) {
        val rnd = Random(seed * 31 + style.ordinal)
        List(count) { FloatArray(6) { rnd.nextFloat() } } // x, y0, velocità, dimensione, fase, extra
    }
    val t by rememberClock()
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        ps.forEach { p ->
            val (x0, y0, sp, sz, ph) = p
            when (style) {
                SkyStyle.SUNSET -> { // lucciole che salgono lente e ogni tanto si spengono
                    val y = h - ((y0 * h + t * h * (0.015f + sp * 0.02f)) % h)
                    val x = w * x0 + sin(t * 0.8f + ph * 6f) * 14.dp.toPx()
                    val blink = ((sin(t * (1.2f + sp) + ph * 9f) + 1f) / 2f).let { it * it * it }
                    val r = (1.5f + sz * 2f).dp.toPx()
                    drawCircle(Brush.radialGradient(listOf(Color(0xFFFFD08A).copy(alpha = 0.7f * blink), Color.Transparent), Offset(x, y), r * 5), r * 5, Offset(x, y))
                    drawCircle(Color(0xFFFFE2B0).copy(alpha = blink), r, Offset(x, y))
                }
                SkyStyle.GOLD -> { // pagliuzze d'oro che cadono girando e brillano quando prendono luce
                    val y = (y0 * h + t * h * (0.02f + sp * 0.03f)) % h
                    val x = w * x0 + sin(t * 0.6f + ph * 6f) * 18.dp.toPx()
                    val flip = sin(t * (1.5f + sp * 2f) + ph * 7f)
                    val glint = if (flip > 0.92f) (flip - 0.92f) / 0.08f else 0f
                    val len = (2f + sz * 4f).dp.toPx()
                    val wd = len * 0.45f * kotlin.math.abs(flip).coerceAtLeast(0.15f)
                    drawRect(lerp(Neon.Violet, Neon.Cyan, (flip + 1f) / 2f).copy(alpha = 0.75f), Offset(x - wd / 2, y - len / 2), Size(wd, len))
                    if (glint > 0f) {
                        drawLine(Color.White.copy(alpha = glint), Offset(x - len, y), Offset(x + len, y), 1.dp.toPx())
                        drawLine(Color.White.copy(alpha = glint), Offset(x, y - len), Offset(x, y + len), 1.dp.toPx())
                    }
                }
                SkyStyle.AURORA -> { // neve leggera che ondeggia
                    val y = (y0 * h + t * h * (0.03f + sp * 0.04f)) % h
                    val x = (w * x0 + sin(t * 0.7f + ph * 6f) * 20.dp.toPx())
                    drawCircle(Color.White.copy(alpha = 0.35f + 0.4f * sz), (1f + sz * 2.2f).dp.toPx(), Offset(x, y))
                }
                SkyStyle.OCEAN -> { // bolle che salgono tremolando
                    val y = h - ((y0 * h + t * h * (0.03f + sp * 0.05f)) % h)
                    val x = w * x0 + sin(t * 2f + ph * 6f) * 6.dp.toPx()
                    val r = (2f + sz * 5f).dp.toPx()
                    drawCircle(Neon.Lilac.copy(alpha = 0.35f), r, Offset(x, y), style = Stroke(1.dp.toPx()))
                    drawCircle(Color.White.copy(alpha = 0.5f), r * 0.25f, Offset(x - r * 0.35f, y - r * 0.35f))
                }
                SkyStyle.NEON -> Unit
            }
        }
    }
}

private operator fun FloatArray.component6() = this[5]
