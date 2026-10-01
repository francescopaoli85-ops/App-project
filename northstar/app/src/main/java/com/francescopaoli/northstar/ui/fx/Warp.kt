package com.francescopaoli.northstar.ui.fx

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import com.francescopaoli.northstar.ui.theme.Neon
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * "Salto nell'iperspazio": le stelle si allungano verso i bordi accelerando,
 * poi un lampo di luce. Usato quando si salva un nuovo obiettivo.
 */
@Composable
fun WarpOverlay(onFinished: () -> Unit, durationMs: Int = 1300) {
    val p = remember { Animatable(0f) }
    val stars = remember {
        val rnd = Random(77)
        val colors = listOf(Color.White, Neon.Cyan, Neon.Lilac, Color(0xFFE6DEFF))
        List(170) { Triple(rnd.nextFloat() * 6.283f, 0.02f + rnd.nextFloat() * 0.5f, colors[rnd.nextInt(colors.size)]) }
    }
    LaunchedEffect(Unit) {
        p.animateTo(1f, tween(durationMs, easing = LinearEasing))
        onFinished()
    }
    Canvas(Modifier.fillMaxSize()) {
        val t = p.value
        // lo sfondo si scurisce, poi esplode in bianco/viola
        val dim = (t / 0.25f).coerceAtMost(1f)
        drawRect(Neon.Night.copy(alpha = 0.85f * dim))
        val c = center
        val maxR = size.maxDimension * 0.8f
        val speed = t * t * t // accelera
        stars.forEach { (a, r0, col) ->
            val r = maxR * (r0 + speed * 2.2f)
            val len = maxR * (0.01f + speed * 0.9f) * (0.4f + r0)
            val dir = Offset(cos(a), sin(a))
            val head = c + dir * r
            val tail = c + dir * (r - len).coerceAtLeast(maxR * r0 * 0.9f)
            drawLine(
                Brush.linearGradient(listOf(Color.Transparent, col.copy(alpha = (0.3f + t).coerceAtMost(1f))), tail, head),
                tail, head, strokeWidth = 1.5f + 3f * speed, cap = StrokeCap.Round,
            )
        }
        // nucleo luminoso al centro che cresce
        drawCircle(
            Brush.radialGradient(listOf(Color.White.copy(alpha = 0.9f * t), Neon.Violet.copy(alpha = 0.5f * t), Color.Transparent), c, maxR * 0.5f * t + 1f),
            maxR * 0.5f * t + 1f, c,
        )
        if (t > 0.82f) drawRect(Color.White.copy(alpha = ((t - 0.82f) / 0.18f).coerceIn(0f, 1f)))
    }
}
