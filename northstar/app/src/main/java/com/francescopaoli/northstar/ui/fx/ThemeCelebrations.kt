package com.francescopaoli.northstar.ui.fx

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.francescopaoli.northstar.ui.theme.Neon
import com.francescopaoli.northstar.ui.theme.SkyStyle
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/** La festa quando raggiungi un obiettivo: diversa per ogni tema. */
@Composable
fun ThemeCelebration(modifier: Modifier = Modifier.fillMaxSize()) {
    val center = Offset(0.5f, 0.36f)
    when (Neon.Style) {
        SkyStyle.NEON -> Box(modifier) {
            Fireworks(Modifier.fillMaxSize())
            PhysicsConfetti(Modifier.fillMaxSize(), burstCenter = center)
        }
        SkyStyle.SUNSET -> Lanterns(modifier)
        SkyStyle.GOLD -> PhysicsConfetti(
            modifier, burstCenter = center,
            colors = listOf(Neon.Violet, Neon.Cyan, Neon.Lilac, Color(0xFFFFF4D6), Color(0xFF8C6A1C)),
        )
        SkyStyle.AURORA -> Box(modifier) {
            AuroraSky(seed = 2, intensity = 2.2f, modifier = Modifier.fillMaxSize(), overlay = true)
            Constellation(Modifier.fillMaxSize(), center)
        }
        SkyStyle.OCEAN -> Box(modifier) {
            Bubbles(Modifier.fillMaxSize())
            Jellyfish(Modifier.fillMaxSize())
        }
    }
}

/** Lanterne volanti: salgono ondeggiando, illuminate, e si perdono in cielo. */
@Composable
private fun Lanterns(modifier: Modifier) {
    val t by rememberClock()
    val ls = remember { val r = Random(21); List(16) { FloatArray(5) { r.nextFloat() } } }
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        ls.forEach { (x0, sp, ph, sz, delay) ->
            val local = t - delay * 3f
            if (local < 0f) return@forEach
            val prog = (local * (0.07f + sp * 0.06f)) % 1.25f
            val y = h * (1.1f - prog)
            val x = w * (0.08f + x0 * 0.84f) + sin(t * 0.6f + ph * 6f) * 18.dp.toPx()
            val s = (0.7f + sz * 0.7f) * (1f - prog * 0.35f)
            val fade = (1.1f - prog).coerceIn(0f, 1f)
            val bw = 18.dp.toPx() * s
            val bh = 24.dp.toPx() * s
            val flicker = 0.85f + 0.15f * sin(t * 9f + ph * 20f)
            drawCircle(
                Brush.radialGradient(listOf(Color(0xFFFFA14A).copy(alpha = 0.4f * fade * flicker), Color.Transparent), Offset(x, y), bh * 2.2f),
                bh * 2.2f, Offset(x, y),
            )
            drawRoundRect(
                Brush.verticalGradient(listOf(Color(0xFFFFC870).copy(alpha = fade), Color(0xFFE0603F).copy(alpha = fade)), y - bh / 2, y + bh / 2),
                Offset(x - bw / 2, y - bh / 2), Size(bw, bh), CornerRadius(bw * 0.45f, bw * 0.45f),
            )
            drawCircle(Color(0xFFFFF0C0).copy(alpha = fade * flicker), bw * 0.18f, Offset(x, y + bh * 0.25f))
        }
    }
}

/** Le stelle si uniscono una alla volta in una costellazione a forma di stella. */
@Composable
private fun Constellation(modifier: Modifier, center: Offset) {
    val t by rememberClock()
    Canvas(modifier) {
        val c = Offset(size.width * center.x, size.height * center.y)
        val r = size.width * 0.4f
        val pts = List(5) { i ->
            val a = -1.5708f + i * 1.2566f
            Offset(c.x + cos(a) * r, c.y + sin(a) * r)
        }
        val order = listOf(0, 2, 4, 1, 3, 0) // pentagramma
        val seg = 0.45f
        for (i in 0 until order.size - 1) {
            val q = ((t - 0.4f - i * seg) / seg).coerceIn(0f, 1f)
            if (q <= 0f) continue
            val a = pts[order[i]]
            val b = pts[order[i + 1]]
            drawLine(Neon.Cyan.copy(alpha = 0.55f), a, a + (b - a) * q, 1.5.dp.toPx(), StrokeCap.Round)
        }
        pts.forEachIndexed { i, p ->
            val tw = (sin(t * 3f + i * 1.7f) + 1f) / 2f
            drawCircle(Brush.radialGradient(listOf(Color.White.copy(alpha = 0.6f), Color.Transparent), p, 14.dp.toPx()), 14.dp.toPx(), p)
            drawCircle(Color.White.copy(alpha = 0.6f + 0.4f * tw), (2.5f + tw).dp.toPx(), p)
        }
    }
}

/** Bolle che salgono veloci dal fondo, tante. */
@Composable
private fun Bubbles(modifier: Modifier) {
    val t by rememberClock()
    val bs = remember { val r = Random(8); List(40) { FloatArray(4) { r.nextFloat() } } }
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        bs.forEach { (x0, sp, sz, ph) ->
            val y = h * 1.05f - ((ph + t * (0.12f + sp * 0.15f)) % 1.1f) * h
            val x = w * x0 + sin(t * 3f + ph * 9f) * 8.dp.toPx()
            val r = (3f + sz * 7f).dp.toPx()
            drawCircle(Neon.Lilac.copy(alpha = 0.5f), r, Offset(x, y), style = Stroke(1.2.dp.toPx()))
            drawCircle(Color.White.copy(alpha = 0.6f), r * 0.25f, Offset(x - r * 0.35f, y - r * 0.35f))
        }
    }
}

/** Una medusa luminosa sale dal fondo e poi galleggia sopra la stella. */
@Composable
private fun Jellyfish(modifier: Modifier) {
    val t by rememberClock()
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val rise = (t / 3.5f).coerceAtMost(1f)
        val easeRise = 1f - (1f - rise) * (1f - rise)
        val bob = sin(t * 1.4f) * 10.dp.toPx()
        val cx = w * 0.5f + sin(t * 0.5f) * 20.dp.toPx()
        val cy = h * 1.1f - easeRise * h * 0.95f + bob
        val bw = 80.dp.toPx()
        val pulse = 1f + 0.08f * sin(t * 2.8f)
        // alone
        drawCircle(Brush.radialGradient(listOf(Neon.Cyan.copy(alpha = 0.3f), Color.Transparent), Offset(cx, cy), bw * 1.6f), bw * 1.6f, Offset(cx, cy))
        // tentacoli ondulati
        repeat(6) { i ->
            val path = Path()
            val sx = cx + (i - 2.5f) * bw * 0.13f
            path.moveTo(sx, cy)
            var k = 0f
            while (k <= 1f) {
                path.lineTo(sx + sin(k * 7f + t * 3f + i) * 8.dp.toPx() * k, cy + k * 110.dp.toPx())
                k += 0.05f
            }
            drawPath(path, Neon.Lilac.copy(alpha = 0.45f), style = Stroke(1.5.dp.toPx(), cap = StrokeCap.Round))
        }
        // campana
        val bell = Path()
        val half = bw / 2 * pulse
        bell.moveTo(cx - half, cy)
        bell.cubicTo(cx - half, cy - bw * 0.75f, cx + half, cy - bw * 0.75f, cx + half, cy)
        var x = cx + half
        while (x >= cx - half) {
            bell.lineTo(x, cy + sin((x - cx) / 6f + t * 4f) * 3.dp.toPx())
            x -= 4f
        }
        bell.close()
        drawPath(bell, Brush.verticalGradient(listOf(Neon.Lilac.copy(alpha = 0.75f), Neon.Cyan.copy(alpha = 0.3f)), cy - bw * 0.6f, cy))
        drawCircle(Color.White.copy(alpha = 0.35f), bw * 0.12f, Offset(cx - half * 0.35f, cy - bw * 0.35f))
    }
}
