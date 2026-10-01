package com.francescopaoli.northstar.ui.fx

import android.graphics.RenderEffect
import android.os.Build
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/**
 * Onda d'urto: un anello che si allarga dal punto [cx],[cy] (frazioni dello schermo)
 * e deforma per un attimo tutto quello che attraversa. Android 13+, altrimenti nulla.
 */
fun Modifier.shockwave(cx: Float = 0.5f, cy: Float = 0.38f, delayMs: Long = 120, durationMs: Int = 1100): Modifier = composed {
    if (Build.VERSION.SDK_INT < 33 || !LocalFx.current.full || !LocalFx.current.shaders) return@composed this
    val shader = remember { Shaders.create(Shaders.SHOCKWAVE) } ?: return@composed this
    val progress = remember { Animatable(0f) }
    var size by remember { mutableStateOf(IntSize.Zero) }
    val density = LocalDensity.current
    LaunchedEffect(Unit) {
        delay(delayMs)
        progress.animateTo(1f, tween(durationMs, easing = SoftOut))
    }
    this
        .onSizeChanged { size = it }
        .graphicsLayer {
            val p = progress.value
            if (p <= 0f || p >= 1f || size == IntSize.Zero) { renderEffect = null; return@graphicsLayer }
            val maxR = maxOf(size.width, size.height) * 1.1f
            shader.setFloatUniform("center", size.width * cx, size.height * cy)
            shader.setFloatUniform("radius", maxR * p)
            shader.setFloatUniform("width", with(density) { (60.dp + 40.dp * p).toPx() })
            shader.setFloatUniform("strength", with(density) { 36.dp.toPx() } * (1f - p))
            renderEffect = RenderEffect.createRuntimeShaderEffect(shader, "content").asComposeRenderEffect()
        }
}
