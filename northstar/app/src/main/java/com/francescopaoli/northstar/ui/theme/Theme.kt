package com.francescopaoli.northstar.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/** Una palette completa: ogni tema ne definisce una. */
data class Palette(
    val id: String,
    val name: String,
    val night: Color,      // sfondo principale
    val surface: Color,    // carte
    val surfaceHi: Color,  // header
    val navBar: Color,
    val track: Color,      // binario degli anelli
    val accent1: Color,    // "viola" in Notte Neon
    val accent2: Color,    // "ciano" in Notte Neon
    val soft: Color,       // accento tenue (etichette)
    val text: Color,
    val textSoft: Color,
    val textMid: Color,
    val text2: Color,
    val text3: Color,
    val link: Color,
    val inactive: Color,
    val postponed: Color,
    /** Testo e icone sopra i bottoni a gradiente (scuro se gli accenti sono chiari, es. oro). */
    val onAccent: Color = Color.White,
)

object Palettes {
    val NotteNeon = Palette(
        "neon", "Notte Neon",
        night = Color(0xFF0F0920), surface = Color(0xFF1B1236), surfaceHi = Color(0xFF241849), navBar = Color(0xFF17102E),
        track = Color(0xFF2A2050), accent1 = Color(0xFF7C3AED), accent2 = Color(0xFF22D3EE), soft = Color(0xFFB08CF7),
        text = Color(0xFFF1EEFC), textSoft = Color(0xFFD8D0EE), textMid = Color(0xFFC9C0E3), text2 = Color(0xFF9B8FC7),
        text3 = Color(0xFF6E6291), link = Color(0xFF8A7FB0), inactive = Color(0xFF5C5482), postponed = Color(0xFF524A78),
    )
    /** Arancio, rosa e corallo su viola scuro. */
    val Tramonto = Palette(
        "tramonto", "Tramonto",
        night = Color(0xFF1A0B1C), surface = Color(0xFF2A1230), surfaceHi = Color(0xFF3A1838), navBar = Color(0xFF221028),
        track = Color(0xFF45203F), accent1 = Color(0xFFFF4F8B), accent2 = Color(0xFFFFA14A), soft = Color(0xFFFF9C8A),
        text = Color(0xFFFFF1EC), textSoft = Color(0xFFF5D9D3), textMid = Color(0xFFE8C3BE), text2 = Color(0xFFC99AA6),
        text3 = Color(0xFF8E6577), link = Color(0xFFB4889A), inactive = Color(0xFF6E4A60), postponed = Color(0xFF6A4560),
    )
    /** Oro caldo su nero: elegante, pochi colori. */
    val OroNero = Palette(
        "oro", "Oro e nero",
        night = Color(0xFF0A0A0C), surface = Color(0xFF17161A), surfaceHi = Color(0xFF211F25), navBar = Color(0xFF121114),
        track = Color(0xFF2B2830), accent1 = Color(0xFFC9962B), accent2 = Color(0xFFF2D27A), soft = Color(0xFFE3C267),
        text = Color(0xFFF7F2E6), textSoft = Color(0xFFE9E1CF), textMid = Color(0xFFD8CFBC), text2 = Color(0xFFA89F8C),
        text3 = Color(0xFF6F685B), link = Color(0xFF9A917F), inactive = Color(0xFF555048), postponed = Color(0xFF4D473E),
        onAccent = Color(0xFF1A1408),
    )
    val all = listOf(NotteNeon, Tramonto, OroNero)
    fun byId(id: String?) = all.firstOrNull { it.id == id } ?: NotteNeon
}

/**
 * Colori del tema attivo. I nomi restano quelli di Notte Neon (Violet = accento 1, Cyan = accento 2)
 * così il resto dell'app non cambia: cambiando [palette] si ricolora tutto, nebulosa compresa.
 */
object Neon {
    var palette by mutableStateOf(Palettes.NotteNeon)

    val Night get() = palette.night
    val Surface get() = palette.surface
    val SurfaceHi get() = palette.surfaceHi
    val NavBar get() = palette.navBar
    val Track get() = palette.track
    val Violet get() = palette.accent1
    val Cyan get() = palette.accent2
    val Lilac get() = palette.soft
    val Text get() = palette.text
    val TextSoft get() = palette.textSoft
    val TextMid get() = palette.textMid
    val Text2 get() = palette.text2
    val Text3 get() = palette.text3
    val Link get() = palette.link
    val Inactive get() = palette.inactive
    val Postponed get() = palette.postponed
    val OnAccent get() = palette.onAccent
    val Border get() = Violet.copy(alpha = 0.30f)

    val gradient get() = listOf(Violet, Cyan, Violet)
    val gradient2 get() = listOf(Violet, Cyan)
    val headerBrush get() = Brush.linearGradient(listOf(Surface, SurfaceHi))
}

private fun scheme() = darkColorScheme(
    primary = Neon.Violet,
    onPrimary = Color.White,
    secondary = Neon.Cyan,
    onSecondary = Neon.Night,
    background = Neon.Night,
    onBackground = Neon.Text,
    surface = Neon.Surface,
    onSurface = Neon.Text,
    surfaceVariant = Neon.SurfaceHi,
    onSurfaceVariant = Neon.Text2,
    surfaceContainerHigh = Neon.Surface,
    surfaceContainerHighest = Neon.SurfaceHi,
    outline = Neon.Border,
)

/** Font di sistema con pesi alti sui titoli, per un look deciso. */
private val type = Typography(
    headlineLarge = TextStyle(fontSize = 34.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.8).sp),
    headlineMedium = TextStyle(fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.5).sp),
    headlineSmall = TextStyle(fontSize = 23.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.3).sp, lineHeight = 30.sp),
    titleMedium = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.SemiBold),
    bodyMedium = TextStyle(fontSize = 13.5.sp, lineHeight = 21.sp),
    labelSmall = TextStyle(fontSize = 10.5.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.4.sp),
)

@Composable
fun NorthstarTheme(content: @Composable () -> Unit) =
    MaterialTheme(colorScheme = scheme(), typography = type, content = content)
