package com.francescopaoli.northstar.ui.components

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import com.francescopaoli.northstar.ui.fx.LocalSparks
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.francescopaoli.northstar.ui.fx.flicker
import com.francescopaoli.northstar.ui.fx.pop
import com.francescopaoli.northstar.ui.theme.Neon

private val FlameBrush = Brush.verticalGradient(listOf(Color(0xFFFFC107), Color(0xFFFF6B3D), Neon.Violet))

/** Fiammella della serie: colorata e tremolante se attiva, spenta se la serie è 0. */
@Composable
fun FlameIcon(size: Dp, lit: Boolean, modifier: Modifier = Modifier) {
    Icon(
        NsIcons.Flame, null,
        tint = if (lit) Color.White else Neon.Inactive,
        modifier = modifier
            .size(size)
            .then(
                if (!lit) Modifier
                else Modifier
                    .flicker()
                    // gradiente giallo → arancio → viola applicato solo sulla forma dell'icona
                    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                    .drawWithContent {
                        drawContent()
                        drawRect(FlameBrush, blendMode = BlendMode.SrcIn)
                    },
            ),
    )
}

/** Riga del passo della settimana: spunta rapida senza aprire il dettaglio. */
@Composable
fun WeeklyStepRow(
    goalTitle: String,
    step: String?,
    done: Boolean,
    onToggle: () -> Unit,
    onPick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val box by animateColorAsState(if (done) Neon.Cyan else Color.Transparent, label = "c")
    val sparks = LocalSparks.current
    var boxCenter by remember { mutableStateOf(Offset.Zero) }
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(
                role = if (step != null) Role.Checkbox else Role.Button,
                onClick = if (step == null) onPick else { { if (!done) sparks?.emit(boxCenter, big = true); onToggle() } },
            )
            .padding(vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(11.dp),
    ) {
        if (step != null) {
            Box(
                Modifier.size(22.dp).onGloballyPositioned { boxCenter = it.boundsInRoot().center }
                    .clip(RoundedCornerShape(7.dp)).background(box)
                    .border(1.5.dp, Neon.Cyan, RoundedCornerShape(7.dp)),
                contentAlignment = Alignment.Center,
            ) { if (done) Icon(NsIcons.Check, null, tint = Neon.Night, modifier = Modifier.size(14.dp).pop()) }
        } else {
            Box(
                Modifier.size(22.dp).clip(RoundedCornerShape(7.dp)).background(Neon.Violet.copy(alpha = 0.25f)),
                contentAlignment = Alignment.Center,
            ) { Icon(NsIcons.Plus, null, tint = Neon.Lilac, modifier = Modifier.size(13.dp)) }
        }
        Column(Modifier.weight(1f)) {
            Text(
                step ?: "Scegli un piccolo passo",
                color = when { done -> Neon.Text3; step == null -> Neon.Lilac; else -> Neon.Text },
                fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis,
                textDecoration = if (done) TextDecoration.LineThrough else null,
            )
            Text(goalTitle, color = Neon.Text3, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}
