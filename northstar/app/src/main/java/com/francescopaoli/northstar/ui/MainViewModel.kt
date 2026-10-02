package com.francescopaoli.northstar.ui

import android.app.Activity
import android.app.PendingIntent
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.francescopaoli.northstar.AppContainer
import com.francescopaoli.northstar.auth.Session
import com.francescopaoli.northstar.data.Area
import com.francescopaoli.northstar.data.CalendarMode
import com.francescopaoli.northstar.data.Criterion
import com.francescopaoli.northstar.data.Goal
import com.francescopaoli.northstar.data.GoalAction
import com.francescopaoli.northstar.data.GoalRepository
import com.francescopaoli.northstar.data.GoalStatus
import com.francescopaoli.northstar.data.Settings
import com.francescopaoli.northstar.domain.PersonalityProfiler
import com.francescopaoli.northstar.domain.SummaryBuilder
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

sealed interface SessionState {
    data object Loading : SessionState
    data object LoggedOut : SessionState
    data class LoggedIn(val session: Session) : SessionState
}

/** Stato e azioni condivise da tutte le schermate. */
@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModel(private val c: AppContainer) : ViewModel() {

    val firebaseEnabled = c.auth.firebaseEnabled

    val session: StateFlow<SessionState> = c.auth.session
        .map { s -> if (s == null) SessionState.LoggedOut else SessionState.LoggedIn(s) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, SessionState.Loading)

    private val repo: StateFlow<GoalRepository?> = session
        .map { (it as? SessionState.LoggedIn)?.session?.let(c::repositoryFor) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val goals: StateFlow<List<Goal>> = repo
        .flatMapLatest { it?.observeGoals() ?: flowOf(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val settings: StateFlow<Settings> = c.settings.settings
        .stateIn(viewModelScope, SharingStarted.Eagerly, Settings())

    /** true = mostra le card sponsorizzate (consenso ok e niente acquisto "Rimuovi pubblicità"). */
    val showAds: StateFlow<Boolean> = c.ads.showAds.stateIn(viewModelScope, SharingStarted.Eagerly, false)
    val removeAdsPrice: StateFlow<String?> = c.billing.price
    val privacyOptionsRequired get() = c.ads.privacyOptionsRequired

    fun buyRemoveAds(activity: Activity) {
        if (!c.billing.buy(activity)) messages.value = "Acquisto non disponibile ora, riprova tra poco"
    }
    fun restorePurchases() = viewModelScope.launch { c.billing.restore(); messages.value = "Acquisti controllati" }
    fun showPrivacyOptions(activity: Activity) = c.ads.showPrivacyOptions(activity)

    /** Obiettivo in attesa di conferma per il calendario (modalità "chiedi ogni volta"). */
    val calendarConfirm = MutableStateFlow<Goal?>(null)

    /** Messaggi brevi da mostrare in una snackbar. */
    val messages = MutableStateFlow<String?>(null)

    fun goal(id: String): Goal? = goals.value.firstOrNull { it.id == id }

    private fun save(goal: Goal) = viewModelScope.launch { repo.value?.upsert(goal) }

    // ---------- Accesso ----------

    fun signInGoogle(activity: Activity, onDone: (Boolean) -> Unit) = viewModelScope.launch {
        val r = c.auth.signInWithGoogle(activity)
        r.exceptionOrNull()?.let { messages.value = "Accesso con Google non riuscito: ${it.message}" }
        onDone(r.isSuccess)
    }

    fun signInEmail(email: String, pw: String, onDone: (Boolean) -> Unit) = viewModelScope.launch {
        val r = c.auth.signInWithEmail(email, pw)
        r.exceptionOrNull()?.let { messages.value = "Accesso non riuscito: ${it.message}" }
        onDone(r.isSuccess)
    }

    fun enterLocal(name: String) = viewModelScope.launch { c.auth.enterLocal(name) }
    fun signOut() = viewModelScope.launch { c.auth.signOut() }

    // ---------- Obiettivi ----------

    /**
     * Salva l'obiettivo nato dal percorso guidato.
     * Qui usiamo anche i segnali nascosti per decidere come comportarsi col calendario.
     * Ritorna true se è il primo obiettivo (per proporre il collegamento al calendario).
     */
    suspend fun createGoal(area: Area, answers: Map<Criterion, String>, deadline: LocalDate, revisions: Int): Goal {
        val goal = Goal(
            area = area,
            answers = answers,
            deadlineEpochDay = deadline.toEpochDay(),
            summary = SummaryBuilder.build(answers, deadline),
        )
        c.sound.sfx.success()
        c.settings.setProfiledMode(PersonalityProfiler.calendarMode(PersonalityProfiler.Signals(answers, revisions)))
        repo.value?.upsert(goal)
        afterGoalChanged(goal, isNew = true)
        return goal
    }

    fun addAction(id: String, text: String) = goal(id)?.let {
        if (text.isNotBlank()) save(it.copy(actions = it.actions + GoalAction(text = text.trim())))
    }

    fun toggleAction(id: String, actionId: String) = goal(id)?.let { g ->
        val now = System.currentTimeMillis()
        if (g.actions.any { it.id == actionId && !it.done }) c.sound.sfx.done()
        save(g.copy(actions = g.actions.map {
            if (it.id == actionId) it.copy(done = !it.done, doneAt = if (it.done) null else now) else it
        }))
    }

    /** Check-in in un tocco: "sì, è ancora così". */
    fun confirmCheckin(id: String) = goal(id)?.let {
        c.sound.sfx.done()
        save(com.francescopaoli.northstar.domain.Checkins.confirmed(it))
    }

    fun editAction(id: String, actionId: String, text: String) = goal(id)?.let { g ->
        if (text.isNotBlank()) save(g.copy(actions = g.actions.map { if (it.id == actionId) it.copy(text = text.trim()) else it }))
    }

    /** Suggerimenti al primo uso: ciascuno compare una volta sola. */
    fun hintSeen(key: String) = viewModelScope.launch { c.settings.markHintSeen(key) }

    fun removeAction(id: String, actionId: String) = goal(id)?.let { g ->
        save(g.copy(actions = g.actions.filterNot { it.id == actionId }))
    }

    fun achieve(id: String) = goal(id)?.let {
        c.sound.sfx.success()
        save(it.copy(status = GoalStatus.ACHIEVED, achievedAt = System.currentTimeMillis()))
    }

    /** Annulla un "raggiunto" premuto per sbaglio: torna com'era prima. */
    fun undoAchieve(id: String) = goal(id)?.let {
        save(it.copy(
            status = if (it.postponedCount > 0) GoalStatus.POSTPONED else GoalStatus.ACTIVE,
            achievedAt = null,
        ))
    }

    /** Posticipa: cambia colore ma senza toni colpevolizzanti. */
    fun postpone(id: String, newDate: LocalDate) = goal(id)?.let {
        val updated = it.copy(
            status = GoalStatus.POSTPONED,
            deadlineEpochDay = newDate.toEpochDay(),
            postponedCount = it.postponedCount + 1,
            summary = SummaryBuilder.build(it.answers, newDate),
            lastCheckinAt = System.currentTimeMillis(),
        )
        save(updated)
        viewModelScope.launch { afterGoalChanged(updated, isNew = false) }
    }

    /** Risposta a un check-in: aggiorna quel criterio e passa al successivo. */
    fun answerCheckin(id: String, criterion: Criterion, text: String) = goal(id)?.let {
        val answers = if (text.isBlank()) it.answers else it.answers + (criterion to text.trim())
        save(
            it.copy(
                answers = answers,
                summary = SummaryBuilder.build(answers, it.deadline),
                nextCheckinIndex = it.nextCheckinIndex + 1,
                lastCheckinAt = System.currentTimeMillis(),
                checkinPending = false,
            ),
        )
    }

    fun delete(id: String) = viewModelScope.launch { repo.value?.delete(id) }

    // ---------- Google Calendar ----------

    /** Dopo creazione/modifica: evento automatico o richiesta di conferma, secondo il profilo. */
    private suspend fun afterGoalChanged(goal: Goal, isNew: Boolean) {
        val s = c.settings.settings.first()
        if (!s.calendarConnected) return
        if (!isNew && goal.calendarEventId != null) { pushToCalendar(goal); return }
        when (s.calendarMode ?: CalendarMode.AUTO) {
            CalendarMode.AUTO -> pushToCalendar(goal)
            CalendarMode.CONFIRM -> calendarConfirm.value = goal
        }
    }

    fun confirmCalendar(yes: Boolean) {
        val g = calendarConfirm.value ?: return
        calendarConfirm.value = null
        if (yes) viewModelScope.launch { pushToCalendar(g) }
    }

    private suspend fun pushToCalendar(goal: Goal) {
        val token = c.calendar.tokenSilently() ?: return
        val eventId = c.calendar.upsertEvent(token, goal) ?: return
        val latest = goal(goal.id) ?: goal
        if (latest.calendarEventId != eventId) repo.value?.upsert(latest.copy(calendarEventId = eventId))
    }

    /** Primo passo del collegamento: se serve il consenso ritorna il PendingIntent da lanciare. */
    suspend fun beginCalendarAuth(): PendingIntent? = runCatching {
        val r = c.calendar.authorize()
        if (r.hasResolution()) r.pendingIntent else { onCalendarConnected(); null }
    }.getOrElse {
        messages.value = "Non riesco a collegare Google Calendar: ${it.message}"
        null
    }

    fun onCalendarAuthResult(data: Intent?) = viewModelScope.launch {
        runCatching { c.calendar.resultFromIntent(data) }
            .onSuccess { if (it.accessToken != null) onCalendarConnected() }
            .onFailure { messages.value = "Collegamento annullato" }
    }

    private suspend fun onCalendarConnected() {
        c.settings.setCalendarConnected(true)
        c.settings.setCalendarPromptSeen()
        messages.value = "Google Calendar collegato ✦"
        // porta subito sul calendario le scadenze degli obiettivi aperti
        goals.value.filter { it.isOpen }.forEach { pushToCalendar(it) }
    }

    fun calendarPromptSeen() = viewModelScope.launch { c.settings.setCalendarPromptSeen() }
    fun disconnectCalendar() = viewModelScope.launch { c.settings.setCalendarConnected(false) }
    fun setCalendarMode(m: CalendarMode) = viewModelScope.launch { c.settings.setManualMode(m) }
    fun setVoice(v: Boolean) = viewModelScope.launch { c.settings.setVoiceGuide(v) }
    fun setThemeAnimated(id: String, animated: Boolean) = viewModelScope.launch { c.settings.setThemeAnimated(id, animated) }
    fun setTheme(id: String) = viewModelScope.launch { c.settings.setTheme(id) }
    /** "Più tardi" sul banner: si ripresenta tra una settimana. */
    fun hideCalendarBanner() = viewModelScope.launch {
        c.settings.hideCalendarBanner(java.time.LocalDate.now().plusDays(7).toEpochDay())
    }
    fun setReducedEffects(v: Boolean) = viewModelScope.launch { c.settings.setReducedEffects(v) }
    fun setMusicOn(v: Boolean) = viewModelScope.launch { c.settings.setMusicOn(v) }
    fun setAmbientSong(id: String) = viewModelScope.launch { c.settings.setAmbientSong(id) }
    fun setMusicVolume(v: Int) = viewModelScope.launch { c.settings.setMusicVolume(v) }
    fun setAnimateOnPowerSave(v: Boolean) = viewModelScope.launch { c.settings.setAnimateOnPowerSave(v) }
    fun setParallax(v: Boolean) = viewModelScope.launch { c.settings.setParallax(v) }
    fun setSfxOn(v: Boolean) = viewModelScope.launch { c.settings.setSfxOn(v) }
    /** Suoni per le schermate. */
    val sound get() = c.sound
    fun setCheckins(v: Boolean) = viewModelScope.launch { c.settings.setCheckins(v) }

    class Factory(private val c: AppContainer) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = MainViewModel(c) as T
    }
}
