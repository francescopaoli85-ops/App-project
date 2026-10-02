package com.francescopaoli.northstar.ui.fx

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

/** Numero che "conta" da 0 (o dal valore precedente) fino a [target]. */
@Composable
fun animatedInt(target: Int, durationMs: Int = 900, delayMs: Long = 0): Int {
    val a = remember { Animatable(0f) }
    LaunchedEffect(target) {
        delay(delayMs)
        a.animateTo(target.toFloat(), tween(durationMs, easing = SoftOut))
    }
    return a.value.roundToInt()
}
