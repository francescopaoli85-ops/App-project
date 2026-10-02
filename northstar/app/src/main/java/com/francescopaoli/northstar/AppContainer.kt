package com.francescopaoli.northstar

import android.content.Context
import com.francescopaoli.northstar.ads.AdsManager
import com.francescopaoli.northstar.auth.AuthManager
import com.francescopaoli.northstar.billing.BillingManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import com.francescopaoli.northstar.auth.Session
import com.francescopaoli.northstar.calendar.CalendarSync
import com.francescopaoli.northstar.data.FirestoreGoalRepository
import com.francescopaoli.northstar.data.GoalRepository
import com.francescopaoli.northstar.data.LocalGoalRepository
import com.francescopaoli.northstar.data.SettingsStore
import com.google.firebase.firestore.FirebaseFirestore

/** Oggetti condivisi dell'app (dependency injection "a mano", semplice). */
class AppContainer(context: Context) {
    val appContext: Context = context.applicationContext
    val settings = SettingsStore(context)
    val auth = AuthManager(context, settings)
    val calendar = CalendarSync(context)
    /** Scope che vive quanto l'app (per billing e simili). */
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    val ads = AdsManager(context, settings)
    val billing = BillingManager(context, settings, appScope)
    private val local by lazy { LocalGoalRepository(context) }
    /** Musica e effetti sonori (creati al primo uso). */
    val sound by lazy { com.francescopaoli.northstar.audio.SoundManager(context, settings.settings, appScope) }

    /** Il repository giusto per chi è loggato. */
    fun repositoryFor(session: Session): GoalRepository = when (session) {
        is Session.Cloud -> FirestoreGoalRepository(FirebaseFirestore.getInstance(), session.uid)
        is Session.Local -> local
    }

    suspend fun backgroundRepository(): GoalRepository? = auth.currentSession()?.let(::repositoryFor)
}
