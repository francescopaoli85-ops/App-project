package com.francescopaoli.northstar.ui.screens

import android.app.Activity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.francescopaoli.northstar.ui.MainViewModel
import com.francescopaoli.northstar.ui.components.GradientButton
import com.francescopaoli.northstar.ui.components.GradientIconTile
import com.francescopaoli.northstar.ui.components.NsIcons
import com.francescopaoli.northstar.ui.components.TextLink
import com.francescopaoli.northstar.ui.fx.Blob
import com.francescopaoli.northstar.ui.fx.NeonBackdrop
import com.francescopaoli.northstar.ui.fx.OrbitStars
import com.francescopaoli.northstar.ui.fx.enter
import com.francescopaoli.northstar.ui.fx.glow
import com.francescopaoli.northstar.ui.fx.shimmer
import com.francescopaoli.northstar.ui.theme.Neon

@Composable
fun LoginScreen(vm: MainViewModel) {
    val activity = LocalContext.current as Activity
    var busy by remember { mutableStateOf(false) }
    var showEmail by rememberSaveable { mutableStateOf(false) }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var name by rememberSaveable { mutableStateOf("") }

    NeonBackdrop(
        blobs = listOf(
            Blob(0.05f, 0.02f, 0.62f, Neon.Violet, 0.55f, 3f),
            Blob(1.0f, 1.0f, 0.68f, Neon.Cyan, 0.4f, 3.8f),
        ),
        particles = 22, // tante particelle ben distribuite nell'apertura
        seed = 5,
    ) {
        OrbitStars(Modifier.fillMaxWidth().height(420.dp).padding(top = 60.dp))

        Column(Modifier.fillMaxSize().imePadding().navigationBarsPadding()) {
            Column(
                Modifier.weight(1f).fillMaxWidth().padding(horizontal = 34.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                GradientIconTile(
                    NsIcons.Star, 72.dp, 22.dp, 32.dp,
                    modifier = Modifier.enter(0),
                )
                Spacer(Modifier.height(26.dp))
                Text("Northstar", color = Color.White, fontSize = 36.sp, fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.enter(1, stepMs = 120))
                Box(
                    Modifier.enter(1, stepMs = 120).padding(vertical = 16.dp).width(44.dp).height(3.dp)
                        .clip(RoundedCornerShape(2.dp)).background(Brush.horizontalGradient(Neon.gradient2)),
                )
                Text(
                    "Un desiderio vago diventa\nun obiettivo chiaro, con data\ne direzione.",
                    color = Neon.TextMid, fontSize = 14.sp, lineHeight = 22.sp, textAlign = TextAlign.Center,
                    modifier = Modifier.enter(2, stepMs = 120),
                )
            }

            Column(
                Modifier.enter(3, stepMs = 120).padding(start = 26.dp, end = 26.dp, bottom = 34.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (vm.firebaseEnabled) {
                    // accesso rapido con Google: priorità visiva, un solo tocco
                    GoogleButton(busy) {
                        busy = true
                        vm.signInGoogle(activity) { busy = false }
                    }
                    AnimatedVisibility(showEmail, enter = fadeIn() + expandVertically()) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            NeonTextField(email, { email = it }, "Email", singleLine = true)
                            NeonTextField(password, { password = it }, "Password (min 6 caratteri)", singleLine = true, password = true)
                            GradientButton("Entra", {
                                busy = true
                                vm.signInEmail(email, password) { busy = false }
                            }, Modifier.fillMaxWidth(), enabled = !busy && email.contains('@') && password.length >= 6)
                        }
                    }
                    if (!showEmail) TextLink("Usa un'altra email", { showEmail = true }, Modifier.fillMaxWidth())
                } else {
                    // Modalità locale: Firebase non ancora configurato
                    NeonTextField(name, { name = it }, "Come ti chiami?", singleLine = true)
                    GradientButton("Inizia", { vm.enterLocal(name) }, Modifier.fillMaxWidth())
                    Text(
                        "Modalità locale: i dati restano su questo telefono.\nLogin Google e sync arrivano configurando Firebase.",
                        color = Neon.Text3, fontSize = 11.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

@Composable
private fun GoogleButton(busy: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .glow(14.dp, blur = 20.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White)
            .shimmer(14.dp, 0.5f)
            .clickable(enabled = !busy, role = Role.Button, onClick = onClick)
            .padding(15.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (busy) CircularProgressIndicator(Modifier.size(18.dp), color = Neon.Violet, strokeWidth = 2.dp)
        else androidx.compose.foundation.Image(NsIcons.Google, "Google", Modifier.size(18.dp))
        Spacer(Modifier.width(10.dp))
        Text("Continua con Google", color = Color(0xFF17102E), fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
    }
}
