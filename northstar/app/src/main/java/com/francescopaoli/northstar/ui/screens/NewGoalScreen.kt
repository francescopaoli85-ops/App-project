package com.francescopaoli.northstar.ui.screens

import com.francescopaoli.northstar.ui.fx.LiquidOrb
import com.francescopaoli.northstar.ui.fx.LocalFx
import com.francescopaoli.northstar.ui.fx.WarpOverlay
import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.francescopaoli.northstar.data.Area
import com.francescopaoli.northstar.data.Criterion
import com.francescopaoli.northstar.domain.SummaryBuilder
import com.francescopaoli.northstar.ui.MainViewModel
import com.francescopaoli.northstar.ui.components.Chip
import com.francescopaoli.northstar.ui.components.GhostButton
import com.francescopaoli.northstar.ui.components.GradientButton
import com.francescopaoli.northstar.ui.components.GradientIconTile
import com.francescopaoli.northstar.ui.components.NeonCard
import com.francescopaoli.northstar.ui.components.NeonColumnCard
import com.francescopaoli.northstar.ui.components.NsIcons
import com.francescopaoli.northstar.ui.components.RoundIconButton
import com.francescopaoli.northstar.ui.components.TextLink
import com.francescopaoli.northstar.ui.fx.NeonBackdrop
import com.francescopaoli.northstar.ui.fx.PulseRings
import com.francescopaoli.northstar.ui.fx.VoiceWave
import com.francescopaoli.northstar.ui.fx.animatedGradient
import com.francescopaoli.northstar.ui.fx.breathe
import com.francescopaoli.northstar.ui.fx.enter
import com.francescopaoli.northstar.ui.fx.glow
import com.francescopaoli.northstar.ui.fx.pop
import com.francescopaoli.northstar.ui.fx.spin
import com.francescopaoli.northstar.ui.theme.Neon
import kotlinx.coroutines.launch

@Composable
fun NewGoalScreen(vm: MainViewModel, onClose: () -> Unit, onCreated: (firstGoal: Boolean) -> Unit) {
    val ng: NewGoalViewModel = viewModel()
    val ctx = LocalContext.current
    val settings by vm.settings.collectAsStateWithLifecycle()
    val voiceState by ng.voice.state.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var saving by remember { mutableStateOf(false) }
    var pickDate by remember { mutableStateOf(false) }
    val fullFx = LocalFx.current.full
    // salvataggio in corso + "salto nell'iperspazio" (si naviga quando finiscono entrambi)
    var warp by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }
    var firstGoal by remember { mutableStateOf(false) }
    fun hasMic() = ContextCompat.checkSelfPermission(ctx, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED

    // microfono: se negato si passa alla modalità manuale
    val micPerm = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        if (!ok) { ng.chooseTyping(true); vm.setPreferTyping(true) }
        ng.start()
    }
    // le scelte dell'ultima volta (modo di rispondere, voce guida)
    LaunchedEffect(Unit) {
        ng.voiceGuideOn = settings.voiceGuide
        ng.chooseTyping(settings.preferTyping || !voiceState.available)
    }
    // ogni nuova domanda: la voce guida la legge (a voce serve il microfono autorizzato)
    LaunchedEffect(ng.step, ng.preparing) {
        if (!ng.preparing && (ng.typing || hasMic())) ng.enterStep()
    }
    DisposableEffect(Unit) {
        onDispose { ng.voice.silence(); vm.sound.music.setVoice(false, false); vm.sound.music.setTyping(false) }
    }
    // musica: appena entri parte la canzone delle domande; ogni passo aggiunge strumenti fino all'apoteosi (7)
    LaunchedEffect(ng.step, ng.preparing) {
        vm.sound.music.setScene(com.francescopaoli.northstar.audio.Scene.Flow(if (ng.preparing) 1 else ng.step + 1))
    }
    // mentre scrivi la musica si abbassa: concentrazione
    LaunchedEffect(ng.typing, ng.preparing) { vm.sound.music.setTyping(ng.typing && !ng.preparing) }
    // a ogni passo: nota d'arpa che sale; al riepilogo l'arpeggio della vittoria e la festa
    var lastStep by remember { mutableStateOf(ng.step) }
    var celebrate by remember { mutableStateOf(false) }
    LaunchedEffect(ng.step) {
        if (ng.step > lastStep) {
            if (ng.isSummary) {
                vm.sound.sfx.step(6)
                celebrate = true
                kotlinx.coroutines.delay(350)
                vm.sound.sfx.success()
            } else vm.sound.sfx.step(ng.step)
        }
        if (!ng.isSummary) celebrate = false
        lastStep = ng.step
    }
    // quando la guida parla o ascolta, la musica si abbassa
    LaunchedEffect(voiceState.speaking, voiceState.listening) {
        vm.sound.music.setVoice(voiceState.speaking, voiceState.listening)
    }
    // app in secondo piano (tasto Home, altra app, schermo spento): la voce si ferma subito
    androidx.lifecycle.compose.LifecycleStartEffect(ng) {
        ng.voice.resume()
        onStopOrDispose { ng.voice.pause() }
    }
    val setTyping: (Boolean) -> Unit = { t ->
        vm.setPreferTyping(t)
        if (!t && !hasMic()) micPerm.launch(Manifest.permission.RECORD_AUDIO).also { ng.chooseTyping(false) }
        else ng.chooseTyping(t)
    }

    NeonBackdrop(particles = 6, seed = 9) {
        Column(Modifier.fillMaxSize().imePadding()) {
            HeaderBand {
                TopRow(
                    start = { RoundIconButton(NsIcons.Close, "Chiudi", onClose) },
                    end = {
                        Text(
                            when {
                                ng.preparing -> "PREPARAZIONE"
                                ng.isSummary -> "ECCOLO ✦"
                                else -> "PASSO ${ng.step + 1} DI 6"
                            },
                            color = Neon.Text2, fontSize = 11.sp, fontWeight = FontWeight.Bold,
                        )
                    },
                )
                if (!ng.preparing) {
                    // costellazione: ogni risposta accende una stella, il riepilogo la Stella Polare
                    ConstellationProgress(ng.step, Modifier.padding(top = 10.dp))
                    if (!ng.isSummary) AnswerModeSwitch(ng.typing, voiceState.available, setTyping, Modifier.padding(top = 10.dp))
                    // interruttori rapidi: musica e voce guida, senza passare dalle impostazioni
                    Row(Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        QuickToggle("♪  Musica", settings.musicOn) { vm.setMusicOn(it) }
                        QuickToggle("🗣  Voce guida", settings.voiceGuide) { on ->
                            vm.setVoice(on)
                            ng.voiceGuideOn = on
                            if (on) ng.enterStep() else ng.voice.silence()
                        }
                    }
                }
            }

            Box(Modifier.weight(1f)) {
                if (ng.preparing) {
                    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 20.dp)) {
                        PrepStep(
                            area = ng.area, onArea = { ng.area = it },
                            typing = ng.typing, voiceAvailable = voiceState.available, onTyping = { t -> vm.setPreferTyping(t); ng.chooseTyping(t) },
                            musicOn = settings.musicOn, onMusic = vm::setMusicOn,
                            voiceGuide = settings.voiceGuide, onVoiceGuide = { vm.setVoice(it); ng.voiceGuideOn = it },
                        )
                    }
                } else AnimatedContent(
                    targetState = ng.step,
                    transitionSpec = {
                        val dir = if (targetState > initialState) 1 else -1
                        (slideInHorizontally(tween(420)) { it / 4 * dir } + fadeIn(tween(420)))
                            .togetherWith(slideOutHorizontally(tween(300)) { -it / 4 * dir } + fadeOut(tween(250)))
                    },
                    modifier = Modifier.fillMaxSize(),
                    label = "step",
                ) { step ->
                    val c = Criterion.entries.getOrNull(step)
                    Column(
                        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 26.dp, vertical = 22.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        if (c == null) SummaryStep(ng)
                        else {
                            QuestionHeader(c)
                            PreviousAnswer(ng, step)
                            if (c == Criterion.CONTESTUALIZZATO) {
                                DateCard(ng) { pickDate = true }
                                QuickDates(java.time.LocalDate.now(), onPick = { ng.deadline = it }, onCustom = { pickDate = true })
                            }
                            if (ng.typing) {
                                NeonTextField(
                                    ng.answers[c].orEmpty(), ng::setAnswer,
                                    if (c == Criterion.CONTESTUALIZZATO) "Dove, con chi… (facoltativo)" else "Scrivi qui la tua risposta",
                                    minLines = 3, modifier = Modifier.enter(2),
                                )
                            } else {
                                VoiceInput(
                                    answer = ng.answers[c].orEmpty(),
                                    speaking = voiceState.speaking,
                                    listening = voiceState.listening,
                                    partial = voiceState.partial,
                                    level = voiceState.level,
                                    onMic = ng::micTap,
                                )
                            }
                        }
                    }
                }
                // suggerimento fluttuante (una volta sola), sopra il contenuto
                if (!ng.preparing && !ng.isSummary && !ng.typing) CoachBubble(
                    "flow_auto", "Rispondi a voce: dopo 3 secondi passo da solo alla domanda dopo.",
                    settings.seenHints, { vm.hintSeen(it) }, Modifier.align(Alignment.BottomCenter).padding(16.dp),
                )
            }

            // barra in basso: indietro / avanti (o "Iniziamo" in preparazione)
            Column(Modifier.navigationBarsPadding().padding(start = 24.dp, end = 24.dp, bottom = 16.dp, top = 6.dp)) {
                // conto alla rovescia dell'avanti automatico, annullabile
                androidx.compose.animation.AnimatedVisibility(ng.autoNextIn > 0) {
                    Row(
                        Modifier.fillMaxWidth().padding(bottom = 6.dp),
                        horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("Avanti tra ${ng.autoNextIn}…", color = Neon.Cyan, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        TextLink("Annulla", ng::cancelAutoNext, color = Neon.Text2)
                    }
                }
                if (ng.preparing) {
                    GradientButton(
                        "Iniziamo ✦",
                        {
                            if (!ng.typing && !hasMic()) micPerm.launch(Manifest.permission.RECORD_AUDIO) else ng.start()
                        },
                        Modifier.fillMaxWidth(), trailing = NsIcons.Arrow,
                    )
                } else Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (ng.step > 0) GhostButton("Indietro", ng::back, Modifier.weight(1f))
                    GradientButton(
                        text = when {
                            ng.isSummary -> "Salva obiettivo"
                            ng.step == 5 -> "Vedi il riepilogo"
                            else -> "Avanti"
                        },
                        onClick = {
                            if (!ng.isSummary) ng.next()
                            else if (!saving) {
                                saving = true
                                val first = vm.goals.value.isEmpty()
                                val job = scope.launch { vm.createGoal(ng.area, ng.answers.toMap(), ng.deadline, ng.revisions) }
                                if (fullFx) { firstGoal = first; warp = job }
                                else scope.launch { job.join(); onCreated(first) }
                            }
                        },
                        modifier = Modifier.weight(1.4f),
                        enabled = ng.canContinue() && !saving,
                        tapSound = false,
                    )
                }
            }
        }
        // lampo di luce a ogni passo avanti
        StepFlash(if (ng.preparing) -1 else ng.step, Modifier.fillMaxSize())
        // il riepilogo è una festa: la stessa del tema, sopra a tutto
        if (celebrate) {
            if (fullFx) com.francescopaoli.northstar.ui.fx.ThemeCelebration()
            else com.francescopaoli.northstar.ui.fx.ConfettiRain(Modifier.fillMaxSize())
        }
        warp?.let { job ->
            WarpOverlay(onFinished = { scope.launch { job.join(); onCreated(firstGoal) } })
        }
    }

    if (pickDate) NeonDatePicker(ng.deadline, { ng.deadline = it; pickDate = false }, { pickDate = false })
}

/** Pillola on/off nell'intestazione del percorso. */
@Composable
private fun QuickToggle(label: String, on: Boolean, onChange: (Boolean) -> Unit) {
    Text(
        label + if (on) "  ·  sì" else "  ·  no",
        color = if (on) Neon.Text else Neon.Text3,
        fontSize = 11.5.sp, fontWeight = FontWeight.Bold,
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (on) Neon.Violet.copy(alpha = 0.28f) else Color.Transparent)
            .border(1.dp, if (on) Neon.Violet.copy(alpha = 0.6f) else Neon.Track, RoundedCornerShape(20.dp))
            .clickable(role = Role.Switch, onClickLabel = label) { onChange(!on) }
            .padding(horizontal = 12.dp, vertical = 6.dp),
    )
}

@Composable
private fun QuestionHeader(c: Criterion) {
    Column {
        Chip(c.label, Modifier.pop().breathe(1.05f, 2200))
        Text(c.question, color = Color.White, style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(top = 15.dp).enter(0))
        Text(c.hint, color = Neon.Text2, fontSize = 13.sp, lineHeight = 20.sp, modifier = Modifier.padding(top = 8.dp).enter(1))
    }
}

/** Promemoria della risposta precedente, come nel mockup ("✓ Specifico: ..."). */
@Composable
private fun PreviousAnswer(ng: NewGoalViewModel, step: Int) {
    val prev = Criterion.entries.getOrNull(step - 1) ?: return
    val text = ng.answers[prev].orEmpty().ifBlank { return }
    NeonCard(Modifier.fillMaxWidth().enter(2), corner = 12.dp, padding = 12.dp) {
        Icon(NsIcons.Check, null, tint = Neon.Cyan, modifier = Modifier.size(14.dp))
        Text("${prev.label}: $text", color = Neon.TextMid, fontSize = 12.5.sp, maxLines = 2)
    }
}

@Composable
private fun DateCard(ng: NewGoalViewModel, onClick: () -> Unit) {
    NeonCard(Modifier.fillMaxWidth().enter(2), onClick = onClick) {
        GradientIconTile(NsIcons.Calendar, 40.dp, 12.dp, 18.dp, floating = false)
        Column(Modifier.weight(1f)) {
            Text("Scadenza", color = Neon.Text2, fontSize = 11.sp)
            Text(SummaryBuilder.formatDate(ng.deadline), color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
        }
        Text("Cambia", color = Neon.Cyan, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

/** Microfono centrale con onde pulsanti + stato della conversazione. */
@Composable
private fun VoiceInput(
    answer: String,
    speaking: Boolean,
    listening: Boolean,
    partial: String,
    level: Float,
    onMic: () -> Unit,
) {
    Column(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(150.dp), contentAlignment = Alignment.Center) {
            PulseRings(listening || speaking, Modifier.fillMaxSize())
            Canvas(Modifier.size(116.dp).spin(20000)) {
                drawCircle(Neon.Inactive.copy(alpha = 0.6f), style = Stroke(1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 14f))))
            }
            // sfera liquida: si deforma con la voce
            LiquidOrb(level, listening, Modifier.size(150.dp))
            Box(
                Modifier
                    .size(84.dp)
                    .clip(CircleShape)
                    .clickable(role = Role.Button, onClickLabel = "Parla", onClick = onMic),
                contentAlignment = Alignment.Center,
            ) { Icon(NsIcons.Mic, "Microfono", tint = Color.White, modifier = Modifier.size(28.dp)) }
        }
        Spacer(Modifier.height(10.dp))
        Row(
            Modifier.clip(RoundedCornerShape(20.dp)).background(Neon.Violet.copy(alpha = 0.18f)).padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            VoiceWave(level, listening || speaking, Modifier.width(33.dp).height(16.dp))
            Spacer(Modifier.width(10.dp))
            Text(
                when {
                    speaking -> "Ti sto parlando… interrompimi pure"
                    listening -> "Ti sto ascoltando…"
                    answer.isNotBlank() -> "Tocca il microfono per rifarla"
                    else -> "Tocca il microfono e parla"
                },
                color = Neon.Lilac, fontSize = 12.sp, fontWeight = FontWeight.Bold,
            )
        }
        val shown = partial.ifBlank { answer }
        if (shown.isNotBlank()) {
            Text(
                "“$shown”", color = if (partial.isNotBlank()) Neon.Text2 else Neon.TextSoft,
                fontSize = 15.sp, fontStyle = FontStyle.Italic, lineHeight = 22.sp,
                modifier = Modifier.padding(top = 18.dp).enter(0),
            )
        }
    }
}

/**
 * Ultimo passo: la frase riassuntiva. È il momento della ricompensa:
 * la Stella Polare si accende con i raggi, la frase compare parola per parola, la musica è all'apoteosi.
 */
@Composable
private fun SummaryStep(ng: NewGoalViewModel) {
    val summary = SummaryBuilder.build(ng.answers.toMap(), ng.deadline)
    val words = remember(summary) { summary.split(" ") }
    var shown by remember(summary) { mutableStateOf(0) }
    LaunchedEffect(summary) {
        kotlinx.coroutines.delay(600)
        while (shown < words.size) { shown++; kotlinx.coroutines.delay(70) }
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Box(contentAlignment = Alignment.Center) {
            com.francescopaoli.northstar.ui.fx.StarRays(Modifier.size(170.dp), alpha = 0.7f)
            GradientIconTile(NsIcons.Star, 76.dp, 24.dp, 34.dp, Modifier.pop(0, -20f).glow(24.dp, blur = 22.dp, durationMs = 1600))
        }
        Text("Ecco la tua Stella Polare", color = Color.White, style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.enter(1))
        Text("Da un desiderio a una meta vera: ce l'hai fatta, era il passo più difficile.", color = Neon.Text2, fontSize = 13.sp,
            textAlign = TextAlign.Center, modifier = Modifier.padding(top = 6.dp).enter(2))
    }
    NeonColumnCard(Modifier.fillMaxWidth().enter(3, stepMs = 120).glow(18.dp, blur = 16.dp, durationMs = 2400)) {
        Icon(NsIcons.Quote, null, tint = Neon.Cyan.copy(alpha = 0.7f), modifier = Modifier.size(26.dp, 20.dp))
        // la frase si scrive davanti ai tuoi occhi
        Text(words.take(shown).joinToString(" "), color = Neon.TextSoft, fontSize = 16.sp, lineHeight = 25.sp, fontStyle = FontStyle.Italic,
            modifier = Modifier.padding(top = 8.dp))
    }
    Text("${ng.area.label} · le 6 risposte restano nel dettaglio dell'obiettivo.", color = Neon.Text3, fontSize = 12.sp, modifier = Modifier.enter(4))
}

