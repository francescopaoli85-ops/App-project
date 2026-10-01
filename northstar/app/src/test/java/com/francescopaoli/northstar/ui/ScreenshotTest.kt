package com.francescopaoli.northstar.ui

import android.app.Application
import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.size
import androidx.compose.ui.unit.dp
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.francescopaoli.northstar.AppContainer
import com.francescopaoli.northstar.auth.Session
import com.francescopaoli.northstar.data.Area
import com.francescopaoli.northstar.data.Criterion
import com.francescopaoli.northstar.data.Goal
import com.francescopaoli.northstar.data.GoalAction
import com.francescopaoli.northstar.data.GoalStatus
import com.francescopaoli.northstar.ui.screens.AchievementsScreen
import com.francescopaoli.northstar.ui.screens.CalendarConnectScreen
import com.francescopaoli.northstar.ui.screens.CelebrationScreen
import com.francescopaoli.northstar.ui.screens.CheckinScreen
import com.francescopaoli.northstar.ui.screens.DetailScreen
import com.francescopaoli.northstar.ui.screens.HomeScreen
import com.francescopaoli.northstar.ui.screens.LoginScreen
import com.francescopaoli.northstar.ui.screens.NewGoalScreen
import com.francescopaoli.northstar.ui.screens.SettingsScreen
import com.francescopaoli.northstar.ui.screens.WeekSummaryScreen
import com.francescopaoli.northstar.ui.theme.NorthstarTheme
import com.github.takahirom.roborazzi.captureRoboImage
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.LocalDate

/**
 * Disegna ogni schermata su JVM e salva uno screenshot in app/build/outputs/roborazzi/.
 * Serve a controllare i layout senza telefono: ./gradlew testDebugUnitTest
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w393dp-h851dp-xxhdpi", application = Application::class)
class ScreenshotTest {

    @get:Rule val compose = createComposeRule()
    private lateinit var vm: MainViewModel

    private val work = Goal(
        id = "work", area = Area.LAVORO,
        answers = mapOf(
            Criterion.POSITIVO to "Presentare il piano del negozio",
            Criterion.SPECIFICO to "Tre azioni già avviate",
            Criterion.VERIFICABILE to "Il team lo approva",
        ),
        summary = "Entro il 24 novembre presenterò il piano del negozio: tre azioni già avviate — lo saprò quando il team lo approva.",
        deadlineEpochDay = LocalDate.now().plusDays(12).toEpochDay(),
        actions = List(7) {
            GoalAction(
                id = "a$it", text = listOf("Raccogliere i numeri di cassa", "Bozza del piano", "Parlare con Luca",
                    "Rivedere i costi", "Prova con il team", "Preparare le slide", "Presentazione")[it],
                done = it < 5, doneAt = if (it < 5) System.currentTimeMillis() - (4 - it) * 7L * 86_400_000 else null,
            )
        },
    )
    private val run = Goal(
        id = "run", area = Area.SALUTE, answers = mapOf(Criterion.POSITIVO to "Correre 5 km senza fermarmi"),
        deadlineEpochDay = LocalDate.now().plusDays(41).toEpochDay(),
        actions = List(3) { GoalAction(id = "r$it", text = "Uscita $it", done = it == 0) },
    )
    private val dinner = Goal(
        id = "dinner", area = Area.RELAZIONI, answers = mapOf(Criterion.POSITIVO to "Cena con i vecchi amici"),
        status = GoalStatus.POSTPONED, deadlineEpochDay = LocalDate.now().plusDays(20).toEpochDay(),
    )
    private val books = Goal(
        id = "books", area = Area.PERSONALE, answers = mapOf(Criterion.POSITIVO to "Leggere 12 libri quest'anno"),
        status = GoalStatus.ACHIEVED, achievedAt = System.currentTimeMillis(),
    )

    @Before
    fun setUp() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val c = AppContainer(app)
        runBlocking {
            c.settings.setLocalName("Francesco")
            val repo = c.repositoryFor(Session.Local("Francesco"))
            listOf(work, run, dinner, books).forEach { repo.upsert(it) }
        }
        vm = MainViewModel(c)
    }

    /** Monta la schermata, lascia correre le animazioni d'ingresso e scatta. */
    private fun shot(name: String, atMs: Long = 2_500, content: @Composable () -> Unit) {
        compose.mainClock.autoAdvance = false
        compose.setContent {
            // nei test il disegno è software: niente shader (la nebulosa è verificata a parte)
            androidx.compose.runtime.CompositionLocalProvider(
                com.francescopaoli.northstar.ui.fx.LocalFx provides com.francescopaoli.northstar.ui.fx.FxConfig(full = true, shaders = false),
            ) { NorthstarTheme { content() } }
        }
        compose.waitUntil(5_000) { vm.goals.value.isNotEmpty() }
        compose.mainClock.advanceTimeBy(atMs)
        compose.onRoot().captureRoboImage("build/outputs/roborazzi/$name.png")
    }

    @Test fun login() = shot("01_login") { LoginScreen(vm) }
    @Test fun home() = shot("02_home") { HomeScreen(vm, {}, {}, {}, {}) }
    @Test fun newGoal() = shot("03_nuovo_obiettivo") { NewGoalScreen(vm, {}, {}) }
    @Test fun detail() = shot("04_dettaglio") { DetailScreen(vm, "work", {}, {}, {}) }
    @Test fun achievements() = shot("05_traguardi") { AchievementsScreen(vm) {} }
    @Test fun calendar() = shot("06_collega_calendario") { CalendarConnectScreen(vm) {} }
    @Test fun checkin() = shot("07_checkin") { CheckinScreen(vm, "work", {}, {}) }
    @Test fun celebration() = shot("08_celebrazione") { CelebrationScreen(vm, "books", {}, {}) }
    @Test fun settings() = shot("09_impostazioni") { SettingsScreen(vm, {}, {}) }
    @Test fun week() = shot("10_riepilogo_settimana") { WeekSummaryScreen(vm) {} }
    @Test fun warp() = shot("11_iperspazio", atMs = 950) {
        com.francescopaoli.northstar.ui.fx.NeonBackdrop { com.francescopaoli.northstar.ui.fx.WarpOverlay(onFinished = {}) }
    }
    @Test fun orb() = shot("12_sfera_liquida", atMs = 1_200) {
        com.francescopaoli.northstar.ui.fx.NeonBackdrop {
            com.francescopaoli.northstar.ui.fx.LiquidOrb(
                0.8f, true,
                androidx.compose.ui.Modifier.align(androidx.compose.ui.Alignment.Center).then(androidx.compose.ui.Modifier.size(220.dp)),
            )
        }
    }
    @Test fun polaris() = shot("14_stella_polare") {
        com.francescopaoli.northstar.ui.screens.PolarisScreen(vm, {}, {}, {})
    }
    @Test fun homeTramonto() = themed("15_home_tramonto", com.francescopaoli.northstar.ui.theme.Palettes.Tramonto) { HomeScreen(vm, {}, {}, {}, {}) }
    @Test fun homeOro() = themed("16_home_oro_nero", com.francescopaoli.northstar.ui.theme.Palettes.OroNero) { HomeScreen(vm, {}, {}, {}, {}) }

    private fun themed(name: String, p: com.francescopaoli.northstar.ui.theme.Palette, content: @Composable () -> Unit) {
        com.francescopaoli.northstar.ui.theme.Neon.palette = p
        try { shot(name, content = content) } finally {
            com.francescopaoli.northstar.ui.theme.Neon.palette = com.francescopaoli.northstar.ui.theme.Palettes.NotteNeon
        }
    }
    @Test fun celebrationLater() = shot("13_celebrazione_fuochi", atMs = 3_200) { CelebrationScreen(vm, "books", {}, {}) }
}
