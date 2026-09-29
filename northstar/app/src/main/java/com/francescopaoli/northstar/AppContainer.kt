package com.francescopaoli.northstar

import android.content.Context
import com.francescopaoli.northstar.auth.AuthManager
import com.francescopaoli.northstar.auth.Session
import com.francescopaoli.northstar.calendar.CalendarSync
import com.francescopaoli.northstar.data.FirestoreGoalRepository
import com.francescopaoli.northstar.data.GoalRepository
import com.francescopaoli.northstar.data.LocalGoalRepository
import com.francescopaoli.northstar.data.SettingsStore
import com.google.firebase.firestore.FirebaseFirestore

/** Oggetti condivisi dell'app (dependency injection "a mano", semplice). */
class AppContainer(context: Context) {
    val settings = SettingsStore(context)
    val auth = AuthManager(context, settings)
    val calendar = CalendarSync(context)
    private val local by lazy { LocalGoalRepository(context) }

    /** Il repository giusto per chi è loggato. */
    fun repositoryFor(session: Session): GoalRepository = when (session) {
        is Session.Cloud -> FirestoreGoalRepository(FirebaseFirestore.getInstance(), session.uid)
        is Session.Local -> local
    }

    suspend fun backgroundRepository(): GoalRepository? = auth.currentSession()?.let(::repositoryFor)
}
