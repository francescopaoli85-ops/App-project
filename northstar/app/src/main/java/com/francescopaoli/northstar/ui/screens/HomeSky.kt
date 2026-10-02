package com.francescopaoli.northstar.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.francescopaoli.northstar.data.Goal
import com.francescopaoli.northstar.ui.components.color
import com.francescopaoli.northstar.ui.fx.rememberClock
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

/** Posizioni del cielo (frazioni del riquadro): disposte come una costellazione, mai sovrapposte. */
private val SLOTS = listOf(
    Offset(0.20f, 0.30f), Offset(0.52f, 0.52f), Offset(0.80f, 0.22f), Offset(0.36f, 0.12f),
    Offset(0.70f, 0.66f), Offset(0.12f, 0.62f), Offset(0.90f, 0.48f),
)
private val POLAR = Offset(0.5f, 0.90f)
private val GOLD = Color(0xFFFFD76A)

/**
 * "Il tuo cielo": ogni obiettivo è una stella del colore della sua area, collegata alle altre.
 * Più azioni fai, più la stella è grande e luminosa. In basso la Stella Polare dorata.
 * Tocchi una stella e si apre l'obiettivo; tocchi la Stella Polare e si apre la tua Stella Polare.
 */
@Composable
fun HomeSky(
    goals: List<Goal>,
    onGoal: (String) -> Unit,
    onPolaris: () -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 220.dp,
) {
    val shown = goals.take(SLOTS.size)
    val t by rememberClock(fps = 30)
    Canvas(
        modifier.fillMaxWidth().height(height).pointerInput(shown) {
            detectTapGestures { tap ->
                val w = size.width.toFloat(); val h = size.height.toFloat()
                val hit = shown.indices.minByOrNull { i -> dist(tap, Offset(SLOTS[i].x * w, SLOTS[i].y * h)) }
                val polar = Offset(POLAR.x * w, POLAR.y * h)
                val radius = 40.dp.toPx()
                when {
                    hit != null && dist(tap, Offset(SLOTS[hit].x * w, SLOTS[hit].y * h)) < radius -> onGoal(shown[hit].id)
                    dist(tap, polar) < radius -> onPolaris()
                }
            }
        },
    ) {
        val pts = shown.indices.map { Offset(SLOTS[it].x * size.width, SLOTS[it].y * size.height) }
        val polar = Offset(POLAR.x * size.width, POLAR.y * size.height)

        // la Stella Polare
        val pp = 0.5f + 0.5f * sin(t * 1.3f)
        drawCircle(Brush.radialGradient(listOf(GOLD.copy(alpha = 0.35f + 0.15f * pp), Color.Transparent), polar, 30.dp.toPx()), 30.dp.toPx(), polar)
        drawStar(polar, 11.dp.toPx(), GOLD)

        // una stella per obiettivo: grandezza e luce crescono con le azioni fatte
        shown.forEachIndexed { i, g ->
            val p = pts[i]
            val prog = g.progress
            val c = g.area.color
            val tw = 0.5f + 0.5f * sin(t * 1.7f + i * 1.9f)
            val glow = (18 + prog * 26).dp.toPx() * (0.9f + 0.15f * tw)
            drawCircle(Brush.radialGradient(listOf(c.copy(alpha = 0.55f + 0.2f * prog), Color.Transparent), p, glow), glow, p)
            drawStar(p, (6 + prog * 8).dp.toPx(), Color.White.copy(alpha = 0.75f + 0.25f * tw))
        }
    }
}

private fun dist(a: Offset, b: Offset) = hypot(a.x - b.x, a.y - b.y)

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

/** Una frase al giorno, sempre la stessa per tutta la giornata. */
fun phraseOfTheDay(day: Long = java.time.LocalDate.now().toEpochDay()): String {
    val phrases = listOf(
        "Un obiettivo è un sogno con una data.",
        "Ogni stella si accende un passo alla volta.",
        "Non serve vedere tutta la strada: basta il prossimo passo.",
        "La costanza batte l'intensità.",
        "Piccoli passi, ogni giorno, portano lontano.",
        "Quello che fai oggi illumina il tuo domani.",
        "Segui la tua Stella Polare, anche nelle notti nuvolose.",
    )
    return phrases[(day % phrases.size).toInt()]
}
