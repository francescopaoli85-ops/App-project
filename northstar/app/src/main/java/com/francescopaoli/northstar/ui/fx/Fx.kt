package com.francescopaoli.northstar.ui.fx

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.geometry.Offset
import dev.chrisbanes.haze.HazeState

/**
 * Livello degli effetti: "full" = tutto il pacchetto (shader, vetro, parallasse, scintille).
 * [shaders] = false solo nei test: lì il disegno è software e gli shader non girano.
 * [animated] = false: versione statica del tema (scelta per tema o risparmio energetico).
 *   Niente ridisegni a ogni fotogramma: il cielo è fermo nella sua posa migliore.
 */
data class FxConfig(val full: Boolean = true, val shaders: Boolean = true, val animated: Boolean = true)

val LocalFx = staticCompositionLocalOf { FxConfig() }

/** Inclinazione del telefono, da -1 a 1 su x e y (vedi Parallax.kt). */
val LocalParallax = staticCompositionLocalOf<State<Offset>> { mutableStateOf(Offset.Zero) }

/** Sorgente dello sfondo da sfocare sotto le card di vetro (impostata da NeonBackdrop). */
val LocalHaze = staticCompositionLocalOf<HazeState?> { null }
