package com.francescopaoli.northstar.ui.screens

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
    val g = goals.firstOrNull { it.id == id } ?: run { NeonBackdrop { }; return }
    var pickDate by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    val postponed = g.status == GoalStatus.POSTPONED

    NeonBackdrop(particles = 6, seed = 4) {
        Column(Modifier.fillMaxSize().imePadding()) {
            HeaderBand(blob = Blob(1f, 1f, 0.5f, Neon.Cyan, 0.25f)) {
                TopRow(
                    start = { RoundIconButton(NsIcons.Back, "Indietro", onBack) },
                    end = {
                        val synced = settings.calendarConnected && g.calendarEventId != null
                        Row(
                            Modifier.clip(RoundedCornerShape(20.dp)).background(Color.White.copy(alpha = 0.08f))
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Icon(NsIcons.Calendar, null, tint = if (synced) Neon.Cyan else Neon.Text3, modifier = Modifier.size(12.dp))
                            Text(if (synced) "Sincronizzato" else "Solo in app", color = if (synced) Neon.Cyan else Neon.Text3,
                                fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                        }
                    },
                )
                Row(Modifier.padding(top = 20.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    ProgressRing(g.progress, 78.dp, 7.dp, muted = postponed, label = "${(g.progress * 100).toInt()}%",
                        modifier = Modifier.glow(39.dp, blur = 14.dp, durationMs = 2400))
                    Column {
                        Chip(g.area.label, Modifier.pop(150), filled = true)
                        Text(g.title, color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.ExtraBold, lineHeight = 23.sp,
                            modifier = Modifier.padding(top = 6.dp).enter(0))
                        Text(
                            if (postponed) "Nuova data: ${SummaryBuilder.formatDate(g.deadline)} · ci si riprova, con calma"
                            else "Scadenza: ${SummaryBuilder.formatDate(g.deadline)}",
                            color = Neon.Text2, fontSize = 11.5.sp, modifier = Modifier.padding(top = 4.dp).enter(1),
                        )
                    }
                }
            }

            Column(
                Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // alla scadenza l'app chiede se è stato raggiunto
                if (g.isDue()) DueBanner(onYes = { vm.achieve(g.id); onAchieved() }, onLater = { pickDate = true })

                NeonColumnCard(Modifier.fillMaxWidth().enter(0)) {
                    Icon(NsIcons.Quote, null, tint = Neon.Cyan.copy(alpha = 0.6f), modifier = Modifier.size(26.dp, 20.dp))
                    Text(g.summary, color = Neon.TextSoft, fontSize = 13.5.sp, lineHeight = 21.sp, fontStyle = FontStyle.Italic,
                        modifier = Modifier.padding(top = 8.dp))
                }

                ActionsCard(g, vm, Modifier.enter(1))

                if (g.isOpen && !g.isDue()) {
                    val crit = Checkins.nextCriterion(g)
                    NeonCard(Modifier.fillMaxWidth().enter(2), corner = 14.dp, padding = 13.dp, onClick = onCheckin) {
                        Icon(NsIcons.Bell, null, tint = Neon.Cyan, modifier = Modifier.size(15.dp).bellSwing())
                        Text("Prossimo check-in: \"${crit.label.lowercase()}?\"", color = Neon.TextMid, fontSize = 12.sp,
                            modifier = Modifier.weight(1f))
                        Icon(NsIcons.Chevron, null, tint = Neon.Text3, modifier = Modifier.size(12.dp).nudgeX())
                    }
                }

                AnswersCard(g, Modifier.enter(3))
                TextLink("Elimina obiettivo", { confirmDelete = true }, Modifier.align(Alignment.CenterHorizontally), color = Neon.Text3)
            }

            if (g.isOpen) {
                Row(
                    Modifier.navigationBarsPadding().padding(start = 22.dp, end = 22.dp, bottom = 16.dp, top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    GhostButton("Posticipa", { pickDate = true }, Modifier.weight(1f))
                    GradientButton("Segna come raggiunto", { vm.achieve(g.id); onAchieved() }, Modifier.weight(1.4f))
                }
            }
        }
    }

    if (pickDate) NeonDatePicker(
        initial = maxOf(g.deadline, java.time.LocalDate.now()).plusWeeks(2),
        onPick = { vm.postpone(g.id, it); pickDate = false },
        onDismiss = { pickDate = false },
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

/** Azioni concrete verso la scadenza: da qui nasce la percentuale. */
@Composable
private fun ActionsCard(g: Goal, vm: MainViewModel, modifier: Modifier) {
    var newAction by rememberSaveable { mutableStateOf("") }
    NeonColumnCard(modifier.fillMaxWidth(), corner = 16.dp) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            ProgressRing(g.progress, 44.dp, 5.dp, delayMs = 200)
            Column {
                Text("Passo dopo passo", color = Neon.Text, fontSize = 13.5.sp, fontWeight = FontWeight.Bold)
                Text(
                    if (g.actions.isEmpty()) "Aggiungi le azioni che ti portano alla meta"
                    else "${g.actions.count { it.done }} azioni fatte su ${g.actions.size} verso la scadenza",
                    color = Neon.Text2, fontSize = 11.5.sp,
                )
            }
        }
        Column(Modifier.padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            g.actions.forEachIndexed { i, a -> ActionRow(a, Modifier.enter(i, 60), { vm.toggleAction(g.id, a.id) }, { vm.removeAction(g.id, a.id) }) }
            if (g.isOpen) Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NeonTextField(newAction, { newAction = it }, "Nuova azione…", singleLine = true, modifier = Modifier.weight(1f))
                Box(
                    Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(Neon.Violet.copy(alpha = 0.25f))
                        .clickable(role = Role.Button, onClickLabel = "Aggiungi azione") { vm.addAction(g.id, newAction); newAction = "" },
                    contentAlignment = Alignment.Center,
                ) { Icon(NsIcons.Plus, "Aggiungi", tint = Neon.Cyan, modifier = Modifier.size(18.dp)) }
            }
        }
    }
}

@Composable
private fun ActionRow(a: GoalAction, modifier: Modifier, onToggle: () -> Unit, onRemove: () -> Unit) {
    val box by animateColorAsState(if (a.done) Neon.Cyan else Color.Transparent, label = "c")
    Row(
        modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).clickable(role = Role.Checkbox, onClick = onToggle).padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            Modifier.size(20.dp).clip(RoundedCornerShape(6.dp)).background(box).border(1.5.dp, Neon.Cyan, RoundedCornerShape(6.dp)),
            contentAlignment = Alignment.Center,
        ) { if (a.done) Icon(NsIcons.Check, null, tint = Neon.Night, modifier = Modifier.size(13.dp).pop()) }
        Text(
            a.text, color = if (a.done) Neon.Text3 else Neon.TextMid, fontSize = 13.sp, modifier = Modifier.weight(1f),
            textDecoration = if (a.done) TextDecoration.LineThrough else null,
        )
        Icon(NsIcons.Trash, "Rimuovi", tint = Neon.Text3, modifier = Modifier.size(15.dp).clickable(onClick = onRemove))
    }
}

/** Le 6 risposte originali, apribili a scomparsa. */
@Composable
private fun AnswersCard(g: Goal, modifier: Modifier) {
    var open by rememberSaveable { mutableStateOf(false) }
    NeonColumnCard(modifier.fillMaxWidth().clickable { open = !open }, corner = 16.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Le tue 6 risposte", color = Neon.Text, fontSize = 13.5.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Text(if (open) "Chiudi" else "Apri", color = Neon.Cyan, fontSize = 12.sp)
        }
        AnimatedVisibility(open, enter = fadeIn() + expandVertically(), exit = shrinkVertically()) {
            Column(Modifier.padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Criterion.entries.forEach { c ->
                    Column {
                        Text(c.label.uppercase(), color = Neon.Lilac, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        val text = if (c == Criterion.CONTESTUALIZZATO)
                            listOf(SummaryBuilder.formatDate(g.deadline), g.answers[c].orEmpty()).filter { it.isNotBlank() }.joinToString(" · ")
                        else g.answers[c].orEmpty().ifBlank { "—" }
                        Text(text, color = Neon.TextMid, fontSize = 13.sp, lineHeight = 19.sp)
                    }
                }
            }
        }
    }
}
