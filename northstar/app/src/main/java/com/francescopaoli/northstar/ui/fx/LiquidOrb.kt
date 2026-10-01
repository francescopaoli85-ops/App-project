package com.francescopaoli.northstar.ui.fx

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.rotate
import com.francescopaoli.northstar.ui.theme.Neon
import kotlin.math.cos
import kotlin.math.sin

/**
 * Sfera "liquida" del microfono: il bordo ondeggia sempre un po'
 * e si deforma di più quando parli (segue il volume della voce).
 */
@Composable
fun LiquidOrb(level: Float, active: Boolean, modifier: Modifier = Modifier) {
    val t by rememberClock()
    // il volume arriva a scatti: lo ammorbidisco
    val lv by animateFloatAsState(if (active) level else 0f, tween(140), label = "lv")
    Canvas(modifier) {
        val c = center
        val base = size.minDimension * 0.36f
        val amp = base * (0.035f + 0.16f * lv + if (active) 0.03f else 0f)
        val path = Path()
        val n = 72
        for (i in 0..n) {
            val a = i * 6.2832f / n
            // somma di onde a frequenze diverse = forma organica che non si ripete
            val wobble = sin(3 * a + t * 2.1f) * 0.55f + sin(5 * a - t * 1.6f) * 0.3f + sin(2 * a + t * 3.3f) * 0.35f
            val r = base + amp * wobble
            val p = Offset(c.x + cos(a) * r, c.y + sin(a) * r)
            if (i == 0) path.moveTo(p.x, p.y) else path.lineTo(p.x, p.y)
        }
        path.close()
        // alone
        drawCircle(
            Brush.radialGradient(listOf(Neon.Violet.copy(alpha = 0.55f + 0.3f * lv), Neon.Cyan.copy(alpha = 0.12f), Color.Transparent), c, base * 1.9f),
            base * 1.9f, c,
        )
        // corpo con gradiente che ruota
        rotate(t * 40f, c) {
            drawPath(path, Brush.linearGradient(listOf(Neon.Violet, Neon.Cyan, Neon.Violet), Offset(c.x - base, c.y - base), Offset(c.x + base, c.y + base)))
        }
        // riflesso in alto a sinistra: dà volume
        drawCircle(
            Brush.radialGradient(listOf(Color.White.copy(alpha = 0.35f), Color.Transparent), Offset(c.x - base * 0.35f, c.y - base * 0.4f), base * 0.6f),
            base * 0.6f, Offset(c.x - base * 0.35f, c.y - base * 0.4f),
        )
    }
}
