package com.francescopaoli.northstar.ui.fx

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier

/*
 * Transizioni "condivise": l'anello e il titolo di un obiettivo volano dalla card della Home
 * all'header del Dettaglio (e ritorno). Fuori dalla navigazione (es. nei test) non fanno nulla.
 */

@OptIn(ExperimentalSharedTransitionApi::class)
val LocalSharedScope = staticCompositionLocalOf<SharedTransitionScope?> { null }
val LocalNavAnimScope = staticCompositionLocalOf<AnimatedVisibilityScope?> { null }

/** Elemento che vola mantenendo il suo disegno (es. l'anello di progresso). */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun Modifier.sharedElementOf(key: String): Modifier {
    val shared = LocalSharedScope.current ?: return this
    val anim = LocalNavAnimScope.current ?: return this
    if (!LocalFx.current.full) return this
    return with(shared) { this@sharedElementOf.sharedElement(rememberSharedContentState(key), anim) }
}

/** Testo che vola cambiando dimensione (scalato, così non va a capo a metà volo). */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun Modifier.sharedTextOf(key: String): Modifier {
    val shared = LocalSharedScope.current ?: return this
    val anim = LocalNavAnimScope.current ?: return this
    if (!LocalFx.current.full) return this
    return with(shared) {
        this@sharedTextOf.sharedBounds(
            rememberSharedContentState(key), anim,
            resizeMode = SharedTransitionScope.ResizeMode.ScaleToBounds(),
        )
    }
}
