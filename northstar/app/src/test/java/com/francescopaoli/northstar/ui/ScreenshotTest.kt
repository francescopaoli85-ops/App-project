package com.francescopaoli.northstar.ui

import android.app.Application
import androidx.compose.runtime.Composable
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
        actions = List(7) { GoalAction(id = "a$it", text = "Azione ${it + 1}", done = it < 5) },
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
    private fun shot(name: String, content: @Composable () -> Unit) {
        compose.mainClock.autoAdvance = false
        compose.setContent { NorthstarTheme { content() } }
        compose.waitUntil(5_000) { vm.goals.value.isNotEmpty() }
        compose.mainClock.advanceTimeBy(2_500)
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
}
