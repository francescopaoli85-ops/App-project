package com.francescopaoli.northstar.ui.fx

import android.graphics.BlurMaskFilter
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.francescopaoli.northstar.ui.theme.Neon
import kotlinx.coroutines.delay

/*
 * Libreria di effetti "Notte Neon".
 * Requisito esplicito: tante animazioni, marcate, sempre in movimento.
 * Loop brevi (1.5–2.5 s) così l'occhio percepisce davvero il movimento.
 */

/** Curva morbida con leggero overshoot, tipo cubic-bezier(.2,.8,.2,1) del mockup. */
val SoftOut = CubicBezierEasing(0.2f, 0.8f, 0.2f, 1f)

/** Gradiente viola→ciano che scorre continuamente (background-position shift). */
fun Modifier.animatedGradient(
    corner: Dp,
    colors: List<Color> = Neon.gradient,
    durationMs: Int = 2400,
): Modifier = composed {
    val t by rememberInfiniteTransition(label = "grad").animateFloat(
        0f, 1f, infiniteRepeatable(tween(durationMs, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "g",
    )
    drawBehind {
        // il gradiente è largo 2.2x (come background-size: 220%) e scorre in diagonale
        val shift = -1.2f * size.width * t
        val brush = Brush.linearGradient(
            colors,
            start = Offset(shift, 0f),
            end = Offset(shift + 2.2f * size.width, size.height * 1.4f),
        )
        val r = corner.toPx().coerceAtMost(size.minDimension / 2)
        drawRoundRect(brush, cornerRadius = CornerRadius(r, r))
    }
}

/**
 * Bagliore sfocato attorno all'elemento (box-shadow neon).
 * Se [pulse] è attivo il bagliore "respira" tra viola e ciano.
 */
fun Modifier.glow(
    corner: Dp,
    color: Color = Neon.Violet,
    color2: Color = Neon.Cyan,
    blur: Dp = 22.dp,
    pulse: Boolean = true,
    durationMs: Int = 1600,
): Modifier = composed {
    val p by if (pulse) rememberInfiniteTransition(label = "glow").animateFloat(
        0f, 1f, infiniteRepeatable(tween(durationMs, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "p",
    ) else remember { androidx.compose.runtime.mutableFloatStateOf(0.5f) }
    val density = LocalDensity.current
    val paint = remember { android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG) }
    drawBehind {
        val blurPx = with(density) { blur.toPx() } * (0.7f + 0.8f * p)
        paint.maskFilter = BlurMaskFilter(blurPx, BlurMaskFilter.Blur.NORMAL)
        paint.color = androidx.compose.ui.graphics.lerp(color, color2, p).copy(alpha = 0.55f + 0.35f * p).toArgb()
        val r = corner.toPx().coerceAtMost(size.minDimension / 2)
        drawIntoCanvas { c ->
            c.nativeCanvas.drawRoundRect(0f, 0f, size.width, size.height, r, r, paint)
        }
    }
}

/** Leggero "respiro" di scala, abbinato al bagliore sui CTA. */
fun Modifier.breathe(max: Float = 1.03f, durationMs: Int = 1600): Modifier = composed {
    val s by rememberInfiniteTransition(label = "br").animateFloat(
        1f, max, infiniteRepeatable(tween(durationMs, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "s",
    )
    graphicsLayer { scaleX = s; scaleY = s }
}

/** Fascia di luce che attraversa in diagonale (shimmer) sui bottoni principali. */
fun Modifier.shimmer(corner: Dp, alpha: Float = 0.35f, durationMs: Int = 3200): Modifier = composed {
    val t by rememberInfiniteTransition(label = "sh").animateFloat(
        -0.5f, 1.6f, infiniteRepeatable(tween(durationMs, easing = FastOutSlowInEasing)), label = "t",
    )
    drawWithContent {
        drawContent()
        val r = corner.toPx().coerceAtMost(size.minDimension / 2)
        val clip = Path().apply { addRoundRect(RoundRect(0f, 0f, size.width, size.height, CornerRadius(r, r))) }
        clipPath(clip) {
            val x = size.width * t
            val band = size.width * 0.35f
            drawRect(
                Brush.linearGradient(
                    listOf(Color.Transparent, Color.White.copy(alpha = alpha), Color.Transparent),
                    start = Offset(x - band, 0f), end = Offset(x, size.height),
                ),
            )
        }
    }
}

/** Fluttua su e giù. */
fun Modifier.floatY(amplitude: Dp = 6.dp, durationMs: Int = 2200, delayMs: Int = 0): Modifier = composed {
    val y by rememberInfiniteTransition(label = "fy").animateFloat(
        0f, 1f, infiniteRepeatable(tween(durationMs, delayMs, FastOutSlowInEasing), RepeatMode.Reverse), label = "y",
    )
    graphicsLayer { translationY = -amplitude.toPx() * y }
}

/** Oscillazione tipo campanella (scossa rapida, poi pausa). */
fun Modifier.bellSwing(durationMs: Int = 2000): Modifier = composed {
    val a by rememberInfiniteTransition(label = "bell").animateFloat(
        0f, 0f,
        infiniteRepeatable(keyframes {
            this.durationMillis = durationMs
            0f at 0
            14f at (durationMs * 0.05f).toInt()
            -12f at (durationMs * 0.10f).toInt()
            8f at (durationMs * 0.15f).toInt()
            -6f at (durationMs * 0.20f).toInt()
            0f at (durationMs * 0.25f).toInt()
        }), label = "a",
    )
    graphicsLayer {
        rotationZ = a
        transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.5f, 0.1f)
    }
}

/** Piccola spinta orizzontale (chevron che "invita" a toccare). */
fun Modifier.nudgeX(distance: Dp = 3.dp, durationMs: Int = 1600): Modifier = composed {
    val x by rememberInfiniteTransition(label = "nx").animateFloat(
        0f, 1f, infiniteRepeatable(tween(durationMs / 2, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "x",
    )
    graphicsLayer { translationX = distance.toPx() * x }
}

/** Scintillio (twinkle): opacità e scala pulsanti. */
fun Modifier.twinkle(durationMs: Int = 2200, delayMs: Int = 0): Modifier = composed {
    val v by rememberInfiniteTransition(label = "tw").animateFloat(
        0f, 1f, infiniteRepeatable(tween(durationMs / 2, delayMs, FastOutSlowInEasing), RepeatMode.Reverse), label = "v",
    )
    graphicsLayer { alpha = 0.15f + 0.85f * v; val s = 0.8f + 0.5f * v; scaleX = s; scaleY = s }
}

/** Rotazione continua lenta (elementi che orbitano). */
fun Modifier.spin(durationMs: Int = 9000, reverse: Boolean = false): Modifier = composed {
    val r by rememberInfiniteTransition(label = "spin").animateFloat(
        0f, if (reverse) -360f else 360f, infiniteRepeatable(tween(durationMs, easing = LinearEasing)), label = "r",
    )
    graphicsLayer { rotationZ = r }
}

/**
 * Ingresso: dissolvenza + scivolata dal basso, con ritardo scalare (stagger)
 * in base a [index].
 */
fun Modifier.enter(index: Int = 0, stepMs: Long = 90, baseDelayMs: Long = 0, offset: Dp = 14.dp): Modifier = composed {
    val p = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay(baseDelayMs + index * stepMs)
        p.animateTo(1f, tween(550, easing = SoftOut))
    }
    graphicsLayer {
        alpha = p.value
        translationY = (1f - p.value) * offset.toPx()
    }
}

/** Entrata "pop" con rimbalzo (numeri, badge). */
fun Modifier.pop(delayMs: Long = 0, fromRotation: Float = 0f): Modifier = composed {
    val p = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay(delayMs)
        p.animateTo(1f, spring(dampingRatio = 0.42f, stiffness = Spring.StiffnessMediumLow))
    }
    graphicsLayer {
        alpha = p.value.coerceIn(0f, 1f)
        val s = 0.4f + 0.6f * p.value
        scaleX = s; scaleY = s
        rotationZ = fromRotation * (1f - p.value)
    }
}

/** Bordo tratteggiato (per gli obiettivi posticipati: si distinguono senza colpevolizzare). */
fun Modifier.dashedBorder(color: Color, corner: Dp, width: Dp = 1.dp): Modifier = drawBehind {
    val r = corner.toPx()
    drawRoundRect(
        color, cornerRadius = CornerRadius(r, r),
        style = Stroke(width.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f))),
    )
}
