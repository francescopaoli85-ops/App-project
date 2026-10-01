package com.francescopaoli.northstar.ui.screens

import com.francescopaoli.northstar.ui.fx.LocalFx
import com.francescopaoli.northstar.ui.fx.shockwave
import com.francescopaoli.northstar.ui.fx.shimmer
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.francescopaoli.northstar.data.GoalStatus
import com.francescopaoli.northstar.ui.MainViewModel
import com.francescopaoli.northstar.ui.components.GradientButton
import com.francescopaoli.northstar.ui.components.GradientIconTile
import com.francescopaoli.northstar.ui.components.NsIcons
import com.francescopaoli.northstar.ui.components.TextLink
import com.francescopaoli.northstar.ui.fx.BurstRings
import com.francescopaoli.northstar.ui.fx.ConfettiRain
import com.francescopaoli.northstar.ui.fx.NeonBackdrop
import com.francescopaoli.northstar.ui.fx.breathe
import com.francescopaoli.northstar.ui.fx.enter
import com.francescopaoli.northstar.ui.fx.pop
import com.francescopaoli.northstar.ui.fx.twinkle
import com.francescopaoli.northstar.ui.theme.Neon

/** Festa! Coriandoli continui, anelli che esplodono, badge con rimbalzo marcato. */
@Composable
fun CelebrationScreen(vm: MainViewModel, id: String, onAchievements: () -> Unit, onHome: () -> Unit, onUndo: () -> Unit = {}) {
    val goals by vm.goals.collectAsStateWithLifecycle()
    val g = goals.firstOrNull { it.id == id }
    val count = goals.count { it.status == GoalStatus.ACHIEVED }
    // rete di sicurezza per i tocchi sbagliati: "Annulla" resta visibile 8 secondi
    var canUndo by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(true) }
    androidx.compose.runtime.LaunchedEffect(Unit) { kotlinx.coroutines.delay(8_000); canUndo = false }

    val fullFx = LocalFx.current.full
    // l'onda d'urto parte dalla stella e deforma tutto lo schermo, nebulosa compresa
    NeonBackdrop(modifier = Modifier.shockwave(cy = 0.36f), particles = 6, seed = 8) {
        // ogni tema ha la sua festa (neon: fuochi, tramonto: lanterne, oro: coriandoli d'oro…)
        if (fullFx) com.francescopaoli.northstar.ui.fx.ThemeCelebration() else ConfettiRain(Modifier.fillMaxSize())
        Column(Modifier.fillMaxSize().navigationBarsPadding()) {
            Column(
                Modifier.weight(1f).fillMaxWidth().padding(horizontal = 30.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Box(Modifier.size(170.dp), contentAlignment = Alignment.Center) {
                    BurstRings(Modifier.fillMaxSize())
                    GradientIconTile(
                        NsIcons.Star, 92.dp, 28.dp, 44.dp,
                        Modifier.pop(0, -10f).breathe(1.08f, 1500).then(
                            // oro: la stella viene "coniata", con un riflesso metallico che la attraversa
                            if (Neon.Style == com.francescopaoli.northstar.ui.theme.SkyStyle.GOLD) Modifier.shimmer(28.dp, 0.7f, 1700) else Modifier,
                        ),
                    )
                }
                Spacer(Modifier.height(18.dp))
                Text("Obiettivo raggiunto!", color = Color.White, fontSize = 27.sp, fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.enter(0, baseDelayMs = 350))
                Text(
                    "\"${g?.title.orEmpty()}\" — fatto. Da un desiderio vago a un risultato reale.",
                    color = Neon.TextMid, fontSize = 14.sp, lineHeight = 22.sp, textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 10.dp).widthIn(max = 280.dp).enter(1, stepMs = 130, baseDelayMs = 350),
                )
                Row(
                    Modifier.padding(top = 22.dp).enter(2, stepMs = 130, baseDelayMs = 350)
                        .clip(RoundedCornerShape(16.dp)).background(Neon.Surface)
                        .border(1.dp, Neon.Violet.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Icon(NsIcons.Star, null, tint = Color(0xFFFFC107), modifier = Modifier.size(16.dp).twinkle(1400))
                    Text("Nuovo traguardo sbloccato: \"$count ${if (count == 1) "raggiunto" else "raggiunti"}\"", color = Neon.TextMid, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
                }
            }
            Column(
                Modifier.padding(start = 26.dp, end = 26.dp, bottom = 30.dp).enter(3, stepMs = 130, baseDelayMs = 350),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                GradientButton("Vedi i tuoi traguardi", onAchievements, Modifier.fillMaxWidth())
                TextLink("Torna alla home", onHome, Modifier.fillMaxWidth())
                androidx.compose.animation.AnimatedVisibility(canUndo, exit = androidx.compose.animation.fadeOut()) {
                    TextLink("Annulla, non l'ho ancora raggiunto", onUndo, Modifier.fillMaxWidth(), color = Neon.Text3)
                }
            }
        }
    }
}
