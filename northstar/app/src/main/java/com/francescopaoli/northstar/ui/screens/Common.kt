package com.francescopaoli.northstar.ui.screens

import dev.chrisbanes.haze.hazeChild
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import com.francescopaoli.northstar.ui.fx.twinkle
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.francescopaoli.northstar.ui.fx.Blob
import com.francescopaoli.northstar.ui.fx.BlobLayer
import com.francescopaoli.northstar.ui.fx.ParticleField
import com.francescopaoli.northstar.ui.theme.Neon
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/** Fascia in alto con gradiente scuro, blob e particelle (Nuovo obiettivo, Dettaglio, Check-in). */
@Composable
fun HeaderBand(
    modifier: Modifier = Modifier,
    blob: Blob = Blob(1.0f, 0.0f, 0.45f, Neon.Violet, 0.4f),
    content: @Composable () -> Unit,
) {
    Box(
        modifier
            .fillMaxWidth()
            .glassHeader()
    ) {
        BlobLayer(listOf(blob), Modifier.matchParentSize())
        ParticleField(5, Modifier.matchParentSize(), seed = 11)
        Twinkles()
        Column(Modifier.statusBarsPadding().padding(start = 22.dp, end = 22.dp, top = 14.dp, bottom = 24.dp)) { content() }
        Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(0.dp).background(Neon.Violet.copy(alpha = 0.25f)).padding(top = 1.dp))
    }
}

/** Due stelline che scintillano, come negli header del mockup. */
@Composable
fun androidx.compose.foundation.layout.BoxScope.Twinkles() {
    androidx.compose.foundation.Canvas(
        Modifier.align(Alignment.TopStart).statusBarsPadding().padding(start = 50.dp, top = 18.dp).size(7.dp).twinkle(2000),
    ) { drawCircle(Neon.Cyan) }
    androidx.compose.foundation.Canvas(
        Modifier.align(Alignment.TopEnd).statusBarsPadding().padding(end = 60.dp, top = 34.dp).size(5.dp).twinkle(2600, 500),
    ) { drawCircle(Neon.Lilac) }
}

@Composable
fun TopRow(start: @Composable () -> Unit, end: @Composable () -> Unit = {}) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        start(); end()
    }
}

/** Campo di testo nello stile neon. */
@Composable
fun NeonTextField(
    value: String,
    onChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    singleLine: Boolean = false,
    minLines: Int = 1,
    password: Boolean = false,
) {
    OutlinedTextField(
        value, onChange,
        modifier = modifier.fillMaxWidth(),
        placeholder = { Text(placeholder, color = Neon.Text3) },
        singleLine = singleLine,
        minLines = minLines,
        shape = RoundedCornerShape(14.dp),
        textStyle = TextStyle(color = Neon.Text, fontSize = 15.sp),
        visualTransformation = if (password) androidx.compose.ui.text.input.PasswordVisualTransformation()
        else androidx.compose.ui.text.input.VisualTransformation.None,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Neon.Cyan,
            unfocusedBorderColor = Neon.Violet.copy(alpha = 0.35f),
            focusedContainerColor = Neon.Surface,
            unfocusedContainerColor = Neon.Surface,
            cursorColor = Neon.Cyan,
        ),
    )
}

/** Selettore data (solo date future). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NeonDatePicker(initial: LocalDate, onPick: (LocalDate) -> Unit, onDismiss: () -> Unit) {
    val today = LocalDate.now()
    val state = rememberDatePickerState(
        initialSelectedDateMillis = initial.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli(),
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long) =
                !Instant.ofEpochMilli(utcTimeMillis).atZone(ZoneOffset.UTC).toLocalDate().isBefore(today.plusDays(1))
        },
    )
    val colors = DatePickerDefaults.colors(
        containerColor = Neon.Surface,
        selectedDayContainerColor = Neon.Violet,
        todayDateBorderColor = Neon.Cyan,
        todayContentColor = Neon.Cyan,
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        colors = colors,
        confirmButton = {
            TextButton({
                state.selectedDateMillis?.let { onPick(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()) }
            }) { Text("Conferma", color = Neon.Cyan) }
        },
        dismissButton = { TextButton(onDismiss) { Text("Annulla", color = Neon.Text2) } },
    ) { DatePicker(state, colors = colors, title = null, showModeToggle = false) }
}

/** Header di vetro: la nebulosa ci scorre dietro sfocata; senza vetro, il gradiente scuro. */
@Composable
private fun Modifier.glassHeader(): Modifier {
    val haze = com.francescopaoli.northstar.ui.fx.LocalHaze.current
    return if (haze != null && com.francescopaoli.northstar.ui.fx.LocalFx.current.full && android.os.Build.VERSION.SDK_INT >= 31) {
        this.hazeChild(
            haze,
            dev.chrisbanes.haze.HazeStyle(
                backgroundColor = Neon.Night,
                tint = dev.chrisbanes.haze.HazeTint(Neon.SurfaceHi.copy(alpha = 0.65f)),
                blurRadius = 30.dp, noiseFactor = 0.05f,
            ),
        )
    } else this.background(Neon.headerBrush)
}
