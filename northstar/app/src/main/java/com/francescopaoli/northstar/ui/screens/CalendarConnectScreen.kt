package com.francescopaoli.northstar.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.francescopaoli.northstar.ui.MainViewModel
import com.francescopaoli.northstar.ui.components.GradientButton
import com.francescopaoli.northstar.ui.components.GradientIconTile
import com.francescopaoli.northstar.ui.components.NeonCard
import com.francescopaoli.northstar.ui.components.NsIcons
import com.francescopaoli.northstar.ui.components.RoundIconButton
import com.francescopaoli.northstar.ui.components.TextLink
import com.francescopaoli.northstar.ui.fx.Blob
import com.francescopaoli.northstar.ui.fx.NeonBackdrop
import com.francescopaoli.northstar.ui.fx.enter
import com.francescopaoli.northstar.ui.fx.pop
import com.francescopaoli.northstar.ui.fx.rock
import com.francescopaoli.northstar.ui.theme.Neon
import kotlinx.coroutines.launch

/** Proposta di sincronizzazione con Google Calendar (mai all'avvio: solo dopo il primo obiettivo). */
@Composable
fun CalendarConnectScreen(vm: MainViewModel, onDone: () -> Unit) {
    val scope = rememberCoroutineScope()
    val settings by vm.settings.collectAsStateWithLifecycle()
    val consent = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { res ->
        vm.onCalendarAuthResult(res.data)
    }
    // appena collegato si torna indietro
    LaunchedEffect(settings.calendarConnected) { if (settings.calendarConnected) onDone() }

    NeonBackdrop(blobs = listOf(Blob(1f, 0f, 0.62f, Neon.Cyan, 0.35f, 3f)), particles = 8, seed = 3) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            Row(Modifier.fillMaxWidth().padding(22.dp), horizontalArrangement = Arrangement.End) {
                RoundIconButton(NsIcons.Close, "Chiudi", { vm.calendarPromptSeen(); onDone() })
            }
            Column(Modifier.weight(1f).padding(horizontal = 28.dp)) {
                GradientIconTile(NsIcons.Calendar, 66.dp, 20.dp, 30.dp, Modifier.enter(0), iconModifier = Modifier.rock())
                Spacer(Modifier.height(22.dp))
                Text("Northstar funziona meglio\ncon Google Calendar", color = Color.White, fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold, lineHeight = 30.sp, modifier = Modifier.enter(0))
                Text(
                    "Collega il calendario e le scadenze dei tuoi obiettivi diventano promemoria puntuali, senza doverli riscrivere a mano.",
                    color = Neon.Text2, fontSize = 13.5.sp, lineHeight = 21.sp, modifier = Modifier.padding(top = 10.dp).enter(1),
                )
                Column(Modifier.padding(top = 24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    listOf(
                        "Scadenze aggiunte in automatico",
                        "Promemoria coordinati con la tua agenda reale",
                        "Puoi scollegarlo quando vuoi dalle impostazioni",
                    ).forEachIndexed { i, t ->
                        NeonCard(Modifier.fillMaxWidth().enter(i, 80, 280), corner = 14.dp, padding = 13.dp) {
                            Icon(NsIcons.Check, null, tint = Neon.Cyan, modifier = Modifier.size(16.dp).pop(400L + i * 80))
                            Text(t, color = Neon.TextSoft, fontSize = 12.5.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
            Column(
                Modifier.padding(start = 26.dp, end = 26.dp, bottom = 30.dp).enter(2),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                GradientButton("Collega ora", {
                    scope.launch {
                        vm.beginCalendarAuth()?.let { consent.launch(IntentSenderRequest.Builder(it.intentSender).build()) }
                    }
                }, Modifier.fillMaxWidth())
                TextLink("Più tardi", { vm.calendarPromptSeen(); onDone() }, Modifier.fillMaxWidth())
            }
        }
    }
}
