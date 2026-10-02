package com.francescopaoli.northstar.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Face
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Person
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.francescopaoli.northstar.data.Area
import com.francescopaoli.northstar.ui.fx.LocalFx
import com.francescopaoli.northstar.ui.theme.Neon
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * "Sentiero verso la stella": l'avanzamento senza percentuali.
 * Una scia che avanza tappa dopo tappa (una per azione) verso la Stella Polare in fondo,
 * che si accende d'oro quando tutti i passi sono fatti.
 */
@Composable
fun StarTrail(done: Int, total: Int, modifier: Modifier = Modifier, height: Dp = 22.dp, muted: Boolean = false) {
    val target = if (total <= 0) 0f else done.coerceIn(0, total).toFloat() / total
    val anim = remember { Animatable(0f) }
    LaunchedEffect(target) { anim.animateTo(target, tween(900, easing = FastOutSlowInEasing)) }
    val pulse = if (LocalFx.current.animated) {
        rememberInfiniteTransition(label = "trail").animateFloat(
            0f, 1f, infiniteRepeatable(tween(1600), RepeatMode.Reverse), label = "pulse",
        ).value
    } else 0.5f
    val head = if (muted) Neon.Text3 else Neon.Cyan
    val complete = total > 0 && done >= total
    Canvas(modifier.fillMaxWidth().height(height)) {
        val y = size.height / 2
        val starR = size.height * 0.42f
        val x0 = 4.dp.toPx()
        val x1 = size.width - starR * 2.4f
        val track = 3.dp.toPx()
        // binario
        drawLine(Neon.Track, Offset(x0, y), Offset(x1, y), track, StrokeCap.Round)
        // scia percorsa
        val xh = x0 + (x1 - x0) * anim.value
        if (anim.value > 0f) drawLine(
            Brush.horizontalGradient(listOf(Neon.Violet.copy(alpha = 0.25f), head), startX = x0, endX = xh.coerceAtLeast(x0 + 1f)),
            Offset(x0, y), Offset(xh, y), track, StrokeCap.Round,
        )
        // tappe: una per azione
        if (total > 1) for (i in 1 until total) {
            val x = x0 + (x1 - x0) * i / total
            drawCircle(if (i <= done) Color.White.copy(alpha = 0.85f) else Neon.Inactive, 1.8.dp.toPx(), Offset(x, y))
        }
        // testa luminosa
        if (anim.value > 0f && !complete) {
            drawCircle(head.copy(alpha = 0.18f + 0.14f * pulse), 7.dp.toPx() + 2.dp.toPx() * pulse, Offset(xh, y))
            drawCircle(Color.White, 3.5.dp.toPx(), Offset(xh, y))
        }
        // la Stella Polare in fondo
        val sc = Offset(size.width - starR * 1.15f, y)
        if (complete) drawCircle(Color(0xFFFFD76A).copy(alpha = 0.25f + 0.2f * pulse), starR * 1.7f, sc)
        drawStar(sc, starR, if (complete) Color(0xFFFFD76A) else if (anim.value > 0.75f) Neon.Lilac else Neon.Inactive)
    }
}

private fun DrawScope.drawStar(c: Offset, r: Float, color: Color) {
    val p = Path()
    for (i in 0 until 10) {
        val rr = if (i % 2 == 0) r else r * 0.45f
        val a = -PI / 2 + i * PI / 5
        val pt = Offset(c.x + (rr * cos(a)).toFloat(), c.y + (rr * sin(a)).toFloat())
        if (i == 0) p.moveTo(pt.x, pt.y) else p.lineTo(pt.x, pt.y)
    }
    p.close()
    drawPath(p, color)
}

/** Colore e icona di ogni area: per riconoscere gli obiettivi a colpo d'occhio. */
val Area.color: Color
    get() = when (this) {
        Area.LAVORO -> Color(0xFF38BDF8)
        Area.SALUTE -> Color(0xFF2EE6A6)
        Area.RELAZIONI -> Color(0xFFFF7EB3)
        Area.PERSONALE -> Color(0xFFB08CF7)
    }

val Area.icon: ImageVector
    get() = when (this) {
        Area.LAVORO -> Icons.Outlined.Build
        Area.SALUTE -> Icons.Outlined.FavoriteBorder
        Area.RELAZIONI -> Icons.Outlined.Face
        Area.PERSONALE -> Icons.Outlined.Person
    }

val Area.hint: String
    get() = when (this) {
        Area.LAVORO -> "carriera, studio, progetti"
        Area.SALUTE -> "corpo, sport, sonno, cibo"
        Area.RELAZIONI -> "famiglia, amici, coppia"
        Area.PERSONALE -> "passioni, crescita, tempo per te"
    }
