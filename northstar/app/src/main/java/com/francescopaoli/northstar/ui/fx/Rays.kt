package com.francescopaoli.northstar.ui.fx

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.francescopaoli.northstar.ui.theme.Neon

/** Raggi di luce che ruotano piano dietro la stellina: invita a toccarla. */
@Composable
fun StarRays(modifier: Modifier, alpha: Float = 0.45f) {
    androidx.compose.foundation.Canvas(modifier.spin(14000)) {
        val c = center
        repeat(8) { i ->
            val a = i * 0.785f
            val len = size.minDimension * if (i % 2 == 0) 0.5f else 0.36f
            drawLine(
                androidx.compose.ui.graphics.Brush.linearGradient(
                    listOf(Neon.Lilac.copy(alpha = alpha), androidx.compose.ui.graphics.Color.Transparent),
                    c, androidx.compose.ui.geometry.Offset(c.x + kotlin.math.cos(a) * len, c.y + kotlin.math.sin(a) * len),
                ),
                c, androidx.compose.ui.geometry.Offset(c.x + kotlin.math.cos(a) * len, c.y + kotlin.math.sin(a) * len),
                strokeWidth = 2.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round,
            )
        }
    }
}

