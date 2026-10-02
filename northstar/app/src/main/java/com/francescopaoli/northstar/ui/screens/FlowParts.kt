package com.francescopaoli.northstar.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.francescopaoli.northstar.data.Area
import com.francescopaoli.northstar.ui.components.color
import com.francescopaoli.northstar.ui.components.hint
import com.francescopaoli.northstar.ui.components.icon
import com.francescopaoli.northstar.ui.fx.LocalFx
import com.francescopaoli.northstar.ui.fx.LocalSparks
import com.francescopaoli.northstar.ui.fx.animatedGradient
import com.francescopaoli.northstar.ui.fx.enter
import com.francescopaoli.northstar.ui.fx.pop
import com.francescopaoli.northstar.ui.theme.Neon
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

// ---------------- Preparazione ----------------

/**
 * Prima della prima domanda: area, modo di rispondere, musica e voce.
 * Tutto ben visibile e a un tocco; le scelte restano ricordate per la volta dopo.
 */
@Composable
fun PrepStep(
    area: Area,
    onArea: (Area) -> Unit,
    typing: Boolean,
    voiceAvailable: Boolean,
    onTyping: (Boolean) -> Unit,
    musicOn: Boolean,
    onMusic: (Boolean) -> Unit,
    voiceGuide: Boolean,
    onVoiceGuide: (Boolean) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Prepariamo il viaggio", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.enter(0))
        Text("Tre scelte veloci, poi 6 domande per trasformare un desiderio in una meta.",
            color = Neon.Text2, fontSize = 13.sp, lineHeight = 19.sp, modifier = Modifier.enter(1))

        PrepLabel("1 · Di che area è il tuo obiettivo?")
        // aree grandi, 2 per riga, ognuna col suo colore e un esempio
        Area.entries.chunked(2).forEachIndexed { r, row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEachIndexed { c, a ->
                    AreaTile(a, a == area, Modifier.weight(1f).pop((r * 2 + c) * 70L)) { onArea(a) }
                }
            }
        }

        PrepLabel("2 · Come vuoi rispondere?")
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ModeCard(
                "🎙", "Guidata a voce", "Ti leggo le domande e rispondi parlando",
                selected = !typing, enabled = voiceAvailable, modifier = Modifier.weight(1f),
            ) { onTyping(false) }
            ModeCard(
                "✍", "Manuale", "Leggi e scrivi tu, con calma",
                selected = typing, enabled = true, modifier = Modifier.weight(1f),
            ) { onTyping(true) }
        }
        if (!voiceAvailable) Text("Il riconoscimento vocale non è disponibile su questo telefono.", color = Neon.Text3, fontSize = 11.sp)

        PrepLabel("3 · Audio")
        PrepSwitch("♪  Musica di sottofondo", "cresce a ogni risposta", musicOn, onMusic)
        PrepSwitch("🗣  Voce guida", "legge ad alta voce le domande", voiceGuide, onVoiceGuide)
    }
}

@Composable
private fun PrepLabel(t: String) =
    Text(t, color = Neon.Lilac, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 12.dp))

@Composable
private fun AreaTile(a: Area, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(18.dp)
    Column(
        modifier
            .clip(shape)
            .background(if (selected) a.color.copy(alpha = 0.22f) else Neon.Surface.copy(alpha = 0.8f))
            .border(if (selected) 2.dp else 1.dp, if (selected) a.color else Neon.Violet.copy(alpha = 0.25f), shape)
            .clickable(role = Role.RadioButton, onClickLabel = a.label, onClick = onClick)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            Modifier.size(36.dp).clip(RoundedCornerShape(11.dp)).background(a.color.copy(alpha = if (selected) 0.35f else 0.16f)),
            contentAlignment = Alignment.Center,
        ) { Icon(a.icon, null, tint = a.color, modifier = Modifier.size(20.dp)) }
        Text(a.label, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        Text(a.hint, color = Neon.Text2, fontSize = 11.sp, lineHeight = 14.sp)
    }
}

@Composable
private fun ModeCard(
    emoji: String, title: String, sub: String, selected: Boolean, enabled: Boolean, modifier: Modifier, onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(18.dp)
    Column(
        modifier
            .clip(shape)
            .then(if (selected) Modifier.animatedGradient(18.dp) else Modifier.background(Neon.Surface.copy(alpha = 0.8f)))
            .border(1.dp, if (selected) Color.Transparent else Neon.Violet.copy(alpha = 0.25f), shape)
            .clickable(enabled = enabled, role = Role.RadioButton, onClickLabel = title, onClick = onClick)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(emoji, fontSize = 22.sp, color = if (enabled) Color.White else Neon.Text3)
        Text(title, color = if (selected) Neon.OnAccent else if (enabled) Color.White else Neon.Text3, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        Text(sub, color = if (selected) Neon.OnAccent.copy(alpha = 0.85f) else Neon.Text2, fontSize = 11.5.sp, lineHeight = 15.sp)
    }
}

@Composable
private fun PrepSwitch(title: String, sub: String, on: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Neon.Surface.copy(alpha = 0.7f))
            .clickable(role = Role.Switch) { onChange(!on) }.padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, color = Neon.Text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text(sub, color = Neon.Text3, fontSize = 11.sp)
        }
        androidx.compose.material3.Switch(
            on, onChange,
            colors = androidx.compose.material3.SwitchDefaults.colors(
                checkedTrackColor = Neon.Violet, checkedThumbColor = Color.White,
                uncheckedTrackColor = Neon.Track, uncheckedThumbColor = Neon.Text3, uncheckedBorderColor = Neon.Track,
            ),
        )
    }
}

// ---------------- Modo di risposta durante le domande ----------------

/** Selettore grande e chiaro: rispondere a voce o scrivere (si cambia in qualsiasi momento). */
@Composable
fun AnswerModeSwitch(typing: Boolean, voiceAvailable: Boolean, onTyping: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Neon.Track.copy(alpha = 0.8f)).padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        listOf(false to "🎙  Rispondo a voce", true to "✍  Scrivo io").forEach { (t, label) ->
            val on = t == typing
            val enabled = t || voiceAvailable
            Text(
                label, color = if (on) Neon.OnAccent else if (enabled) Neon.Text2 else Neon.Text3,
                fontSize = 13.sp, fontWeight = FontWeight.Bold, textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.weight(1f).clip(RoundedCornerShape(11.dp))
                    .then(if (on) Modifier.animatedGradient(11.dp) else Modifier)
                    .clickable(enabled = enabled, role = Role.RadioButton) { onTyping(t) }
                    .padding(vertical = 10.dp),
            )
        }
    }
}

// ---------------- Costellazione dei passi ----------------

/**
 * L'avanzamento del percorso come una costellazione: 6 stelle (le domande) e la Stella Polare (il riepilogo).
 * A ogni passo la nuova stella si accende con un lampo, un anello che si allarga e scintille:
 * si vede che stai progredendo. Al riepilogo la Stella Polare si accende d'oro.
 */
@Composable
fun ConstellationProgress(step: Int, modifier: Modifier = Modifier) {
    val n = 7 // 6 domande + la Stella Polare
    val ys = floatArrayOf(0.62f, 0.30f, 0.55f, 0.25f, 0.60f, 0.35f, 0.45f)
    val ignite = remember { Animatable(1f) }
    var lastStep by remember { mutableStateOf(step) }
    var igniteIndex by remember { mutableStateOf(-1) }
    var bounds by remember { mutableStateOf(androidx.compose.ui.geometry.Rect.Zero) }
    val sparks = LocalSparks.current
    LaunchedEffect(step) {
        if (step > lastStep) {
            igniteIndex = step - 1
            // scintille dalla stella appena accesa (coordinate dello schermo)
            if (bounds != androidx.compose.ui.geometry.Rect.Zero) {
                val i = (step - 1).coerceIn(0, n - 1)
                val x = bounds.left + bounds.width * (0.04f + 0.92f * i / (n - 1))
                val y = bounds.top + bounds.height * ys[i]
                sparks?.emit(Offset(x, y), big = step >= n - 1)
            }
            ignite.snapTo(0f)
            ignite.animateTo(1f, tween(1100, easing = FastOutSlowInEasing))
        }
        lastStep = step
    }
    val pulse = if (LocalFx.current.animated) rememberInfiniteTransition(label = "c").animateFloat(
        0f, 1f, infiniteRepeatable(tween(1400), RepeatMode.Reverse), label = "p",
    ).value else 0.5f
    val gold = Color(0xFFFFD76A)
    Canvas(modifier.fillMaxWidth().height(46.dp).onGloballyPositioned { bounds = it.boundsInRoot() }) {
        val pts = List(n) { i -> Offset(size.width * (0.04f + 0.92f * i / (n - 1)), size.height * ys[i]) }
        // linee: piene fino al passo fatto, tratteggiate dopo
        for (i in 1 until n) {
            val lit = i <= step
            val a = pts[i - 1]
            val b = if (lit && i == step && ignite.value < 1f) a + (pts[i] - a) * ignite.value else pts[i]
            if (lit) drawLine(Neon.Lilac.copy(alpha = 0.8f), a, b, 1.6.dp.toPx())
            else drawLine(Neon.Inactive.copy(alpha = 0.6f), pts[i - 1], pts[i], 1.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 6f)))
        }
        pts.forEachIndexed { i, p ->
            val isPolar = i == n - 1
            val lit = i < step || (isPolar && step >= n - 1)
            val current = i == step && !isPolar
            val r = (if (isPolar) 8.dp else 4.dp).toPx()
            if (lit) {
                val glowC = if (isPolar) gold else Neon.Cyan
                drawCircle(Brush.radialGradient(listOf(glowC.copy(alpha = 0.55f), Color.Transparent), p, r * 3.2f), r * 3.2f, p)
            }
            // lampo e anello che si allarga sulla stella appena accesa
            if (i == igniteIndex && ignite.value < 1f) {
                val k = ignite.value
                drawCircle((if (isPolar) gold else Neon.Cyan).copy(alpha = (1f - k) * 0.9f), r + 26.dp.toPx() * k, p, style = Stroke(2.dp.toPx()))
                drawCircle(Color.White.copy(alpha = (1f - k) * 0.6f), r * (1f + 2f * (1f - k)), p)
            }
            if (current) drawCircle(Neon.Cyan.copy(alpha = 0.25f + 0.25f * pulse), r * 2.4f, p, style = Stroke(1.5.dp.toPx()))
            val color = when {
                isPolar && lit -> gold
                isPolar -> Neon.Inactive
                lit -> Color.White
                current -> Neon.Cyan
                else -> Neon.Inactive
            }
            if (isPolar) drawStar(p, r * (if (lit) 1.2f + 0.15f * pulse else 1f), color)
            else drawCircle(color, if (lit) r else r * 0.7f, p)
        }
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

/** Lampo di luce dall'alto a ogni passo avanti: una carezza visiva, non un'esplosione. */
@Composable
fun StepFlash(step: Int, modifier: Modifier = Modifier) {
    val a = remember { Animatable(0f) }
    var last by remember { mutableStateOf(step) }
    LaunchedEffect(step) {
        if (step > last) {
            a.snapTo(0.55f)
            a.animateTo(0f, tween(900))
        }
        last = step
    }
    if (a.value > 0f) Canvas(modifier) {
        drawRect(Brush.radialGradient(
            listOf(Neon.Cyan.copy(alpha = a.value * 0.5f), Neon.Violet.copy(alpha = a.value * 0.25f), Color.Transparent),
            Offset(size.width / 2, 0f), size.width * 1.1f,
        ))
    }
}
