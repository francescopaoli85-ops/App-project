package com.francescopaoli.northstar.ui.screens

import androidx.compose.foundation.layout.Box
import com.francescopaoli.northstar.ui.fx.spin
import com.francescopaoli.northstar.ui.fx.animatedInt
import com.francescopaoli.northstar.ui.fx.sharedElementOf
import com.francescopaoli.northstar.ui.fx.sharedTextOf
import com.francescopaoli.northstar.domain.Engagement
import com.francescopaoli.northstar.ui.components.WeeklyStepRow
import com.francescopaoli.northstar.ads.AdPlacement
import com.francescopaoli.northstar.ads.NativeAdCard
import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.francescopaoli.northstar.data.Goal
import com.francescopaoli.northstar.ui.components.icon
import com.francescopaoli.northstar.ui.components.color
import com.francescopaoli.northstar.ui.components.StarTrail
import com.francescopaoli.northstar.data.Area
import com.francescopaoli.northstar.data.GoalStatus
import com.francescopaoli.northstar.domain.SummaryBuilder
import com.francescopaoli.northstar.ui.MainViewModel
import com.francescopaoli.northstar.ui.SessionState
import com.francescopaoli.northstar.ui.components.BottomNav
import com.francescopaoli.northstar.ui.components.GradientButton
import com.francescopaoli.northstar.ui.components.GradientIconTile
import com.francescopaoli.northstar.ui.components.NeonCard
import com.francescopaoli.northstar.ui.components.NsIcons
import com.francescopaoli.northstar.ui.components.ProgressRing
import com.francescopaoli.northstar.ui.components.Tab
import com.francescopaoli.northstar.ui.fx.NeonBackdrop
import com.francescopaoli.northstar.ui.fx.bellSwing
import com.francescopaoli.northstar.ui.fx.enter
import com.francescopaoli.northstar.ui.fx.nudgeX
import com.francescopaoli.northstar.ui.fx.pop
import com.francescopaoli.northstar.ui.theme.Neon

@Composable
fun HomeScreen(
    vm: MainViewModel,
    onNew: () -> Unit,
    onOpen: (String) -> Unit,
    onCalendar: () -> Unit,
    onTab: (Tab) -> Unit,
    onWeek: () -> Unit = {},
    onPolaris: () -> Unit = {},
    onCheckin: (String) -> Unit = {},
) {
    val goals by vm.goals.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    val session by vm.session.collectAsStateWithLifecycle()
    val showAds by vm.showAds.collectAsStateWithLifecycle()
    val name = (session as? SessionState.LoggedIn)?.session?.name.orEmpty()

    // permesso notifiche (Android 13+) per i check-in
    val notifPerm = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= 33) notifPerm.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    // prima quelli arrivati a scadenza, poi per data
    val open = goals.filter { it.isOpen }.sortedWith(compareBy({ !it.isDue() }, { it.deadlineEpochDay }))
    val achieved = goals.count { it.status == GoalStatus.ACHIEVED }
    val streak = Engagement.streakWeeks(goals)
    val doneThisWeek = Engagement.doneThisWeek(goals)
    // "+" su una card o "Scegli un piccolo passo": nuovo passo senza aprire l'obiettivo
    var quickAddFor by androidx.compose.runtime.saveable.rememberSaveable { androidx.compose.runtime.mutableStateOf<String?>(null) }
    val checkinGoal = open.firstOrNull { com.francescopaoli.northstar.domain.Checkins.needsAnswer(it) }

    NeonBackdrop(particles = 8, seed = 2) {
        Twinkles()
        Column(Modifier.fillMaxSize()) {
          Box(Modifier.weight(1f)) {
            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 22.dp, end = 22.dp, bottom = 20.dp),
                verticalArrangement = Arrangement.spacedBy(11.dp),
            ) {
                item {
                    Column(Modifier.statusBarsPadding().padding(top = 18.dp, bottom = 8.dp)) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text("Ciao $name", color = Neon.Text2, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                Text("I tuoi obiettivi", color = Color.White, style = MaterialTheme.typography.headlineMedium)
                            }
                            Box(contentAlignment = Alignment.Center) {
                                com.francescopaoli.northstar.ui.fx.StarRays(Modifier.size(78.dp))
                                GradientIconTile(
                                    NsIcons.Star, 44.dp, 14.dp, 20.dp,
                                    Modifier.clip(RoundedCornerShape(14.dp)).clickable(onClickLabel = "La tua Stella Polare", onClick = onPolaris),
                                )
                            }
                        }
                        Row(Modifier.padding(top = 18.dp), horizontalArrangement = Arrangement.spacedBy(22.dp)) {
                            Stat(open.size, "attivi", Neon.Lilac, Modifier.pop(300))
                            Stat(achieved, "raggiunti", Neon.Cyan, Modifier.pop(400))
                        }
                    }
                }
                item {
                    GradientButton(
                        "+ Nuovo obiettivo", onNew, Modifier.fillMaxWidth().padding(top = 4.dp),
                        corner = 16.dp, trailing = NsIcons.Arrow,
                    )
                }
                // il collegamento al calendario si propone solo dopo il primo obiettivo
                val bannerHidden = java.time.LocalDate.now().toEpochDay() < settings.calendarBannerHiddenUntil
                if (!settings.calendarConnected && goals.isNotEmpty() && !bannerHidden) {
                    item(key = "calendar") {
                        CalendarBanner(onCalendar, vm::hideCalendarBanner, Modifier.enter(0, baseDelayMs = 150).animateItem())
                    }
                }
                // check-in in un tocco, direttamente qui
                checkinGoal?.let { g ->
                    item(key = "checkin-${g.id}") {
                        CheckinCard(
                            g, onYes = { vm.confirmCheckin(g.id) }, onUpdate = { onCheckin(g.id) },
                            modifier = Modifier.enter(0, baseDelayMs = 200).animateItem(),
                        )
                    }
                }
                if (open.isNotEmpty()) {
                    item(key = "week") {
                        ThisWeekCard(
                            open, doneThisWeek.map { it.second.id }.toSet(), streak,
                            onToggle = { g, a -> vm.toggleAction(g, a) },
                            onPick = { quickAddFor = it }, onWeek = onWeek,
                            modifier = Modifier.enter(0, baseDelayMs = 250),
                        )
                    }
                }
                if (open.isEmpty()) {
                    item { EmptyState(Modifier.enter(1)) }
                }
                val adSlot = if (showAds) AdPlacement.slot(open.size, AdPlacement.HOME_AFTER) else null
                // obiettivi divisi per area: ognuna col suo colore e la sua icona
                var shown = 0
                Area.entries.forEach { area ->
                    val inArea = open.filter { it.area == area }
                    if (inArea.isEmpty()) return@forEach
                    item(key = "area-${area.name}") { AreaHeader(area, inArea.size, Modifier.padding(top = 10.dp).animateItem()) }
                    inArea.forEach { g ->
                        val i = shown++
                        item(key = g.id) { GoalCard(g, Modifier.enter(i, baseDelayMs = 50).animateItem(), onAdd = { quickAddFor = g.id }) { onOpen(g.id) } }
                        if (adSlot == i + 1) item(key = "ad") { NativeAdCard(Modifier.fillMaxWidth().enter(0, baseDelayMs = 450)) }
                    }
                }
            }
            // suggerimento fluttuante, staccato dal contenuto (compare una volta sola)
            if (open.isNotEmpty()) CoachBubble(
                "home_add", "Il + su ogni obiettivo aggiunge un'azione al volo, senza aprirlo.",
                settings.seenHints, { vm.hintSeen(it) }, Modifier.align(Alignment.BottomCenter).padding(16.dp),
            )
          }
            BottomNav(Tab.HOME, onTab)
        }
    }

    quickAddFor?.let { id ->
        QuickTextDialog(
            title = "Nuovo passo",
            placeholder = "Es. 20 minuti di corsa domani",
            confirm = "Aggiungi",
            onConfirm = { vm.addAction(id, it); quickAddFor = null },
            onDismiss = { quickAddFor = null },
        )
    }
}

/** Check-in in Home: si risponde con un tocco, oppure si aggiorna la risposta. */
@Composable
private fun CheckinCard(g: Goal, onYes: () -> Unit, onUpdate: () -> Unit, modifier: Modifier) {
    val crit = com.francescopaoli.northstar.domain.Checkins.nextCriterion(g)
    com.francescopaoli.northstar.ui.components.NeonColumnCard(modifier.fillMaxWidth(), corner = 17.dp) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(NsIcons.Bell, null, tint = Neon.Cyan, modifier = Modifier.size(16.dp).bellSwing(2200))
            Text("CHECK-IN · ${crit.label.uppercase()}", color = Neon.Lilac, style = MaterialTheme.typography.labelSmall)
        }
        Text(
            com.francescopaoli.northstar.domain.Checkins.question(g, crit), color = Neon.Text,
            fontSize = 14.5.sp, fontWeight = FontWeight.SemiBold, lineHeight = 20.sp, modifier = Modifier.padding(top = 8.dp, bottom = 12.dp),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            com.francescopaoli.northstar.ui.components.GhostButton("Qualcosa è cambiato", onUpdate, Modifier.weight(1f))
            GradientButton("Sì, tutto ok", onYes, Modifier.weight(1f), corner = 14.dp, glowing = false, tapSound = false)
        }
    }
}

@Composable
private fun Stat(n: Int, label: String, color: Color, modifier: Modifier) {
    Row(modifier, verticalAlignment = Alignment.Bottom) {
        Text("${animatedInt(n, 800, 300)}", color = color, fontSize = 23.sp, fontWeight = FontWeight.ExtraBold)
        Text(" $label", color = Neon.Text2, fontSize = 12.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(bottom = 4.dp))
    }
}

/**
 * Invito a collegare Google Calendar: chiaro su cosa fa, con un bottone vero
 * e una X per nasconderlo (torna dopo una settimana).
 */
@Composable
private fun CalendarBanner(onConnect: () -> Unit, onHide: () -> Unit, modifier: Modifier) {
    // una riga sola: tocchi ovunque per collegare, la X la nasconde per una settimana
    Row(
        modifier.fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Neon.Surface.copy(alpha = 0.7f))
            .border(1.dp, Neon.Violet.copy(alpha = 0.3f), RoundedCornerShape(14.dp))
            .clickable(onClickLabel = "Collega Google Calendar", onClick = onConnect)
            .padding(start = 12.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        GradientIconTile(NsIcons.Calendar, 30.dp, 9.dp, 14.dp, floating = false)
        Column(Modifier.weight(1f)) {
            Text("Collega Google Calendar", color = Neon.Text, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Text("Scadenze in agenda, in automatico", color = Neon.Text3, fontSize = 11.sp)
        }
        Text("Collega", color = Neon.Cyan, fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
        Icon(
            NsIcons.Close, "Nascondi", tint = Neon.Text3,
            modifier = Modifier.size(34.dp).clip(RoundedCornerShape(17.dp)).clickable(onClickLabel = "Nascondi", onClick = onHide).padding(10.dp),
        )
    }
}

@Composable
private fun EmptyState(modifier: Modifier) {
    NeonCard(modifier.fillMaxWidth().padding(top = 8.dp), padding = 20.dp) {
        Column {
            Text("Nessun obiettivo, per ora", color = Neon.Text, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Text(
                "Tocca \"Nuovo obiettivo\": 6 domande, e un desiderio vago diventa una meta con una data.",
                color = Neon.Text2, fontSize = 12.5.sp, lineHeight = 19.sp, modifier = Modifier.padding(top = 6.dp),
            )
        }
    }
}

/** Intestazione di gruppo: icona e colore dell'area, quanti obiettivi ci sono. */
@Composable
private fun AreaHeader(area: Area, count: Int, modifier: Modifier) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(
            Modifier.size(28.dp).clip(RoundedCornerShape(9.dp)).background(area.color.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center,
        ) { Icon(area.icon, null, tint = area.color, modifier = Modifier.size(16.dp)) }
        Text(area.label, color = Neon.Text, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        Text(if (count == 1) "1 obiettivo" else "$count obiettivi", color = Neon.Text3, fontSize = 12.sp)
    }
}

@Composable
fun GoalCard(g: Goal, modifier: Modifier = Modifier, onAdd: (() -> Unit)? = null, onClick: () -> Unit) {
    val postponed = g.status == GoalStatus.POSTPONED
    val done = g.actions.count { it.done }
    val days = g.daysLeft()
    val sub = when {
        g.isDue() -> "è arrivato il giorno ✦"
        postponed -> "nuova data · ${SummaryBuilder.formatDate(g.deadline)}"
        days == 1L -> "manca 1 giorno"
        else -> "$days giorni rimasti"
    }
    val steps = when {
        g.actions.isEmpty() -> "nessuna azione ancora"
        done == g.actions.size -> "tutte le azioni fatte"
        else -> "$done di ${g.actions.size} azioni"
    }
    NeonCard(modifier.fillMaxWidth(), dashed = postponed, onClick = onClick) {
        // striscia del colore dell'area
        Box(Modifier.size(4.dp, 46.dp).clip(RoundedCornerShape(2.dp)).background(g.area.color.copy(alpha = if (postponed) 0.4f else 0.9f)))
        Column(Modifier.weight(1f)) {
            Text(g.title, color = Neon.Text, fontSize = 14.5.sp, fontWeight = FontWeight.SemiBold, maxLines = 2,
                overflow = TextOverflow.Ellipsis, modifier = Modifier.sharedTextOf("title-${g.id}"))
            StarTrail(done, g.actions.size, Modifier.padding(top = 8.dp), height = 18.dp, muted = postponed)
            Text("$steps · $sub", color = if (g.isDue()) Neon.Cyan else Neon.Text3, fontSize = 11.5.sp, modifier = Modifier.padding(top = 4.dp))
        }
        if (onAdd != null) Box(
            Modifier.size(34.dp).clip(RoundedCornerShape(11.dp)).background(Neon.Violet.copy(alpha = 0.22f))
                .clickable(role = androidx.compose.ui.semantics.Role.Button, onClickLabel = "Aggiungi un'azione", onClick = onAdd),
            contentAlignment = Alignment.Center,
        ) { Icon(NsIcons.Plus, "Aggiungi un'azione", tint = Neon.Cyan, modifier = Modifier.size(15.dp)) }
    }
}

/**
 * "Questa settimana": un piccolo passo per ogni obiettivo aperto, da spuntare al volo.
 * È il motivo per tornare nell'app qualche volta a settimana.
 */
@Composable
private fun ThisWeekCard(
    open: List<Goal>,
    doneIds: Set<String>,
    streak: Int,
    onToggle: (goalId: String, actionId: String) -> Unit,
    onPick: (goalId: String) -> Unit,
    onWeek: () -> Unit,
    modifier: Modifier,
) {
    com.francescopaoli.northstar.ui.components.NeonColumnCard(modifier.fillMaxWidth().padding(top = 4.dp), corner = 17.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("QUESTA SETTIMANA", color = Neon.Lilac, style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(1f))
            Text("Riepilogo", color = Neon.Cyan, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable(onClick = onWeek).padding(4.dp))
        }
        Text(
            buildString {
                append(if (doneIds.isEmpty()) "La prossima azione di ogni obiettivo: spuntala quando l'hai fatta." else "Ottimo: ${doneIds.size} ${if (doneIds.size == 1) "azione fatta" else "azioni fatte"} questa settimana.")
                if (streak > 1) append(" · $streak settimane di fila")
            },
            color = Neon.Text2, fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp, bottom = 6.dp),
        )
        open.take(3).forEachIndexed { i, g ->
            // se questa settimana ha già fatto un passo per l'obiettivo, lo mostro spuntato
            val doneHere = g.actions.lastOrNull { it.id in doneIds }
            val step = doneHere ?: Engagement.weeklyStep(g)
            WeeklyStepRow(
                goalTitle = g.title, step = step?.text, done = doneHere != null,
                onToggle = { step?.let { onToggle(g.id, it.id) } },
                onPick = { onPick(g.id) },
                modifier = Modifier.enter(i, 70, 350),
            )
        }
    }
}
