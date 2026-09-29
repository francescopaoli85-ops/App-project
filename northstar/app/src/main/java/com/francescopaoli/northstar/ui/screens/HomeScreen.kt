package com.francescopaoli.northstar.ui.screens

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
import androidx.compose.foundation.lazy.itemsIndexed
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
import com.francescopaoli.northstar.ui.fx.glow
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
) {
    val goals by vm.goals.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    val session by vm.session.collectAsStateWithLifecycle()
    val name = (session as? SessionState.LoggedIn)?.session?.name.orEmpty()

    // permesso notifiche (Android 13+) per i check-in
    val notifPerm = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= 33) notifPerm.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    // prima quelli arrivati a scadenza, poi per data
    val open = goals.filter { it.isOpen }.sortedWith(compareBy({ !it.isDue() }, { it.deadlineEpochDay }))
    val achieved = goals.count { it.status == GoalStatus.ACHIEVED }

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
                            GradientIconTile(NsIcons.Star, 44.dp, 14.dp, 20.dp)
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
                if (!settings.calendarConnected && goals.isNotEmpty()) {
                    item { CalendarBanner(onCalendar, Modifier.enter(0, baseDelayMs = 150)) }
                }
                if (open.isEmpty()) {
                    item { EmptyState(Modifier.enter(1)) }
                }
                itemsIndexed(open, key = { _, g -> g.id }) { i, g ->
                    GoalCard(g, Modifier.enter(i, baseDelayMs = 50)) { onOpen(g.id) }
                }
            }
            BottomNav(Tab.HOME, onTab)
        }
    }
}

@Composable
private fun Stat(n: Int, label: String, color: Color, modifier: Modifier) {
    Row(modifier, verticalAlignment = Alignment.Bottom) {
        Text("$n", color = color, fontSize = 23.sp, fontWeight = FontWeight.ExtraBold)
        Text(" $label", color = Neon.Text2, fontSize = 12.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(bottom = 4.dp))
    }
}

@Composable
private fun CalendarBanner(onClick: () -> Unit, modifier: Modifier) {
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Neon.Violet.copy(alpha = 0.14f))
            .border(1.dp, Neon.Violet.copy(alpha = 0.3f), RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 13.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(NsIcons.Bell, null, tint = Neon.Cyan, modifier = Modifier.size(16.dp).bellSwing(2200))
        Text("Funziona meglio con Google Calendar collegato", color = Neon.TextMid, fontSize = 11.5.sp,
            fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
        Icon(NsIcons.Chevron, null, tint = Neon.Text2, modifier = Modifier.size(13.dp).nudgeX())
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
            modifier = if (postponed) Modifier else Modifier.glow(21.dp, blur = 6.dp, durationMs = 2400),
        )
        Column(Modifier.weight(1f)) {
            Text(g.area.label.uppercase(), color = Neon.Text2, style = MaterialTheme.typography.labelSmall)
            Text(g.title, color = Neon.Text, fontSize = 14.5.sp, fontWeight = FontWeight.SemiBold, maxLines = 2,
                overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 4.dp))
            Text(sub, color = if (g.isDue()) Neon.Cyan else Neon.Text3, fontSize = 11.5.sp, modifier = Modifier.padding(top = 4.dp))
        }
        Text("$pct%", color = if (postponed) Neon.Text3 else Neon.Lilac, fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
}
