package com.francescopaoli.northstar.ui.components

import android.os.Build
import androidx.compose.ui.graphics.Shape
import com.francescopaoli.northstar.ui.fx.LocalFx
import com.francescopaoli.northstar.ui.fx.LocalHaze
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeChild
import kotlin.math.roundToInt
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.francescopaoli.northstar.ui.fx.SoftOut
import com.francescopaoli.northstar.ui.fx.animatedGradient
import com.francescopaoli.northstar.ui.fx.breathe
import com.francescopaoli.northstar.ui.fx.dashedBorder
import com.francescopaoli.northstar.ui.fx.floatY
import com.francescopaoli.northstar.ui.fx.glow
import com.francescopaoli.northstar.ui.fx.shimmer
import com.francescopaoli.northstar.ui.theme.Neon

/** Bordo "di vetro": più luminoso in alto a sinistra, come se prendesse luce. */
val GlassEdge = Brush.linearGradient(listOf(Color.White.copy(alpha = 0.18f), Neon.Violet.copy(alpha = 0.34f), Neon.Violet.copy(alpha = 0.10f)))

/**
 * Vetro smerigliato: sfoca davvero la nebulosa che c'è dietro (Android 12+).
 * Con effetti ridotti o Android più vecchi: superficie scura piena come prima.
 */
@Composable
fun Modifier.glass(shape: Shape, tint: Color = Neon.Surface.copy(alpha = 0.72f), solid: Color = Neon.Surface): Modifier {
    val haze = LocalHaze.current
    return if (haze != null && LocalFx.current.full && Build.VERSION.SDK_INT >= 31) {
        this.clip(shape).hazeChild(
            haze,
            HazeStyle(backgroundColor = Neon.Night, tint = HazeTint(tint), blurRadius = 26.dp, noiseFactor = 0.05f),
        )
    } else this.clip(shape).background(solid)
}

/** Effetto pressione: il bottone si "schiaccia" un attimo. */
@Composable
private fun pressScale(source: MutableInteractionSource): Float {
    val pressed by source.collectIsPressedAsState()
    return if (pressed) 0.96f else 1f
}

/** CTA principale: gradiente in movimento + bagliore pulsante + shimmer. */
@Composable
fun GradientButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    corner: Dp = 14.dp,
    trailing: ImageVector? = null,
    glowing: Boolean = true,
) {
    val src = remember { MutableInteractionSource() }
    Box(
        modifier
            .scale(pressScale(src))
            .then(if (enabled && glowing) Modifier.breathe(1.025f).glow(corner) else Modifier)
            .clip(RoundedCornerShape(corner))
            .then(if (enabled) Modifier.animatedGradient(corner) else Modifier.background(Neon.Track))
            .shimmer(corner, if (enabled) 0.3f else 0f)
            .clickable(src, null, enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 15.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(text, color = if (enabled) Color.White else Neon.Text3, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            if (trailing != null) Icon(trailing, null, tint = Color.White, modifier = Modifier.size(17.dp))
        }
    }
}

/** Bottone secondario a contorno viola. */
@Composable
fun GhostButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val src = remember { MutableInteractionSource() }
    Box(
        modifier
            .scale(pressScale(src))
            .clip(RoundedCornerShape(14.dp))
            .border(1.5.dp, Neon.Violet.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
            .clickable(src, null, role = Role.Button, onClick = onClick)
            .padding(14.dp),
        contentAlignment = Alignment.Center,
    ) { Text(text, color = Neon.Text2, fontWeight = FontWeight.SemiBold, fontSize = 13.5.sp) }
}

/** Link testuale discreto ("Più tardi", "Usa un'altra email"...). */
@Composable
fun TextLink(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, color: Color = Neon.Link) {
    Text(
        text, color = color, fontSize = 13.sp, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center,
        modifier = modifier.clip(RoundedCornerShape(8.dp)).clickable(onClick = onClick).padding(10.dp),
    )
}

/** Card scura con bordo viola sottile (tratteggiato per i posticipati). */
@Composable
fun NeonCard(
    modifier: Modifier = Modifier,
    dashed: Boolean = false,
    corner: Dp = 17.dp,
    onClick: (() -> Unit)? = null,
    padding: Dp = 15.dp,
    content: @Composable RowScope.() -> Unit,
) {
    val shape = RoundedCornerShape(corner)
    Row(
        modifier
            .glass(shape)
            .then(
                if (dashed) Modifier.dashedBorder(Neon.Text2.copy(alpha = 0.35f), corner)
                else Modifier.border(1.dp, GlassEdge, shape),
            )
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(padding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(13.dp),
        content = content,
    )
}

@Composable
fun NeonColumnCard(modifier: Modifier = Modifier, corner: Dp = 18.dp, content: @Composable ColumnScope.() -> Unit) {
    val shape = RoundedCornerShape(corner)
    Column(
        modifier.glass(shape).border(1.dp, GlassEdge, shape).padding(18.dp),
        content = content,
    )
}

/**
 * Anello di progresso con gradiente viola→ciano.
 * All'apertura si disegna progressivamente da 0 al valore.
 */
@Composable
fun ProgressRing(
    progress: Float,
    size: Dp,
    stroke: Dp,
    modifier: Modifier = Modifier,
    muted: Boolean = false,
    label: String? = null,
    labelSize: TextUnit = 16.sp,
    delayMs: Int = 0,
    glowing: Boolean = false,
    showPercent: Boolean = false,
) {
    val anim = remember { Animatable(0f) }
    // bagliore pulsante sul solo tratto colorato (drop-shadow del mockup), mai sul centro
    val pulse by androidx.compose.animation.core.rememberInfiniteTransition(label = "ring").animateFloat(
        0f, 1f,
        androidx.compose.animation.core.infiniteRepeatable(
            tween(1200, easing = androidx.compose.animation.core.FastOutSlowInEasing),
            androidx.compose.animation.core.RepeatMode.Reverse,
        ), label = "p",
    )
    LaunchedEffect(progress) { anim.animateTo(progress, tween(1100, delayMs, SoftOut)) }
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(size)) {
            val sw = stroke.toPx()
            val inset = sw / 2
            val arcSize = Size(this.size.width - sw, this.size.height - sw)
            drawArc(Neon.Track, 0f, 360f, false, Offset(inset, inset), arcSize, style = Stroke(sw))
            val brush = if (muted) Brush.linearGradient(listOf(Neon.Postponed, Neon.Postponed))
            else Brush.linearGradient(listOf(Neon.Violet, Neon.Cyan), Offset.Zero, Offset(this.size.width, this.size.height))
            // piccolo minimo visibile, così anche lo 0% ha un "seme" di colore
            val sweep = 360f * anim.value.coerceAtLeast(0.02f)
            if (glowing && !muted) {
                drawArc(brush, -90f, sweep, false, Offset(inset, inset), arcSize, alpha = 0.15f + 0.25f * pulse,
                    style = Stroke(sw * (1.8f + 0.8f * pulse), cap = StrokeCap.Round))
            }
            drawArc(brush, -90f, sweep, false, Offset(inset, inset), arcSize, style = Stroke(sw, cap = StrokeCap.Round))
        }
        val text = label ?: if (showPercent) "${(anim.value * 100).roundToInt()}%" else null
        if (text != null) Text(text, color = Color.White, fontWeight = FontWeight.Bold, fontSize = labelSize)
    }
}

/** Riquadro con gradiente animato e icona bianca (icone "hero"). */
@Composable
fun GradientIconTile(
    icon: ImageVector,
    size: Dp,
    corner: Dp,
    iconSize: Dp,
    modifier: Modifier = Modifier,
    floating: Boolean = true,
    iconModifier: Modifier = Modifier,
) {
    Box(
        modifier
            .then(if (floating) Modifier.floatY(6.dp, 1800) else Modifier)
            .size(size)
            .glow(corner, blur = 18.dp, durationMs = 1800)
            .clip(RoundedCornerShape(corner))
            .animatedGradient(corner),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, null, tint = Color.White, modifier = iconModifier.size(iconSize)) }
}

/** Etichetta del criterio / categoria. */
@Composable
fun Chip(text: String, modifier: Modifier = Modifier, filled: Boolean = false) {
    Text(
        text.uppercase(),
        color = if (filled) Color(0xFF17102E) else Neon.Lilac,
        style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (filled) Neon.Lilac else Neon.Violet.copy(alpha = 0.18f))
            .padding(horizontal = 10.dp, vertical = 4.dp),
    )
}

/** Bottone rotondo semitrasparente (chiudi / indietro). */
@Composable
fun RoundIconButton(icon: ImageVector, description: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.08f))
            .clickable(onClickLabel = description, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, description, tint = Neon.Text, modifier = Modifier.size(15.dp)) }
}

enum class Tab { HOME, TRAGUARDI, IMPOSTAZIONI }

@Composable
fun BottomNav(selected: Tab, onSelect: (Tab) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .glass(RoundedCornerShape(0.dp), tint = Neon.NavBar.copy(alpha = 0.7f), solid = Neon.NavBar)
            .border(width = 1.dp, color = Neon.Violet.copy(alpha = 0.22f), shape = RoundedCornerShape(0.dp))
            .navigationBarsPadding()
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceAround,
    ) {
        listOf(
            Triple(Tab.HOME, NsIcons.Home, "Home"),
            Triple(Tab.TRAGUARDI, NsIcons.Award, "Traguardi"),
            Triple(Tab.IMPOSTAZIONI, NsIcons.Settings, "Impostazioni"),
        ).forEach { (tab, icon, label) ->
            val on = tab == selected
            Column(
                Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(role = Role.Tab) { onSelect(tab) }
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Icon(
                    icon, null, tint = if (on) Neon.Lilac else Neon.Inactive,
                    modifier = Modifier.size(20.dp).then(if (on) Modifier.floatY(2.dp, 2000) else Modifier),
                )
                Text(label, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (on) Neon.Lilac else Neon.Inactive)
            }
        }
    }
}
