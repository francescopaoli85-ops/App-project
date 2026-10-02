package com.francescopaoli.northstar.ui

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.core.tween
import androidx.compose.runtime.CompositionLocalProvider
import com.francescopaoli.northstar.ui.fx.FxConfig
import com.francescopaoli.northstar.ui.fx.LocalFx
import com.francescopaoli.northstar.ui.fx.LocalNavAnimScope
import com.francescopaoli.northstar.ui.fx.LocalParallax
import com.francescopaoli.northstar.ui.fx.LocalSharedScope
import com.francescopaoli.northstar.ui.fx.SparkHost
import com.francescopaoli.northstar.ui.fx.rememberParallax
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.currentBackStackEntryAsState
import kotlinx.coroutines.launch
import com.francescopaoli.northstar.AppContainer
import com.francescopaoli.northstar.ui.components.Tab
import com.francescopaoli.northstar.ui.fx.NeonBackdrop
import com.francescopaoli.northstar.ui.screens.AchievementsScreen
import com.francescopaoli.northstar.ui.screens.CalendarConnectScreen
import com.francescopaoli.northstar.ui.screens.CelebrationScreen
import com.francescopaoli.northstar.ui.screens.CheckinScreen
import com.francescopaoli.northstar.ui.screens.DetailScreen
import com.francescopaoli.northstar.ui.screens.HomeScreen
import com.francescopaoli.northstar.ui.screens.LoginScreen
import com.francescopaoli.northstar.ui.screens.NewGoalScreen
import com.francescopaoli.northstar.ui.screens.SettingsScreen
import com.francescopaoli.northstar.ui.screens.PolarisScreen
import com.francescopaoli.northstar.ui.screens.WeekSummaryScreen
import com.francescopaoli.northstar.ui.theme.Neon

@Composable
fun NorthstarRoot(container: AppContainer, deepLink: String?, onDeepLinkHandled: () -> Unit) {
    val vm: MainViewModel = viewModel(factory = MainViewModel.Factory(container))
    val settings by vm.settings.collectAsStateWithLifecycle()
    val ctx = androidx.compose.ui.platform.LocalContext.current
    // se nel sistema le animazioni sono disattivate, rispettiamo la scelta
    val systemAnimOff = remember {
        android.provider.Settings.Global.getFloat(ctx.contentResolver, android.provider.Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }
    val powerSave by com.francescopaoli.northstar.ui.fx.rememberPowerSaveMode()
    val fx = FxConfig(
        full = !settings.reducedEffects && !systemAnimOff,
        // sfondo statico: scelto per questo tema, o risparmio energetico attivo
        animated = settings.theme !in settings.staticThemes && (!powerSave || settings.animateOnPowerSave),
    )
    // tema scelto: cambiando la palette si ricolora tutta l'app
    val palette = com.francescopaoli.northstar.ui.theme.Palettes.byId(settings.theme)
    androidx.compose.runtime.SideEffect { com.francescopaoli.northstar.ui.theme.Neon.palette = palette }
    // giroscopio: opzione a sé, non dipende dagli sfondi animati né dal risparmio energetico
    val parallax = rememberParallax(settings.parallaxOn)
    CompositionLocalProvider(
        LocalFx provides fx, LocalParallax provides parallax,
        com.francescopaoli.northstar.audio.LocalSound provides container.sound,
    ) {
        SparkHost(enabled = fx.full && fx.animated) { RootContent(vm, deepLink, onDeepLinkHandled) }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun RootContent(vm: MainViewModel, deepLink: String?, onDeepLinkHandled: () -> Unit) {
    val session by vm.session.collectAsStateWithLifecycle()
    val confirm by vm.calendarConfirm.collectAsStateWithLifecycle()
    val message by vm.messages.collectAsStateWithLifecycle()
    val snack = remember { SnackbarHostState() }

    LaunchedEffect(message) {
        message?.let { snack.showSnackbar(it); vm.messages.value = null }
    }

    if (session == SessionState.Loading) {
        NeonBackdrop { }
        return
    }

    val nav = rememberNavController()
    val start = remember { if (session is SessionState.LoggedIn) Routes.HOME else Routes.LOGIN }

    // musica per schermata: il percorso guida i suoi livelli da sé, la festa resta all'apoteosi
    val route = nav.currentBackStackEntryAsState().value?.destination?.route
    LaunchedEffect(route) {
        when (route) {
            null, Routes.NEW -> Unit
            Routes.CELEBRATE -> vm.sound.music.setScene(com.francescopaoli.northstar.audio.Scene.Flow(7))
            else -> vm.sound.music.setScene(com.francescopaoli.northstar.audio.Scene.Ambient)
        }
    }

    // login/logout: sposta la navigazione sulla schermata giusta
    LaunchedEffect(session) {
        val current = nav.currentDestination?.route
        when {
            session is SessionState.LoggedIn && current == Routes.LOGIN -> nav.navigate(Routes.HOME) { popUpTo(0) }
            session == SessionState.LoggedOut && current != Routes.LOGIN -> nav.navigate(Routes.LOGIN) { popUpTo(0) }
        }
    }
    // tocco su una notifica
    LaunchedEffect(deepLink, session) {
        if (deepLink != null && session is SessionState.LoggedIn) {
            nav.navigate(deepLink) { launchSingleTop = true }
            onDeepLinkHandled()
        }
    }

    Box(Modifier.fillMaxSize()) {
      SharedTransitionLayout {
       CompositionLocalProvider(LocalSharedScope provides this) {
        NavHost(
            nav, start,
            enterTransition = { fadeIn(tween(350)) + slideInVertically(tween(420)) { it / 12 } },
            exitTransition = { fadeOut(tween(250)) },
            popEnterTransition = { fadeIn(tween(350)) + scaleIn(tween(350), 0.97f) },
            popExitTransition = { fadeOut(tween(250)) },
        ) {
            composable(Routes.LOGIN) { LoginScreen(vm) }
            // Home, Traguardi e Impostazioni: tre pagine che si sfogliano col dito
            listOf(Routes.HOME to Tab.HOME, Routes.ACHIEVEMENTS to Tab.TRAGUARDI, Routes.SETTINGS to Tab.IMPOSTAZIONI).forEach { (route, tab) ->
                composable(route) {
                  CompositionLocalProvider(LocalNavAnimScope provides this) {
                    TabsHost(vm, tab, nav)
                  }
                }
            }
            composable(Routes.NEW) {
                NewGoalScreen(
                    vm,
                    onClose = { nav.popBackStack() },
                    onCreated = { firstGoal ->
                        val s = vm.settings.value
                        if (firstGoal && !s.calendarConnected && !s.calendarPromptSeen) {
                            nav.navigate(Routes.CALENDAR) { popUpTo(Routes.HOME) }
                        } else nav.popBackStack(Routes.HOME, false)
                    },
                )
            }
            composable(Routes.DETAIL) { e ->
                val id = e.arguments?.getString("id").orEmpty()
              CompositionLocalProvider(LocalNavAnimScope provides this) {
                DetailScreen(
                    vm, id,
                    onBack = { if (!nav.popBackStack()) nav.navigate(Routes.HOME) },
                    onCheckin = { nav.navigate(Routes.checkin(id)) },
                    onAchieved = { nav.navigate(Routes.celebrate(id)) { popUpTo(Routes.HOME) } },
                )
              }
            }
            composable(Routes.CHECKIN) { e ->
                val id = e.arguments?.getString("id").orEmpty()
                CheckinScreen(
                    vm, id,
                    onClose = { if (!nav.popBackStack()) nav.navigate(Routes.HOME) },
                    onDone = { nav.navigate(Routes.detail(id)) { popUpTo(Routes.HOME) } },
                )
            }
            composable(Routes.CELEBRATE) { e ->
                val id = e.arguments?.getString("id").orEmpty()
                CelebrationScreen(
                    vm, id,
                    onAchievements = { nav.navigate(Routes.ACHIEVEMENTS) { popUpTo(Routes.HOME) } },
                    onHome = { nav.popBackStack(Routes.HOME, false) },
                    onUndo = { vm.undoAchieve(id); nav.navigate(Routes.detail(id)) { popUpTo(Routes.HOME) } },
                )
            }
            composable(Routes.CALENDAR) {
                CalendarConnectScreen(vm, onDone = { if (!nav.popBackStack(Routes.HOME, false)) nav.navigate(Routes.HOME) })
            }
            composable(Routes.POLARIS) {
                PolarisScreen(
                    vm,
                    onClose = { if (!nav.popBackStack()) nav.navigate(Routes.HOME) },
                    onOpen = { nav.navigate(Routes.detail(it)) },
                    onNew = { nav.navigate(Routes.NEW) },
                )
            }
            composable(Routes.WEEK) {
                WeekSummaryScreen(vm, onClose = { if (!nav.popBackStack(Routes.HOME, false)) nav.navigate(Routes.HOME) })
            }
        }

       }
      }

        SnackbarHost(snack, Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(16.dp)) {
            Snackbar(it, containerColor = Neon.SurfaceHi, contentColor = Neon.Text)
        }
    }

    // Modalità "chiedi ogni volta": conferma prima di creare l'evento
    confirm?.let { g ->
        AlertDialog(
            onDismissRequest = { vm.confirmCalendar(false) },
            containerColor = Neon.Surface,
            title = { Text("Lo aggiungo al calendario?") },
            text = { Text("Creo un evento su Google Calendar per la scadenza di \"${g.title}\".", color = Neon.Text2) },
            confirmButton = { TextButton({ vm.confirmCalendar(true) }) { Text("Sì, aggiungi", color = Neon.Cyan) } },
            dismissButton = { TextButton({ vm.confirmCalendar(false) }) { Text("No", color = Neon.Text2) } },
        )
    }
}

/**
 * Le tre schermate principali in un pager orizzontale: si passa dall'una all'altra
 * scorrendo col dito o toccando la barra in basso (che segue lo scorrimento).
 */
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun TabsHost(vm: MainViewModel, initial: Tab, nav: NavHostController) {
    val pager = androidx.compose.foundation.pager.rememberPagerState(initialPage = initial.ordinal) { Tab.entries.size }
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val sound = com.francescopaoli.northstar.audio.LocalSound.current
    // fruscio quando la pagina cambia davvero
    var lastPage by androidx.compose.runtime.remember { androidx.compose.runtime.mutableIntStateOf(initial.ordinal) }
    LaunchedEffect(pager.settledPage) {
        if (pager.settledPage != lastPage) { sound?.sfx?.swoosh(); lastPage = pager.settledPage }
    }
    val goTo: (Tab) -> Unit = { t -> scope.launch { pager.animateScrollToPage(t.ordinal) } }
    androidx.compose.foundation.layout.Column(Modifier.fillMaxSize()) {
        CompositionLocalProvider(com.francescopaoli.northstar.ui.components.LocalTabsHosted provides true) {
            androidx.compose.foundation.pager.HorizontalPager(pager, Modifier.weight(1f)) { page ->
                when (Tab.entries[page]) {
                    Tab.HOME -> HomeScreen(
                        vm,
                        onNew = { nav.navigate(Routes.NEW) },
                        onOpen = { nav.navigate(Routes.detail(it)) },
                        onCalendar = { nav.navigate(Routes.CALENDAR) },
                        onTab = goTo,
                        onWeek = { nav.navigate(Routes.WEEK) },
                        onPolaris = { nav.navigate(Routes.POLARIS) },
                        onCheckin = { nav.navigate(Routes.checkin(it)) },
                    )
                    Tab.TRAGUARDI -> AchievementsScreen(vm, onTab = goTo)
                    Tab.IMPOSTAZIONI -> SettingsScreen(vm, onTab = goTo, onConnectCalendar = { nav.navigate(Routes.CALENDAR) })
                }
            }
        }
        com.francescopaoli.northstar.ui.components.BottomNav(Tab.entries[pager.targetPage], goTo)
    }
}
