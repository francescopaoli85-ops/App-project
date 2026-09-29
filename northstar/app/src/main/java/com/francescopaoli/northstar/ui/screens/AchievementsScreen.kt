package com.francescopaoli.northstar.ui.screens

import com.francescopaoli.northstar.ads.AdPlacement
import com.francescopaoli.northstar.ads.NativeAdCard
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.francescopaoli.northstar.data.GoalStatus
import com.francescopaoli.northstar.domain.SummaryBuilder
import com.francescopaoli.northstar.ui.MainViewModel
import com.francescopaoli.northstar.ui.components.GradientIconTile
import com.francescopaoli.northstar.ui.components.NeonCard
import com.francescopaoli.northstar.ui.components.NsIcons
import com.francescopaoli.northstar.ui.components.RoundIconButton
import androidx.compose.foundation.layout.navigationBarsPadding
import com.francescopaoli.northstar.ui.fx.Blob
import com.francescopaoli.northstar.ui.fx.BlobLayer
import com.francescopaoli.northstar.ui.fx.NeonBackdrop
import com.francescopaoli.northstar.ui.fx.RisingSparks
import com.francescopaoli.northstar.ui.fx.enter
import com.francescopaoli.northstar.ui.fx.pop
import com.francescopaoli.northstar.ui.fx.twinkle
import com.francescopaoli.northstar.ui.theme.Neon
import java.time.Instant
import java.time.ZoneId

@Composable
fun AchievementsScreen(vm: MainViewModel, onBack: () -> Unit) {
    val goals by vm.goals.collectAsStateWithLifecycle()
    val showAds by vm.showAds.collectAsStateWithLifecycle()
    val done = goals.filter { it.status == GoalStatus.ACHIEVED }.sortedByDescending { it.achievedAt ?: 0 }

    NeonBackdrop(blobs = emptyList(), particles = 6, seed = 12) {
        Column(Modifier.fillMaxSize()) {
            // header celebrativo con scintille che salgono
            Box(
                Modifier.fillMaxWidth().background(
                    Brush.linearGradient(listOf(Neon.Surface, Color(0xFF2E1A5C), Neon.SurfaceHi)),
                ),
            ) {
                BlobLayer(listOf(Blob(0f, 0f, 0.5f, Neon.Violet, 0.3f)))
                RisingSparks(Modifier.matchParentSize())
                RoundIconButton(NsIcons.Back, "Indietro", onBack,
                    Modifier.statusBarsPadding().padding(start = 22.dp, top = 24.dp))
                Icon(NsIcons.Sparkle, null, tint = Neon.Cyan, modifier = Modifier.padding(start = 40.dp, top = 50.dp).size(9.dp).twinkle())
                Icon(NsIcons.Sparkle, null, tint = Neon.Lilac, modifier = Modifier.align(Alignment.TopEnd).padding(end = 54.dp, top = 70.dp).size(7.dp).twinkle(2200, 600))
                Column(
                    Modifier.fillMaxWidth().statusBarsPadding().padding(top = 24.dp, bottom = 30.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    GradientIconTile(NsIcons.Trophy, 54.dp, 16.dp, 24.dp)
                    Text("Traguardi", color = Color.White, fontSize = 23.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(top = 12.dp))
                    Text(
                        if (done.isEmpty()) "Il primo è là che ti aspetta" else "${done.size} obiettivi raggiunti finora",
                        color = Neon.Text2, fontSize = 12.5.sp, modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
            LazyColumn(
                Modifier.weight(1f).navigationBarsPadding(),
                contentPadding = PaddingValues(start = 22.dp, end = 22.dp, top = 22.dp, bottom = 40.dp),
                verticalArrangement = Arrangement.spacedBy(11.dp),
            ) {
                if (done.isEmpty()) item {
                    NeonCard(Modifier.fillMaxWidth().enter(0), padding = 20.dp) {
                        Text("Quando segnerai un obiettivo come raggiunto, lo troverai qui a brillare.",
                            color = Neon.Text2, fontSize = 13.sp, lineHeight = 20.sp)
                    }
                }
                val adSlot = if (showAds) AdPlacement.slot(done.size, AdPlacement.ACHIEVEMENTS_AFTER) else null
                itemsIndexed(done, key = { _, g -> g.id }) { i, g ->
                    val date = g.achievedAt?.let { Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate() }
                    NeonCard(Modifier.fillMaxWidth().enter(i, 60), padding = 16.dp) {
                        Box(
                            Modifier.pop(150L + i * 60, -20f).size(42.dp).clip(RoundedCornerShape(13.dp))
                                .background(Brush.linearGradient(Neon.gradient2)),
                            contentAlignment = Alignment.Center,
                        ) { Icon(NsIcons.Trophy, null, tint = Color.White, modifier = Modifier.size(19.dp)) }
                        Column(Modifier.weight(1f)) {
                            Text(g.title, color = Neon.Text, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Text(
                                listOfNotNull(date?.let { SummaryBuilder.formatDate(it) }, g.area.label).joinToString(" · "),
                                color = Neon.Text3, fontSize = 11.sp, modifier = Modifier.padding(top = 2.dp),
                            )
                        }
                    }
                    if (adSlot == i + 1) NativeAdCard(Modifier.fillMaxWidth().padding(top = 11.dp).enter(0, baseDelayMs = 450))
                }
            }
        }
    }
}
