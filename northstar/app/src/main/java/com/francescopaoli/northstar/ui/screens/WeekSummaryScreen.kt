package com.francescopaoli.northstar.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.francescopaoli.northstar.domain.Engagement
import com.francescopaoli.northstar.ui.MainViewModel
import com.francescopaoli.northstar.ui.components.FlameIcon
import com.francescopaoli.northstar.ui.components.GradientButton
import com.francescopaoli.northstar.ui.components.NeonCard
import com.francescopaoli.northstar.ui.components.NeonColumnCard
import com.francescopaoli.northstar.ui.components.NsIcons
import com.francescopaoli.northstar.ui.components.RoundIconButton
import com.francescopaoli.northstar.ui.fx.BurstRings
import com.francescopaoli.northstar.ui.fx.NeonBackdrop
import com.francescopaoli.northstar.ui.fx.enter
import com.francescopaoli.northstar.ui.fx.pop
import com.francescopaoli.northstar.ui.theme.Neon
import java.time.LocalDate

/** Riepilogo della domenica: cosa hai fatto, la tua serie, cosa ti aspetta. */
@Composable
fun WeekSummaryScreen(vm: MainViewModel, onClose: () -> Unit) {
    val goals by vm.goals.collectAsStateWithLifecycle()
    val today = LocalDate.now()
    val done = Engagement.doneThisWeek(goals, today)
    val streak = Engagement.streakWeeks(goals, today)
    val open = goals.filter { it.isOpen }.sortedBy { it.deadlineEpochDay }

    NeonBackdrop(particles = 10, seed = 21) {
        Column(Modifier.fillMaxSize().navigationBarsPadding()) {
            HeaderBand {
                TopRow(
                    start = { RoundIconButton(NsIcons.Close, "Chiudi", onClose) },
                    end = { Text("RIEPILOGO", color = Neon.Text2, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                )
            }
            Column(
                Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 22.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(Modifier.size(130.dp), contentAlignment = Alignment.Center) {
                    if (streak > 0) BurstRings(Modifier.fillMaxSize(), listOf(Color(0xFFFFC107), Neon.Violet, Color(0xFFFF6B3D)))
                    FlameIcon(64.dp, lit = streak > 0, modifier = Modifier.pop(0, -12f))
                }
                Text("La tua settimana", color = Color.White, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.enter(0))
                Text(
                    when {
                        done.isEmpty() -> "Settimana tranquilla: succede. Basta un passo piccolo per ripartire."
                        streak > 1 -> "$streak settimane di fila con almeno un passo. Continua così!"
                        else -> "Hai acceso la serie: la prossima settimana tienila viva."
                    },
                    color = Neon.Text2, fontSize = 13.5.sp, lineHeight = 20.sp, textAlign = TextAlign.Center, modifier = Modifier.enter(1),
                )
                Row(Modifier.fillMaxWidth().enter(2), horizontalArrangement = Arrangement.spacedBy(11.dp)) {
                    BigStat("${done.size}", if (done.size == 1) "passo fatto" else "passi fatti", Neon.Cyan, Modifier.weight(1f).pop(300))
                    BigStat("$streak", "sett. di fila", Color(0xFFFFB547), Modifier.weight(1f).pop(400))
                    BigStat("${open.size}", "in corso", Neon.Lilac, Modifier.weight(1f).pop(500))
                }
                if (done.isNotEmpty()) NeonColumnCard(Modifier.fillMaxWidth().enter(3)) {
                    Text("FATTO QUESTA SETTIMANA", color = Neon.Lilac, style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(bottom = 8.dp))
                    done.forEachIndexed { i, (g, a) ->
                        Row(Modifier.padding(vertical = 5.dp).enter(i, 60, 400), verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Icon(NsIcons.Check, null, tint = Neon.Cyan, modifier = Modifier.size(16.dp))
                            Column {
                                Text(a.text, color = Neon.Text, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold)
                                Text(g.title, color = Neon.Text3, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
                open.take(4).forEachIndexed { i, g ->
                    NeonCard(Modifier.fillMaxWidth().enter(4 + i), dashed = g.status == com.francescopaoli.northstar.data.GoalStatus.POSTPONED) {
                        Column(Modifier.weight(1f)) {
                            Text(g.title, color = Neon.Text, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold, maxLines = 1,
                                overflow = TextOverflow.Ellipsis)
                            Text(
                                Engagement.weeklyStep(g)?.let { "Prossimo passo: ${it.text}" } ?: "Scegli il prossimo passo",
                                color = Neon.Text2, fontSize = 11.5.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                            )
                        }
                        val days = g.daysLeft()
                        Text(if (days <= 0) "oggi" else "$days gg", color = Neon.Lilac, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(Modifier.height(4.dp))
            }
            GradientButton("Scegli i passi della settimana", onClose, Modifier.fillMaxWidth().padding(start = 22.dp, end = 22.dp, bottom = 20.dp))
        }
    }
}

@Composable
private fun BigStat(value: String, label: String, color: Color, modifier: Modifier) {
    NeonColumnCard(modifier, corner = 14.dp) {
        Text(value, color = color, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)
        Text(label, color = Neon.Text2, fontSize = 11.sp)
    }
}
