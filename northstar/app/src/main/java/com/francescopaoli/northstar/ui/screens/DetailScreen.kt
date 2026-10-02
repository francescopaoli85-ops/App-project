package com.francescopaoli.northstar.ui.screens

import com.francescopaoli.northstar.ui.fx.LocalSparks
import com.francescopaoli.northstar.ui.fx.sharedElementOf
import com.francescopaoli.northstar.ui.fx.sharedTextOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import com.francescopaoli.northstar.ads.NativeAdCard
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.francescopaoli.northstar.data.Criterion
import com.francescopaoli.northstar.data.Goal
import com.francescopaoli.northstar.data.GoalAction
import com.francescopaoli.northstar.data.GoalStatus
import com.francescopaoli.northstar.domain.Checkins
import com.francescopaoli.northstar.domain.SummaryBuilder
import com.francescopaoli.northstar.ui.MainViewModel
import com.francescopaoli.northstar.ui.components.icon
import com.francescopaoli.northstar.ui.components.color
import com.francescopaoli.northstar.ui.components.StarTrail
import com.francescopaoli.northstar.ui.components.Chip
import com.francescopaoli.northstar.ui.components.GhostButton
import com.francescopaoli.northstar.ui.components.GradientButton
import com.francescopaoli.northstar.ui.components.NeonCard
import com.francescopaoli.northstar.ui.components.NeonColumnCard
import com.francescopaoli.northstar.ui.components.NsIcons
import com.francescopaoli.northstar.ui.components.ProgressRing
import com.francescopaoli.northstar.ui.components.RoundIconButton
import com.francescopaoli.northstar.ui.components.TextLink
import com.francescopaoli.northstar.ui.fx.Blob
import com.francescopaoli.northstar.ui.fx.NeonBackdrop
import com.francescopaoli.northstar.ui.fx.bellSwing
import com.francescopaoli.northstar.ui.fx.enter
import com.francescopaoli.northstar.ui.fx.glow
import com.francescopaoli.northstar.ui.fx.nudgeX
import com.francescopaoli.northstar.ui.fx.pop
import com.francescopaoli.northstar.ui.theme.Neon

@Composable
fun DetailScreen(
    vm: MainViewModel,
    id: String,
    onBack: () -> Unit,
    onCheckin: () -> Unit,
    onAchieved: () -> Unit,
) {
    val goals by vm.goals.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    val showAds by vm.showAds.collectAsStateWithLifecycle()
    val g = goals.firstOrNull { it.id == id } ?: run { NeonBackdrop { }; return }
    var pickDate by remember { mutableStateOf(false) }
    // posticipa in un tocco: scelte rapide, il calendario solo se serve
    var quickPostpone by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var confirmAchieve by remember { mutableStateOf(false) }
    val postponed = g.status == GoalStatus.POSTPONED

    var menu by remember { mutableStateOf(false) }
    var showAnswers by remember { mutableStateOf(false) }
    var adding by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<GoalAction?>(null) }

    // Dettaglio essenziale: titolo, frase, sentiero, azioni. Il resto è nel menu ⋯
    NeonBackdrop(particles = 6, seed = 4) {
        Column(Modifier.fillMaxSize().imePadding()) {
            Row(
                Modifier.fillMaxWidth().statusBarsPadding().padding(start = 18.dp, end = 10.dp, top = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RoundIconButton(NsIcons.Back, "Indietro", onBack)
                Spacer(Modifier.weight(1f))
                Box {
                    Text(
                        "⋯", color = Neon.Text, fontSize = 26.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier.clip(CircleShape).clickable(onClickLabel = "Altre azioni") { menu = true }.padding(horizontal = 14.dp, vertical = 2.dp),
                    )
                    androidx.compose.material3.DropdownMenu(menu, { menu = false }, containerColor = Neon.SurfaceHi) {
                        if (g.isOpen) {
                            MenuItem("📅  Posticipa") { menu = false; quickPostpone = true }
                            MenuItem("✓  Raggiunto") { menu = false; confirmAchieve = true }
                        }
                        MenuItem("💬  Le tue 6 risposte") { menu = false; showAnswers = true }
                        if (g.isOpen) MenuItem("🔔  Check-in adesso") { menu = false; onCheckin() }
                        MenuItem("Elimina", Color(0xFFFF8FA3)) { menu = false; confirmDelete = true }
                    }
                }
            }

            Column(
                Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                AreaGlyph(g.area, Modifier.size(44.dp, 26.dp))
                Text(
                    "${g.area.label.uppercase()} · " + if (postponed) "NUOVA DATA ${SummaryBuilder.formatDate(g.deadline).uppercase()}"
                    else "SCADE IL ${SummaryBuilder.formatDate(g.deadline).uppercase()}",
                    color = g.area.color, style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
                )
                Text(g.title, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, lineHeight = 29.sp,
                    modifier = Modifier.sharedTextOf("title-${g.id}"))
                if (g.summary.isNotBlank()) Text("“${g.summary}”", color = Neon.Text2, fontSize = 14.sp, lineHeight = 21.sp, fontStyle = FontStyle.Italic)

                // alla scadenza l'app chiede se è stato raggiunto
                if (g.isDue()) DueBanner(onYes = { vm.achieve(g.id); onAchieved() }, onLater = { quickPostpone = true })

                StarTrail(g.actions.count { it.done }, g.actions.size, Modifier.padding(vertical = 6.dp), height = 26.dp)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    g.actions.forEachIndexed { i, a ->
                        ActionRow(a, Modifier.enter(i, 50), { vm.toggleAction(g.id, a.id) }, { editing = a }, { vm.removeAction(g.id, a.id) })
                    }
                    if (g.isOpen) Text(
                        "+  Aggiungi azione", color = Neon.Lilac, fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).clickable { adding = true }.padding(vertical = 10.dp),
                    )
                }
            }
            if (g.actions.isNotEmpty()) CoachBubble(
                "detail_longpress", "Tieni premuta un'azione per modificarla o eliminarla.",
                settings.seenHints, { vm.hintSeen(it) }, Modifier.navigationBarsPadding().padding(16.dp),
            )
        }
    }

    if (adding) QuickTextDialog(
        title = "Nuova azione", placeholder = "Es. 20 minuti di corsa domani", confirm = "Aggiungi",
        onConfirm = { vm.addAction(g.id, it); adding = false }, onDismiss = { adding = false },
    )
    editing?.let { a ->
        QuickTextDialog(
            title = "Modifica azione", initial = a.text, placeholder = "Azione", confirm = "Salva",
            onConfirm = { vm.editAction(g.id, a.id, it); editing = null }, onDismiss = { editing = null },
        )
    }
    if (showAnswers) AlertDialog(
        onDismissRequest = { showAnswers = false },
        containerColor = Neon.Surface,
        title = { Text("Le tue 6 risposte", color = Neon.Text, fontWeight = FontWeight.Bold) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Criterion.entries.forEach { c ->
                    Text(c.label.uppercase(), color = Neon.Lilac, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    val text = if (c == Criterion.CONTESTUALIZZATO)
                        listOf(SummaryBuilder.formatDate(g.deadline), g.answers[c].orEmpty()).filter { it.isNotBlank() }.joinToString(" · ")
                    else g.answers[c].orEmpty().ifBlank { "—" }
                    Text(text, color = Neon.TextMid, fontSize = 13.sp, lineHeight = 19.sp)
                }
            }
        },
        confirmButton = { TextButton({ showAnswers = false }) { Text("Chiudi", color = Neon.Cyan) } },
    )

    if (quickPostpone) AlertDialog(
        onDismissRequest = { quickPostpone = false },
        containerColor = Neon.Surface,
        title = { Text("Sposta la scadenza", color = Neon.Text, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Adesso: ${SummaryBuilder.formatDate(g.deadline)}. Va benissimo prendersi più tempo.", color = Neon.Text2, fontSize = 13.sp)
                QuickDates(
                    g.deadline,
                    onPick = { vm.postpone(g.id, it); quickPostpone = false },
                    onCustom = { quickPostpone = false; pickDate = true },
                )
            }
        },
        confirmButton = {},
        dismissButton = { androidx.compose.material3.TextButton({ quickPostpone = false }) { Text("Annulla", color = Neon.Text2) } },
    )
    if (pickDate) NeonDatePicker(
        initial = maxOf(g.deadline, java.time.LocalDate.now()).plusWeeks(2),
        onPick = { vm.postpone(g.id, it); pickDate = false },
        onDismiss = { pickDate = false },
    )
    if (confirmAchieve) AlertDialog(
        onDismissRequest = { confirmAchieve = false },
        containerColor = Neon.Surface,
        title = { Text("L'hai davvero raggiunto?") },
        text = { Text("\"${g.title}\" passerà nei Traguardi.", color = Neon.Text2) },
        confirmButton = {
            TextButton({ confirmAchieve = false; vm.achieve(g.id); onAchieved() }) { Text("Sì, raggiunto!", color = Neon.Cyan) }
        },
        dismissButton = { TextButton({ confirmAchieve = false }) { Text("Non ancora", color = Neon.Text2) } },
    )
    if (confirmDelete) AlertDialog(
        onDismissRequest = { confirmDelete = false },
        containerColor = Neon.Surface,
        title = { Text("Eliminare l'obiettivo?") },
        text = { Text("Non si può annullare.", color = Neon.Text2) },
        confirmButton = { TextButton({ vm.delete(g.id); confirmDelete = false; onBack() }) { Text("Elimina", color = Neon.Lilac) } },
        dismissButton = { TextButton({ confirmDelete = false }) { Text("Annulla", color = Neon.Text2) } },
    )
}

@Composable
private fun DueBanner(onYes: () -> Unit, onLater: () -> Unit) {
    NeonColumnCard(Modifier.fillMaxWidth().pop().glow(18.dp, blur = 18.dp)) {
        Text("È arrivato il giorno ✦", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold)
        Text("L'hai raggiunto? Se serve altro tempo va benissimo: scegli una nuova data.", color = Neon.Text2,
            fontSize = 12.5.sp, lineHeight = 19.sp, modifier = Modifier.padding(top = 6.dp, bottom = 14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            GhostButton("Serve più tempo", onLater, Modifier.weight(1f))
            GradientButton("Sì, fatto!", onYes, Modifier.weight(1f))
        }
    }
}


@Composable
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
private fun ActionRow(a: GoalAction, modifier: Modifier, onToggle: () -> Unit, onEdit: () -> Unit, onRemove: () -> Unit) {
    val box by animateColorAsState(if (a.done) Neon.Cyan else Color.Transparent, label = "c")
    val sparks = LocalSparks.current
    var boxCenter by remember { mutableStateOf(Offset.Zero) }
    var menu by remember { mutableStateOf(false) }
    Box {
    Row(
        modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
            // tocco = spunta (con scintille), pressione lunga = modifica/elimina
            .combinedClickable(
                role = Role.Checkbox,
                onLongClickLabel = "Modifica o elimina",
                onLongClick = { menu = true },
            ) { if (!a.done) sparks?.emit(boxCenter, big = true); onToggle() }
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            Modifier.size(20.dp).onGloballyPositioned { boxCenter = it.boundsInRoot().center }
                .clip(RoundedCornerShape(6.dp)).background(box).border(1.5.dp, Neon.Cyan, RoundedCornerShape(6.dp)),
            contentAlignment = Alignment.Center,
        ) { if (a.done) Icon(NsIcons.Check, null, tint = Neon.Night, modifier = Modifier.size(13.dp).pop()) }
        Text(
            a.text, color = if (a.done) Neon.Text3 else Neon.TextMid, fontSize = 13.sp, modifier = Modifier.weight(1f),
            textDecoration = if (a.done) TextDecoration.LineThrough else null,
        )
    }
    androidx.compose.material3.DropdownMenu(menu, { menu = false }, containerColor = Neon.SurfaceHi) {
        androidx.compose.material3.DropdownMenuItem(
            text = { Text("Modifica", color = Neon.Text) }, onClick = { menu = false; onEdit() },
        )
        androidx.compose.material3.DropdownMenuItem(
            text = { Text("Elimina", color = Color(0xFFFF8FA3)) },
            leadingIcon = { Icon(NsIcons.Trash, null, tint = Color(0xFFFF8FA3), modifier = Modifier.size(15.dp)) },
            onClick = { menu = false; onRemove() },
        )
    }
    }
}



@Composable
private fun MenuItem(text: String, color: Color = Neon.Text, onClick: () -> Unit) =
    androidx.compose.material3.DropdownMenuItem(text = { Text(text, color = color, fontSize = 15.sp) }, onClick = onClick)
