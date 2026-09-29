package com.francescopaoli.northstar.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/** Palette "Notte Neon" (dal documento di specifiche). */
object Neon {
    val Night = Color(0xFF0F0920)        // sfondo principale
    val Surface = Color(0xFF1B1236)      // carte
    val SurfaceHi = Color(0xFF241849)    // header a gradiente
    val NavBar = Color(0xFF17102E)
    val Track = Color(0xFF2A2050)        // binario degli anelli
    val Violet = Color(0xFF7C3AED)       // accento 1
    val Cyan = Color(0xFF22D3EE)         // accento 2
    val Lilac = Color(0xFFB08CF7)
    val Text = Color(0xFFF1EEFC)         // testo primario
    val TextSoft = Color(0xFFD8D0EE)
    val TextMid = Color(0xFFC9C0E3)
    val Text2 = Color(0xFF9B8FC7)        // testo secondario
    val Text3 = Color(0xFF6E6291)
    val Link = Color(0xFF8A7FB0)
    val Inactive = Color(0xFF5C5482)
    val Postponed = Color(0xFF524A78)
    val Border = Violet.copy(alpha = 0.30f)

    val gradient = listOf(Violet, Cyan, Violet)
    val gradient2 = listOf(Violet, Cyan)
    val headerBrush = Brush.linearGradient(listOf(Surface, SurfaceHi))
}

private val scheme = darkColorScheme(
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
    MaterialTheme(colorScheme = scheme, typography = type, content = content)
