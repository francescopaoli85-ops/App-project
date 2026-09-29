package com.francescopaoli.northstar.ui

import androidx.compose.animation.core.tween
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
import com.francescopaoli.northstar.ui.theme.Neon

@Composable
fun NorthstarRoot(container: AppContainer, deepLink: String?, onDeepLinkHandled: () -> Unit) {
    val vm: MainViewModel = viewModel(factory = MainViewModel.Factory(container))
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
        NavHost(
            nav, start,
            enterTransition = { fadeIn(tween(350)) + slideInVertically(tween(420)) { it / 12 } },
            exitTransition = { fadeOut(tween(250)) },
            popEnterTransition = { fadeIn(tween(350)) + scaleIn(tween(350), 0.97f) },
            popExitTransition = { fadeOut(tween(250)) },
        ) {
            composable(Routes.LOGIN) { LoginScreen(vm) }
            composable(Routes.HOME) {
                HomeScreen(
                    vm,
                    onNew = { nav.navigate(Routes.NEW) },
                    onOpen = { nav.navigate(Routes.detail(it)) },
                    onCalendar = { nav.navigate(Routes.CALENDAR) },
                    onTab = { nav.goTab(it) },
                )
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
                DetailScreen(
                    vm, id,
                    onBack = { if (!nav.popBackStack()) nav.navigate(Routes.HOME) },
                    onCheckin = { nav.navigate(Routes.checkin(id)) },
                    onAchieved = { nav.navigate(Routes.celebrate(id)) { popUpTo(Routes.HOME) } },
                )
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
                CelebrationScreen(
                    vm, e.arguments?.getString("id").orEmpty(),
                    onAchievements = { nav.navigate(Routes.ACHIEVEMENTS) { popUpTo(Routes.HOME) } },
                    onHome = { nav.popBackStack(Routes.HOME, false) },
                )
            }
            composable(Routes.ACHIEVEMENTS) { AchievementsScreen(vm, onTab = { nav.goTab(it) }) }
            composable(Routes.CALENDAR) {
                CalendarConnectScreen(vm, onDone = { if (!nav.popBackStack(Routes.HOME, false)) nav.navigate(Routes.HOME) })
            }
            composable(Routes.SETTINGS) {
                SettingsScreen(vm, onTab = { nav.goTab(it) }, onConnectCalendar = { nav.navigate(Routes.CALENDAR) })
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

/** Cambio di tab dalla barra in basso, senza accumulare schermate. */
private fun NavHostController.goTab(tab: Tab) {
    val route = when (tab) {
        Tab.HOME -> Routes.HOME
        Tab.TRAGUARDI -> Routes.ACHIEVEMENTS
        Tab.IMPOSTAZIONI -> Routes.SETTINGS
    }
    navigate(route) {
        popUpTo(Routes.HOME) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
