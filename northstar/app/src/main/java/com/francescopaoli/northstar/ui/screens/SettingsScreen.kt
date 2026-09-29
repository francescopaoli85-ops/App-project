package com.francescopaoli.northstar.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.francescopaoli.northstar.auth.Session
import com.francescopaoli.northstar.data.CalendarMode
import com.francescopaoli.northstar.ui.MainViewModel
import com.francescopaoli.northstar.ui.SessionState
import com.francescopaoli.northstar.ui.components.BottomNav
import com.francescopaoli.northstar.ui.components.GhostButton
import com.francescopaoli.northstar.ui.components.NeonColumnCard
import com.francescopaoli.northstar.ui.components.Tab
import com.francescopaoli.northstar.ui.components.TextLink
import com.francescopaoli.northstar.ui.fx.NeonBackdrop
import com.francescopaoli.northstar.ui.fx.animatedGradient
import com.francescopaoli.northstar.ui.fx.enter
import com.francescopaoli.northstar.ui.theme.Neon

@Composable
fun SettingsScreen(vm: MainViewModel, onTab: (Tab) -> Unit, onConnectCalendar: () -> Unit) {
    val s by vm.settings.collectAsStateWithLifecycle()
    val session = (vm.session.collectAsStateWithLifecycle().value as? SessionState.LoggedIn)?.session

    NeonBackdrop(particles = 6, seed = 14) {
        Column(Modifier.fillMaxSize()) {
            Column(
                Modifier.weight(1f).verticalScroll(rememberScrollState()).statusBarsPadding().padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text("Impostazioni", color = Color.White, style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp))

                NeonColumnCard(Modifier.fillMaxWidth().enter(0)) {
                    Label("Account")
                    Text(session?.name.orEmpty(), color = Neon.Text, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text(
                        when (session) {
                            is Session.Cloud -> session.email ?: "Account Google"
                            else -> "Modalità locale · dati solo su questo telefono"
                        },
                        color = Neon.Text2, fontSize = 12.sp,
                    )
                }

                NeonColumnCard(Modifier.fillMaxWidth().enter(1)) {
                    Label("Google Calendar")
                    if (s.calendarConnected) {
                        Text("Collegato ✦", color = Neon.Cyan, fontWeight = FontWeight.Bold)
                        Text("Creazione eventi", color = Neon.Text, fontSize = 13.sp, modifier = Modifier.padding(top = 14.dp, bottom = 8.dp))
                        ModeSwitch(s.calendarMode ?: CalendarMode.AUTO, vm::setCalendarMode)
                        if (!s.calendarModeManual) Text(
                            "Scelta in base alle tue risposte. Puoi cambiarla quando vuoi.",
                            color = Neon.Text3, fontSize = 11.sp, modifier = Modifier.padding(top = 8.dp),
                        )
                        TextLink("Scollega", vm::disconnectCalendar, color = Neon.Text3)
                    } else {
                        Text("Non collegato", color = Neon.Text2, fontSize = 13.sp, modifier = Modifier.padding(bottom = 10.dp))
                        GhostButton("Collega Google Calendar", onConnectCalendar, Modifier.fillMaxWidth())
                    }
                }

                NeonColumnCard(Modifier.fillMaxWidth().enter(2)) {
                    Label("Esperienza")
                    Toggle("Voce guida nelle domande", s.voiceGuide, vm::setVoice)
                    Toggle("Check-in periodici", s.checkins, vm::setCheckins)
                }

                GhostButton("Esci", vm::signOut, Modifier.fillMaxWidth().enter(3))
            }
            BottomNav(Tab.IMPOSTAZIONI, onTab)
        }
    }
}

@Composable
private fun Label(t: String) =
    Text(t.uppercase(), color = Neon.Lilac, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(bottom = 8.dp))

@Composable
private fun Toggle(label: String, on: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = Neon.TextMid, fontSize = 13.5.sp, modifier = Modifier.weight(1f))
        Switch(
            on, onChange,
            colors = SwitchDefaults.colors(
                checkedTrackColor = Neon.Violet, checkedThumbColor = Color.White,
                uncheckedTrackColor = Neon.Track, uncheckedThumbColor = Neon.Text3, uncheckedBorderColor = Neon.Track,
            ),
        )
    }
}

/** Selettore a due opzioni: "Automatica" / "Chiedi ogni volta". */
@Composable
private fun ModeSwitch(mode: CalendarMode, onPick: (CalendarMode) -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Neon.Track).padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        listOf(CalendarMode.AUTO to "Automatica", CalendarMode.CONFIRM to "Chiedi ogni volta").forEach { (m, label) ->
            val on = m == mode
            Text(
                label, color = if (on) Color.White else Neon.Text2, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f).clip(RoundedCornerShape(9.dp))
                    .then(if (on) Modifier.animatedGradient(9.dp) else Modifier)
                    .clickable(role = Role.RadioButton) { onPick(m) }
                    .padding(vertical = 10.dp),
            )
        }
    }
}
