package com.francescopaoli.northstar.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.background
import com.francescopaoli.northstar.ui.components.icon
import com.francescopaoli.northstar.ui.components.color
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.francescopaoli.northstar.data.Goal
import com.francescopaoli.northstar.domain.Engagement
import com.francescopaoli.northstar.domain.SummaryBuilder
import com.francescopaoli.northstar.ui.MainViewModel
import com.francescopaoli.northstar.ui.components.Chip
import com.francescopaoli.northstar.ui.components.GradientButton
import com.francescopaoli.northstar.ui.components.GradientIconTile
import com.francescopaoli.northstar.ui.components.NeonColumnCard
import com.francescopaoli.northstar.ui.components.NsIcons
import com.francescopaoli.northstar.ui.components.ProgressRing
import com.francescopaoli.northstar.ui.components.RoundIconButton
import com.francescopaoli.northstar.ui.components.glass
import com.francescopaoli.northstar.ui.fx.BurstRings
import com.francescopaoli.northstar.ui.fx.NeonBackdrop
import com.francescopaoli.northstar.ui.fx.StarRays
import com.francescopaoli.northstar.ui.fx.breathe
import com.francescopaoli.northstar.ui.fx.enter
import com.francescopaoli.northstar.ui.fx.pop
import com.francescopaoli.northstar.ui.fx.twinkle
import com.francescopaoli.northstar.ui.theme.Neon

/**
 * "La tua Stella Polare": la stellina in alto in Home porta qui.
 * Mostra in grande l'obiettivo più vicino, come una bussola: è lì che stai andando.
 */
@Composable
fun PolarisScreen(vm: MainViewModel, onClose: () -> Unit, onOpen: (String) -> Unit, onNew: () -> Unit) {
    val goals by vm.goals.collectAsStateWithLifecycle()
    // prima quelli arrivati a scadenza, poi il più vicino
    val open = goals.filter { it.isOpen }.sortedWith(compareBy({ !it.isDue() }, { it.deadlineEpochDay }))
    val polar = open.firstOrNull()

    NeonBackdrop(particles = 16, seed = 33) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            Row(Modifier.fillMaxWidth().padding(22.dp), horizontalArrangement = Arrangement.End) {
                RoundIconButton(NsIcons.Close, "Chiudi", onClose)
            }
            Column(
                Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // la stella: raggi che ruotano, onde che si allargano, e respira
                Box(Modifier.size(230.dp), contentAlignment = Alignment.Center) {
                    StarRays(Modifier.size(230.dp), alpha = 0.6f)
                    BurstRings(Modifier.size(200.dp))
                    GradientIconTile(NsIcons.Star, 96.dp, 30.dp, 46.dp, Modifier.pop(0, -25f).breathe(1.06f, 2200))
                }
                Text("La tua Stella Polare", color = Color.White, style = MaterialTheme.typography.headlineMedium,
                    textAlign = TextAlign.Center, modifier = Modifier.enter(0))
                if (polar == null) {
                    Text(
                        "Ancora nessuna stella da seguire.\nOgni viaggio inizia scegliendo una direzione.",
                        color = Neon.Text2, fontSize = 14.sp, lineHeight = 21.sp, textAlign = TextAlign.Center, modifier = Modifier.enter(1),
                    )
                    GradientButton("Crea il tuo primo obiettivo", onNew, Modifier.fillMaxWidth().padding(top = 8.dp).enter(2))
                } else {
                    Text("L'obiettivo più vicino: è lì che stai andando.", color = Neon.Text2, fontSize = 13.5.sp,
                        textAlign = TextAlign.Center, modifier = Modifier.enter(1))
                    PolarCard(polar, Modifier.enter(2, stepMs = 110)) { onOpen(polar.id) }
                    if (open.size > 1) {
                        Text("LE ALTRE STELLE", color = Neon.Lilac, style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.fillMaxWidth().padding(top = 6.dp).enter(3))
                        open.drop(1).take(4).forEachIndexed { i, g -> OtherStar(g, Modifier.enter(4 + i, 70)) { onOpen(g.id) } }
                    }
                }
            }
        }
    }
}

@Composable
private fun PolarCard(g: Goal, modifier: Modifier, onOpen: () -> Unit) {
    val days = g.daysLeft()
    NeonColumnCard(modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            androidx.compose.foundation.layout.Box(
                Modifier.size(52.dp).clip(androidx.compose.foundation.shape.RoundedCornerShape(16.dp))
                    .background(g.area.color.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center,
            ) { Icon(g.area.icon, null, tint = g.area.color, modifier = Modifier.size(26.dp)) }
            Column(Modifier.weight(1f)) {
                Text(g.area.label.uppercase(), color = g.area.color, style = androidx.compose.material3.MaterialTheme.typography.labelSmall, modifier = Modifier.pop(200))
                Text(g.title, color = Neon.Text, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, lineHeight = 22.sp,
                    modifier = Modifier.padding(top = 6.dp))
            }
        }
        com.francescopaoli.northstar.ui.components.StarTrail(
            g.actions.count { it.done }, g.actions.size, Modifier.padding(top = 14.dp), height = 26.dp,
        )
        Text(g.summary, color = Neon.TextSoft, fontSize = 13.5.sp, lineHeight = 21.sp, fontStyle = FontStyle.Italic,
            modifier = Modifier.padding(top = 14.dp))
        Row(Modifier.padding(top = 14.dp), horizontalArrangement = Arrangement.spacedBy(18.dp)) {
            Info(
                when {
                    g.isDue() -> "oggi"
                    days == 1L -> "1 giorno"
                    else -> "$days giorni"
                },
                "alla meta · ${SummaryBuilder.formatDate(g.deadline)}",
            )
        }
        Engagement.weeklyStep(g)?.let {
            Text("Prossimo passo: ${it.text}", color = Neon.Cyan, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 10.dp))
        }
        GradientButton("Apri l'obiettivo", onOpen, Modifier.fillMaxWidth().padding(top = 16.dp), corner = 12.dp)
    }
}

@Composable
private fun Info(value: String, label: String) {
    Column {
        Text(value, color = Neon.Lilac, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
        Text(label, color = Neon.Text2, fontSize = 11.5.sp)
    }
}

@Composable
private fun OtherStar(g: Goal, modifier: Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(14.dp)
    Row(
        modifier.fillMaxWidth().glass(shape).clip(shape).clickable(onClick = onClick).padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(NsIcons.Star, null, tint = Neon.Lilac, modifier = Modifier.size(14.dp).twinkle(2400))
        Text(g.title, color = Neon.TextMid, fontSize = 13.5.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
        Text("${g.daysLeft().coerceAtLeast(0)} gg", color = Neon.Text3, fontSize = 12.sp)
    }
}
