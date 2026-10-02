package com.francescopaoli.northstar.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.border
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
import androidx.compose.runtime.setValue
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
    val powerSave by com.francescopaoli.northstar.ui.fx.rememberPowerSaveMode()
    val activity = androidx.compose.ui.platform.LocalContext.current as android.app.Activity
    val price by vm.removeAdsPrice.collectAsStateWithLifecycle()
    val session = (vm.session.collectAsStateWithLifecycle().value as? SessionState.LoggedIn)?.session

    NeonBackdrop(particles = 6, seed = 14) {
        Column(Modifier.fillMaxSize()) {
            Column(
                Modifier.weight(1f).verticalScroll(rememberScrollState()).statusBarsPadding().padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text("Impostazioni", color = Color.White, style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp))

                // 1 · SUONI: musica ed effetti, ciascuno col suo volume
                LightSection(Modifier.enter(0)) {
                    Label("Suoni")
                    Toggle("Musica", s.musicOn, vm::setMusicOn)
                    if (s.musicOn) {
                            Segmented(
                            com.francescopaoli.northstar.audio.AmbientSong.entries.map { it.id to it.label },
                            s.ambientSong, vm::setAmbientSong,
                        )
                        VolumeSlider("Volume musica", s.musicVolume, onLive = { vm.sound.music.setVolume(it / 100f) }, onCommit = vm::setMusicVolume)
                    }
                    Toggle("Effetti sonori", s.sfxOn, vm::setSfxOn)
                    if (s.sfxOn) VolumeSlider(
                        "Volume effetti", s.sfxVolume,
                        onLive = { vm.sound.sfx.level = it / 100f },
                        onCommit = { vm.setSfxVolume(it); vm.sound.sfx.tap() }, // anteprima del volume scelto
                    )
                }

                // 2 · ASPETTO: tema e movimento dello sfondo
                LightSection(Modifier.enter(1)) {
                    Label("Aspetto")
                    ThemePicker(s.theme, vm::setTheme)
                    // stato reale: col risparmio energetico lo sfondo può essere fermo anche se il tema è animato
                    val stoppedBySaver = powerSave && !s.animateOnPowerSave
                    Toggle("Sfondo animato", s.theme !in s.staticThemes && !stoppedBySaver) { on ->
                        vm.setThemeAnimated(s.theme, on)
                        if (on && stoppedBySaver) vm.setAnimateOnPowerSave(true)
                    }
                    if (stoppedBySaver) Hint("Fermo per il risparmio energetico: accendilo per farlo ripartire.")
                    // il resto, raccolto: si apre solo se serve
                    var more by androidx.compose.runtime.saveable.rememberSaveable { androidx.compose.runtime.mutableStateOf(false) }
                    Text(
                        if (more) "Meno opzioni  ▴" else "Altre opzioni  ▾", color = Neon.Cyan, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable { more = !more }.padding(vertical = 8.dp),
                    )
                    androidx.compose.animation.AnimatedVisibility(more) {
                        Column {
                            Toggle("Fermalo col risparmio energetico", !s.animateOnPowerSave) { vm.setAnimateOnPowerSave(!it) }
                            Toggle("Effetto giroscopio", s.parallaxOn, vm::setParallax)
                            Toggle("Effetti ridotti", s.reducedEffects, vm::setReducedEffects)
                        }
                    }
                }

                // 3 · GUIDA E PROMEMORIA
                LightSection(Modifier.enter(2)) {
                    Label("Guida e promemoria")
                    Toggle("Voce guida", s.voiceGuide, vm::setVoice)
                    Toggle("Check-in periodici", s.checkins, vm::setCheckins)
                }

                LightSection(Modifier.enter(3)) {
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

                LightSection(Modifier.enter(4)) {
                    Label("Pubblicità")
                    if (s.adFree) {
                        Text("Pubblicità rimossa ✦ Grazie per il supporto!", color = Neon.Cyan, fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
                    } else {
                        Text(
                            "Qualche card sponsorizzata tiene l'app gratuita. Puoi toglierle per sempre con un solo acquisto.",
                            color = Neon.Text2, fontSize = 12.5.sp, lineHeight = 19.sp, modifier = Modifier.padding(bottom = 12.dp),
                        )
                        com.francescopaoli.northstar.ui.components.GradientButton(
                            price?.let { "Rimuovi pubblicità · $it" } ?: "Rimuovi pubblicità",
                            { vm.buyRemoveAds(activity) }, Modifier.fillMaxWidth(), glowing = false,
                        )
                        TextLink("Ripristina acquisto", vm::restorePurchases, Modifier.fillMaxWidth(), color = Neon.Text3)
                    }
                    if (vm.privacyOptionsRequired) TextLink("Preferenze privacy annunci", { vm.showPrivacyOptions(activity) },
                        Modifier.fillMaxWidth(), color = Neon.Text3)
                }

                // ACCOUNT in fondo, con l'uscita
                LightSection(Modifier.enter(5)) {
                    Label("Account")
                    Text(session?.name.orEmpty(), color = Neon.Text, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text(
                        when (session) {
                            is Session.Cloud -> session.email ?: "Account Google"
                            else -> "Modalità locale · dati solo su questo telefono"
                        },
                        color = Neon.Text2, fontSize = 12.sp,
                    )
                    GhostButton("Esci", vm::signOut, Modifier.fillMaxWidth().padding(top = 12.dp))
                }
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

/** Sezione leggera: titolino e righe, senza card pesanti. */
@Composable
private fun LightSection(modifier: Modifier = Modifier, content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    Column(
        modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Neon.Surface.copy(alpha = 0.35f))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        content = content,
    )
}

@Composable
private fun SubLabel(t: String) =
    Text(t, color = Neon.Text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 10.dp, bottom = 8.dp))

@Composable
private fun Hint(t: String) =
    Text(t, color = Neon.Text3, fontSize = 11.sp, lineHeight = 15.sp, modifier = Modifier.padding(top = 6.dp))

/** Cursore di volume: si sente subito mentre lo muovi, si salva quando lo lasci. */
@Composable
private fun VolumeSlider(label: String, value: Int, onLive: (Float) -> Unit, onCommit: (Int) -> Unit) {
    var v by androidx.compose.runtime.remember(value) { androidx.compose.runtime.mutableFloatStateOf(value.toFloat()) }
    Row(Modifier.fillMaxWidth().padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = Neon.TextMid, fontSize = 13.5.sp, modifier = Modifier.weight(1f))
        Text("${v.toInt()}%", color = Neon.Text2, fontSize = 12.sp)
    }
    androidx.compose.material3.Slider(
        value = v,
        onValueChange = { v = it; onLive(it) },
        onValueChangeFinished = { onCommit(v.toInt()) },
        valueRange = 0f..100f,
        colors = androidx.compose.material3.SliderDefaults.colors(
            thumbColor = Neon.Cyan, activeTrackColor = Neon.Violet, inactiveTrackColor = Neon.Track,
        ),
    )
}

/** Selettore a più opzioni (id, etichetta), stesso stile di [ModeSwitch]. */
@Composable
private fun Segmented(options: List<Pair<String, String>>, selected: String, onPick: (String) -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Neon.Track).padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        options.forEach { (id, label) ->
            val on = id == selected
            Text(
                label, color = if (on) Neon.OnAccent else Neon.Text2, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f).clip(RoundedCornerShape(9.dp))
                    .then(if (on) Modifier.animatedGradient(9.dp) else Modifier)
                    .clickable(role = Role.RadioButton) { onPick(id) }
                    .padding(vertical = 10.dp),
            )
        }
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

/** Temi come una fila leggera di cerchi colorati: tocchi e tutta l'app si ricolora subito. */
@Composable
private fun ThemePicker(selected: String, onPick: (String) -> Unit) {
    Row(Modifier.fillMaxWidth()) {
        com.francescopaoli.northstar.ui.theme.Palettes.all.forEach { p ->
            val on = p.id == selected
            Column(
                Modifier.weight(1f).clip(RoundedCornerShape(12.dp))
                    .clickable(role = Role.RadioButton, onClickLabel = p.name) { onPick(p.id) }
                    .padding(vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                androidx.compose.foundation.layout.Box(
                    Modifier.size(40.dp).clip(androidx.compose.foundation.shape.CircleShape)
                        .background(androidx.compose.ui.graphics.Brush.linearGradient(listOf(p.accent1, p.accent2)))
                        .border(if (on) 3.dp else 1.dp, if (on) Color.White else p.track, androidx.compose.foundation.shape.CircleShape),
                )
                Text(
                    p.name.substringBefore(" "), color = if (on) Neon.Text else Neon.Text3, fontSize = 11.sp, maxLines = 1,
                    fontWeight = if (on) FontWeight.Bold else FontWeight.Normal, modifier = Modifier.padding(top = 6.dp),
                )
            }
        }
    }
}
