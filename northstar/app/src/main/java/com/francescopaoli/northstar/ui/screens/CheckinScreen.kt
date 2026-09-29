package com.francescopaoli.northstar.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.francescopaoli.northstar.domain.Checkins
import com.francescopaoli.northstar.ui.MainViewModel
import com.francescopaoli.northstar.ui.components.Chip
import com.francescopaoli.northstar.ui.components.GradientButton
import com.francescopaoli.northstar.ui.components.GradientIconTile
import com.francescopaoli.northstar.ui.components.NsIcons
import com.francescopaoli.northstar.ui.components.RoundIconButton
import com.francescopaoli.northstar.ui.components.TextLink
import com.francescopaoli.northstar.ui.fx.NeonBackdrop
import com.francescopaoli.northstar.ui.fx.bellSwing
import com.francescopaoli.northstar.ui.fx.enter
import com.francescopaoli.northstar.ui.fx.pop
import com.francescopaoli.northstar.ui.theme.Neon

/** Check-in: richiama UN criterio PNL e permette di aggiornare quella risposta. */
@Composable
fun CheckinScreen(vm: MainViewModel, id: String, onClose: () -> Unit, onDone: () -> Unit) {
    val goals by vm.goals.collectAsStateWithLifecycle()
    val g = goals.firstOrNull { it.id == id } ?: run { NeonBackdrop { }; return }
    val crit = Checkins.nextCriterion(g)
    var answering by rememberSaveable { mutableStateOf(false) }
    var text by rememberSaveable(crit) { mutableStateOf(g.answers[crit].orEmpty()) }

    NeonBackdrop(particles = 8, seed = 6) {
        Column(Modifier.fillMaxSize().imePadding()) {
            HeaderBand {
                TopRow(
                    start = { RoundIconButton(NsIcons.Close, "Chiudi", onClose) },
                    end = { Text("CHECK-IN", color = Neon.Text2, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                )
            }
            Column(
                Modifier.weight(1f).fillMaxWidth().padding(horizontal = 30.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                GradientIconTile(NsIcons.Bell, 70.dp, 22.dp, 30.dp, Modifier.enter(0), iconModifier = Modifier.bellSwing(1800))
                Spacer(Modifier.height(24.dp))
                Chip(crit.label, Modifier.pop(120))
                Text(Checkins.question(g, crit), color = Color.White, style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center, modifier = Modifier.padding(top = 14.dp).enter(1))
                Text(
                    "Un check rapido su uno dei criteri del tuo obiettivo, per tenerlo vivo e realistico.",
                    color = Neon.Text2, fontSize = 13.sp, lineHeight = 20.sp, textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 12.dp).widthIn(max = 280.dp).enter(2),
                )
                AnimatedVisibility(answering, enter = fadeIn() + expandVertically()) {
                    Column(Modifier.padding(top = 20.dp)) {
                        Text("La tua risposta di allora — aggiornala se è cambiato qualcosa:", color = Neon.Text3, fontSize = 11.5.sp,
                            modifier = Modifier.padding(bottom = 8.dp))
                        NeonTextField(text, { text = it }, crit.hint, minLines = 3)
                    }
                }
            }
            Column(
                Modifier.navigationBarsPadding().padding(start = 24.dp, end = 24.dp, bottom = 24.dp).enter(3),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (!answering) GradientButton("Sì, rispondi ora", { answering = true }, Modifier.fillMaxWidth())
                else GradientButton("Salva", { vm.answerCheckin(g.id, crit, text); onDone() }, Modifier.fillMaxWidth())
                TextLink("Più tardi", onClose, Modifier.fillMaxWidth())
            }
        }
    }
}
