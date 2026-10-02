package com.francescopaoli.northstar.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.francescopaoli.northstar.ui.theme.Neon

/**
 * Suggerimento al primo uso: un fumetto piccolo che spiega una funzione.
 * Compare finché non tocchi "Ho capito", poi non torna più.
 */
@Composable
fun HintBubble(key: String, text: String, seen: Set<String>, onSeen: (String) -> Unit, modifier: Modifier = Modifier) {
    AnimatedVisibility(key !in seen, modifier, enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(Neon.Cyan.copy(alpha = 0.10f))
                .border(1.dp, Neon.Cyan.copy(alpha = 0.40f), RoundedCornerShape(14.dp))
                .padding(start = 14.dp, end = 6.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("✦  $text", color = Neon.Text, fontSize = 12.5.sp, lineHeight = 18.sp, modifier = Modifier.weight(1f))
            Text(
                "Ho capito", color = Neon.Cyan, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.clip(RoundedCornerShape(10.dp)).clickable { onSeen(key) }.padding(8.dp),
            )
        }
    }
}

/**
 * Suggerimento fluttuante: una nuvoletta sopra il contenuto (non in mezzo alla lista),
 * con ombra e bordo luminoso; sparisce con "Ho capito" e non torna più.
 */
@Composable
fun CoachBubble(key: String, text: String, seen: Set<String>, onSeen: (String) -> Unit, modifier: Modifier = Modifier) {
    AnimatedVisibility(
        key !in seen, modifier,
        enter = fadeIn() + androidx.compose.animation.slideInVertically { it / 2 },
        exit = fadeOut() + androidx.compose.animation.slideOutVertically { it / 2 },
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .androidxShadow()
                .clip(RoundedCornerShape(18.dp))
                .background(Neon.SurfaceHi)
                .border(1.dp, Neon.Cyan.copy(alpha = 0.5f), RoundedCornerShape(18.dp))
                .padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text("💡", fontSize = 18.sp)
            Text(text, color = Neon.Text, fontSize = 13.sp, lineHeight = 18.sp, modifier = Modifier.weight(1f))
            Text(
                "Ho capito", color = Neon.Cyan, fontSize = 12.5.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.clip(RoundedCornerShape(10.dp)).clickable { onSeen(key) }.padding(8.dp),
            )
        }
    }
}

private fun Modifier.androidxShadow() = this.then(
    Modifier.shadow(16.dp, RoundedCornerShape(18.dp), ambientColor = Neon.Cyan, spotColor = Neon.Violet),
)

/** Finestra rapida per scrivere un testo (nuovo passo, modifica azione) senza cambiare schermata. */
@Composable
fun QuickTextDialog(
    title: String,
    initial: String = "",
    placeholder: String,
    confirm: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var text by rememberSaveable { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Neon.Surface,
        title = { Text(title, color = Neon.Text, fontSize = 17.sp, fontWeight = FontWeight.Bold) },
        text = { NeonTextField(text, { text = it }, placeholder, singleLine = true) },
        confirmButton = {
            TextButton({ if (text.isNotBlank()) onConfirm(text.trim()) }, enabled = text.isNotBlank()) {
                Text(confirm, color = if (text.isNotBlank()) Neon.Cyan else Neon.Text3, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = { TextButton(onDismiss) { Text("Annulla", color = Neon.Text2) } },
    )
}
