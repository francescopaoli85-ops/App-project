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
import com.francescopaoli.northstar.ui.fx.animatedGradient
import com.francescopaoli.northstar.ui.fx.glow
import androidx.compose.foundation.shape.CircleShape
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

    // Home essenziale: saluto, stella, obiettivi. Tutto il resto vive altrove (menu, Altro, notifiche).
    NeonBackdrop(particles = 8, seed = 2) {
        Twinkles()
        Column(Modifier.fillMaxSize()) {
          Box(Modifier.weight(1f)) {
            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 22.dp, end = 22.dp, bottom = 110.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // il tuo cielo: ogni obiettivo è una stella, in basso la Stella Polare
                item {
                    Column(Modifier.statusBarsPadding().padding(top = 18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Ciao $name", color = Neon.Text2, fontSize = 13.sp, fontWeight = FontWeight.Medium, modifier = Modifier.fillMaxWidth())
                        HomeSky(open, onGoal = onOpen, onPolaris = onPolaris, modifier = Modifier.padding(top = 4.dp))
                        Text(
                            when (open.size) { 0 -> "Il tuo cielo ti aspetta"; 1 -> "1 stella da raggiungere"; else -> "${open.size} stelle da raggiungere" },
                            color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold,
                        )
                        Text(
                            "“${phraseOfTheDay()}”", color = Neon.Text2, fontSize = 13.sp, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center, modifier = Modifier.padding(top = 4.dp, bottom = 10.dp),
                        )
                    }
                }
                if (open.isEmpty()) item { EmptyState(Modifier.enter(1)) }
                val adSlot = if (showAds) AdPlacement.slot(open.size, AdPlacement.HOME_AFTER) else null
                open.forEachIndexed { i, g ->
                    item(key = g.id) { GoalCard(g, Modifier.enter(i, baseDelayMs = 50).animateItem()) { onOpen(g.id) } }
                    if (adSlot == i + 1) item(key = "ad") { NativeAdCard(Modifier.fillMaxWidth().enter(0, baseDelayMs = 450)) }
                }
            }
            // un solo bottone: crea un nuovo obiettivo
            Box(
                Modifier.align(Alignment.BottomEnd).padding(end = 22.dp, bottom = 22.dp).size(62.dp)
                    .glow(31.dp, blur = 18.dp, durationMs = 2600)
                    .clip(CircleShape).animatedGradient(31.dp)
                    .clickable(role = androidx.compose.ui.semantics.Role.Button, onClickLabel = "Nuovo obiettivo", onClick = onNew),
                contentAlignment = Alignment.Center,
            ) { Icon(NsIcons.Plus, "Nuovo obiettivo", tint = Neon.OnAccent, modifier = Modifier.size(26.dp)) }
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
fun GoalCard(g: Goal, modifier: Modifier = Modifier, onAdd: (() -> Unit)? = null, onClick: () -> Unit) {
    val postponed = g.status == GoalStatus.POSTPONED
    val done = g.actions.count { it.done }
    val days = g.daysLeft()
    val line = buildString {
        append(if (g.actions.isEmpty()) "nessuna azione" else "$done di ${g.actions.size} azioni")
        append(" · ")
        append(when { g.isDue() -> "è arrivato il giorno ✦"; days == 1L -> "1 giorno"; else -> "$days giorni" })
    }
    Box(modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Neon.Surface.copy(alpha = 0.75f)).clickable(onClick = onClick)) {
        // striscia del colore dell'area e la sua piccola costellazione: la firma della card
        Box(Modifier.align(Alignment.CenterStart).padding(vertical = 14.dp).size(3.dp, 52.dp).clip(RoundedCornerShape(2.dp)).background(g.area.color))
        Column(Modifier.padding(start = 18.dp, end = 16.dp, top = 14.dp, bottom = 14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(g.title, color = Neon.Text, fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 2,
                overflow = TextOverflow.Ellipsis, modifier = Modifier.sharedTextOf("title-${g.id}"))
            StarTrail(done, g.actions.size, height = 18.dp, muted = postponed)
            Text(line, color = if (g.isDue()) Neon.Cyan else Neon.Text3, fontSize = 12.sp)
        }
    }
}

