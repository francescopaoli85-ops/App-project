package com.francescopaoli.northstar.ui.fx

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.dp
import com.francescopaoli.northstar.ui.theme.Neon
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

/**
 * Nebulosa animata. Con shader (Android 13+) viene calcolata a metà risoluzione
 * e ingrandita: è morbida, la differenza non si vede e costa 4 volte meno.
 * Senza shader: i blob pulsanti di riserva.
 */
@Composable
fun NebulaLayer(seed: Int, fallback: List<Blob>) {
    // shader solo con scheda grafica attiva (sempre sui telefoni; non nei disegni software)
    val fx = LocalFx.current
    if (Build.VERSION.SDK_INT >= 33 && fx.full && fx.shaders) {
        val shader = remember { Shaders.create(Shaders.NEBULA) }
        if (shader != null) { ShaderNebula(shader, seed); return }
    }
    BlobLayer(fallback)
}

@RequiresApi(33)
@Composable
private fun ShaderNebula(shader: android.graphics.RuntimeShader, seed: Int) {
    val brush = remember(shader) { ShaderBrush(shader) }
    val t by rememberClock(fps = 30)
    val parallax = LocalParallax.current
    Box(Modifier.fillMaxSize()) {
        Canvas(
            Modifier
                .fillMaxSize(0.5f)
                .graphicsLayer { scaleX = 2f; scaleY = 2f; transformOrigin = TransformOrigin(0f, 0f) },
        ) {
            // gli shader richiedono la scheda grafica: se Android disegna in software
            // (raro: certi screenshot di sistema, i test) uso un cielo semplice invece di andare in errore
            if (!drawContext.canvas.nativeCanvas.isHardwareAccelerated) {
                drawRect(Brush.radialGradient(listOf(Neon.Violet.copy(alpha = 0.35f), Color.Transparent), Offset(size.width * 0.8f, size.height * 0.1f), size.width * 0.9f))
                drawRect(Brush.radialGradient(listOf(Neon.Cyan.copy(alpha = 0.16f), Color.Transparent), Offset(size.width * 0.1f, size.height * 0.75f), size.width * 0.8f))
                return@Canvas
            }
            val p = parallax.value
            // i colori seguono il tema attivo
            Neon.Violet.let { shader.setFloatUniform("cViolet", it.red, it.green, it.blue) }
            Neon.Cyan.let { shader.setFloatUniform("cCyan", it.red, it.green, it.blue) }
            Neon.Night.let { shader.setFloatUniform("cNight", it.red, it.green, it.blue) }
            shader.setFloatUniform("iResolution", size.width, size.height)
            shader.setFloatUniform("iTime", t + seed * 31f)
            // livello più lontano: si sposta poco
            shader.setFloatUniform("iOffset", p.x * 14.dp.toPx(), p.y * 14.dp.toPx())
            drawRect(brush)
        }
    }
}

private class Star(
    val x: Float, val y: Float, val r: Float, val depth: Int,
    val color: Color, val period: Float, val phase: Float,
)

/**
 * Campo di stelle su 3 livelli di profondità: le più vicine si spostano di più con la parallasse.
 * Ogni tanto passa una stella cadente.
 */
@Composable
fun StarField(seed: Int, count: Int, modifier: Modifier = Modifier) {
    val stars = remember(seed, count, Neon.palette) {
        val rnd = Random(seed * 7 + 3)
        val colors = listOf(Color.White, Color(0xFFE6DEFF), Neon.Lilac, Neon.Cyan)
        List(count) {
            val depth = rnd.nextInt(3)
            Star(
                rnd.nextFloat(), rnd.nextFloat(), 0.5f + depth * 0.45f + rnd.nextFloat() * 0.4f, depth,
                colors[rnd.nextInt(colors.size)], 1.6f + rnd.nextFloat() * 2.6f, rnd.nextFloat() * 10f,
            )
        }
    }
    val t by rememberClock()
    val parallax = LocalParallax.current
    Canvas(modifier.fillMaxSize()) {
        val p = parallax.value
        stars.forEach { s ->
            val shift = (10 + s.depth * 14).dp.toPx()
            val k = (sin(2 * PI * (t + s.phase) / s.period).toFloat() + 1f) / 2f
            val c = Offset(size.width * s.x + p.x * shift, size.height * s.y + p.y * shift)
            val alpha = 0.25f + 0.75f * k
            // alone morbido per le stelle più grandi
            if (s.depth == 2) drawCircle(
                Brush.radialGradient(listOf(s.color.copy(alpha = 0.35f * alpha), Color.Transparent), c, s.r.dp.toPx() * 4),
                s.r.dp.toPx() * 4, c,
            )
            drawCircle(s.color.copy(alpha = alpha), s.r.dp.toPx(), c)
        }
        shootingStar(t, seed)
    }
}

/** Una stella cadente ogni ~7 secondi, in diagonale, con scia che sfuma. */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.shootingStar(t: Float, seed: Int) {
    val period = 7f
    val cycle = ((t + seed) / period).toInt()
    val local = (t + seed) % period
    val dur = 0.9f
    if (local > dur) return
    val rnd = Random(cycle * 13 + seed)
    val start = Offset(size.width * (0.2f + rnd.nextFloat() * 0.8f), size.height * rnd.nextFloat() * 0.45f)
    val dir = Offset(-0.85f, 0.52f)
    val travel = size.width * 0.55f
    val prog = local / dur
    val head = start + dir * (travel * prog)
    val tail = head - dir * (90.dp.toPx() * (1f - prog * 0.3f))
    val fade = if (prog < 0.15f) prog / 0.15f else 1f - (prog - 0.15f) / 0.85f
    drawLine(
        Brush.linearGradient(listOf(Color.Transparent, Color.White.copy(alpha = 0.9f * fade)), tail, head),
        tail, head, strokeWidth = 2.dp.toPx(), cap = StrokeCap.Round,
    )
    drawCircle(Color.White.copy(alpha = fade), 2.dp.toPx(), head)
}
