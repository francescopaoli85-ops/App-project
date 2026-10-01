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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.francescopaoli.northstar.data.Goal
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

    NeonBackdrop(particles = 8, seed = 2) {
        Twinkles()
        Column(Modifier.fillMaxSize()) {
            LazyColumn(
                Modifier.weight(1f),
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
                if (open.isNotEmpty()) {
                    item(key = "week") {
                        ThisWeekCard(
                            open, doneThisWeek.map { it.second.id }.toSet(), streak,
                            onToggle = { g, a -> vm.toggleAction(g, a) },
                            onPick = onOpen, onWeek = onWeek,
                            modifier = Modifier.enter(0, baseDelayMs = 250),
                        )
                    }
                }
                if (open.isEmpty()) {
                    item { EmptyState(Modifier.enter(1)) }
                }
                val adSlot = if (showAds) AdPlacement.slot(open.size, AdPlacement.HOME_AFTER) else null
                open.forEachIndexed { i, g ->
                    item(key = g.id) { GoalCard(g, Modifier.enter(i, baseDelayMs = 50)) { onOpen(g.id) } }
                    if (adSlot == i + 1) item(key = "ad") { NativeAdCard(Modifier.fillMaxWidth().enter(0, baseDelayMs = 450)) }
                }
            }
            BottomNav(Tab.HOME, onTab)
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
    com.francescopaoli.northstar.ui.components.NeonColumnCard(modifier.fillMaxWidth(), corner = 17.dp) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            GradientIconTile(NsIcons.Calendar, 40.dp, 12.dp, 18.dp, floating = false, iconModifier = Modifier.bellSwing(2600))
            Column(Modifier.weight(1f)) {
                Text("Collega Google Calendar", color = Neon.Text, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Text("Le scadenze finiscono da sole nella tua agenda.", color = Neon.Text2, fontSize = 12.sp, lineHeight = 17.sp)
            }
            Icon(
                NsIcons.Close, "Nascondi", tint = Neon.Text3,
                modifier = Modifier.size(28.dp).clip(RoundedCornerShape(14.dp)).clickable(onClickLabel = "Nascondi", onClick = onHide).padding(7.dp),
            )
        }
        GradientButton("Collega", onConnect, Modifier.fillMaxWidth().padding(top = 12.dp), corner = 12.dp, glowing = false)
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

@Composable
fun GoalCard(g: Goal, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val postponed = g.status == GoalStatus.POSTPONED
    val pct = (g.progress * 100).toInt()
    val days = g.daysLeft()
    val sub = when {
        g.isDue() -> "è arrivato il giorno ✦"
        postponed -> "nuova data · ${SummaryBuilder.formatDate(g.deadline)}"
        days == 1L -> "manca 1 giorno"
        else -> "$days giorni rimasti"
    }
    NeonCard(modifier.fillMaxWidth(), dashed = postponed, onClick = onClick) {
        ProgressRing(
            g.progress, 42.dp, 4.5.dp, muted = postponed,
            glowing = true,
            // vola fino all'header del Dettaglio
            modifier = Modifier.sharedElementOf("ring-${g.id}"),
        )
        Column(Modifier.weight(1f)) {
            Text(g.area.label.uppercase(), color = Neon.Text2, style = MaterialTheme.typography.labelSmall)
            Text(g.title, color = Neon.Text, fontSize = 14.5.sp, fontWeight = FontWeight.SemiBold, maxLines = 2,
                overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 4.dp).sharedTextOf("title-${g.id}"))
            Text(sub, color = if (g.isDue()) Neon.Cyan else Neon.Text3, fontSize = 11.5.sp, modifier = Modifier.padding(top = 4.dp))
        }
        Text("${animatedInt(pct, 1100)}%", color = if (postponed) Neon.Text3 else Neon.Lilac, fontSize = 16.sp, fontWeight = FontWeight.Bold)
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
                append(if (doneIds.isEmpty()) "Basta un passo piccolo." else "Ottimo: ${doneIds.size} ${if (doneIds.size == 1) "passo fatto" else "passi fatti"} questa settimana.")
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
