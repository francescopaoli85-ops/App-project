package com.francescopaoli.northstar.notify

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.francescopaoli.northstar.NorthstarApp
import com.francescopaoli.northstar.domain.Checkins
import com.francescopaoli.northstar.ui.Routes
import java.util.concurrent.TimeUnit

/**
 * Gira una volta al giorno:
 *  - obiettivo scaduto → chiede "l'hai raggiunto?" (al massimo ogni 2 giorni)
 *  - altrimenti, se è il momento, un check-in su un criterio PNL
 * Al massimo UNA notifica di check-in per giro, per non essere pesanti.
 */
class CheckinWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {

    override suspend fun doWork(): Result {
        val c = (applicationContext as NorthstarApp).container
        val repo = c.backgroundRepository() ?: return Result.success()
        val checkinsOn = c.settings.current().checkins
        val now = System.currentTimeMillis()
        var checkinSent = false

        for (goal in repo.getGoals().filter { it.isOpen }) {
            val id = goal.id.hashCode()
            if (goal.isDue()) {
                val last = goal.lastCheckinAt ?: 0
                if (now - last >= TimeUnit.DAYS.toMillis(2)) {
                    Notifications.show(
                        applicationContext, id,
                        "È arrivato il giorno ✦",
                        "Com'è andata con \"${goal.title}\"? Raccontamelo.",
                        Routes.detail(goal.id),
                    )
                    repo.upsert(goal.copy(lastCheckinAt = now))
                }
            } else if (checkinsOn && !checkinSent && Checkins.isCheckinDue(goal, now)) {
                val crit = Checkins.nextCriterion(goal)
                Notifications.show(
                    applicationContext, id,
                    "Check-in · ${crit.label}",
                    Checkins.question(goal, crit),
                    Routes.checkin(goal.id),
                )
                // il criterio avanza solo quando l'utente risponde; qui segno solo l'invio
                repo.upsert(goal.copy(lastCheckinAt = now))
                checkinSent = true
            }
        }
        return Result.success()
    }

    companion object {
        fun schedule(context: Context) {
            val req = PeriodicWorkRequestBuilder<CheckinWorker>(1, TimeUnit.DAYS).build()
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork("checkins", ExistingPeriodicWorkPolicy.KEEP, req)
        }
    }
}
