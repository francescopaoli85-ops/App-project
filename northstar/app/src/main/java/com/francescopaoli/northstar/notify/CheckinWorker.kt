package com.francescopaoli.northstar.notify

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.francescopaoli.northstar.NorthstarApp
import com.francescopaoli.northstar.domain.Checkins
import com.francescopaoli.northstar.domain.Engagement
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import com.francescopaoli.northstar.ui.Routes
import java.util.concurrent.TimeUnit

/**
 * Gira ogni 6 ore (l'orario esatto lo decide Android). A ogni giro:
 *  - settimana: lunedì "nuova settimana", giovedì promemoria se non hai fatto passi,
 *    domenica sera il riepilogo. Al massimo una di queste al giorno.
 *  - obiettivo scaduto → chiede "l'hai raggiunto?" (al massimo ogni 2 giorni)
 *  - altrimenti, se è il momento, un check-in su un criterio PNL
 * Al massimo UNA notifica "morbida" (settimana o check-in) al giorno, per non essere pesanti.
 */
class CheckinWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {

    override suspend fun doWork(): Result {
        val c = (applicationContext as NorthstarApp).container
        val repo = c.backgroundRepository() ?: return Result.success()
        val checkinsOn = c.settings.current().checkins
        val now = System.currentTimeMillis()
        val all = repo.getGoals()
        val today = LocalDate.now()
        // una sola notifica "morbida" al giorno
        var checkinSent = c.settings.lastSent("soft_day") == today.toEpochDay()
        if (!checkinSent && weekly(c, all, today)) {
            c.settings.markSent("soft_day", today.toEpochDay())
            checkinSent = true
        }

        for (goal in all.filter { it.isOpen }) {
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
                c.settings.markSent("soft_day", today.toEpochDay())
                checkinSent = true
            }
        }
        return Result.success()
    }

    /** Notifiche della settimana. true se ne ha mandata una. */
    private suspend fun weekly(c: com.francescopaoli.northstar.AppContainer, all: List<com.francescopaoli.northstar.data.Goal>, today: LocalDate): Boolean {
        val open = all.filter { it.isOpen }
        if (open.isEmpty()) return false
        val week = Engagement.weekKey(today)
        val hour = LocalTime.now().hour
        val doneThisWeek = Engagement.doneThisWeek(all, today)

        suspend fun once(tag: String, title: String, text: String, route: String): Boolean {
            if (c.settings.lastSent(tag) == week) return false
            Notifications.show(applicationContext, tag.hashCode(), title, text, route)
            c.settings.markSent(tag, week)
            return true
        }

        return when {
            today.dayOfWeek == DayOfWeek.MONDAY && hour >= 8 -> {
                val steps = open.count { Engagement.weeklyStep(it) != null }
                once(
                    "monday", "Nuova settimana ✦",
                    if (steps > 0) "Hai $steps passi pronti per questa settimana. Ne basta uno piccolo."
                    else "Scegli un piccolo passo per i tuoi obiettivi di questa settimana.",
                    Routes.HOME,
                )
            }
            today.dayOfWeek == DayOfWeek.THURSDAY && hour >= 10 && doneThisWeek.isEmpty() -> {
                val g = open.firstOrNull { Engagement.weeklyStep(it) != null } ?: open.first()
                val step = Engagement.weeklyStep(g)?.text
                once(
                    "midweek", "Un piccolo passo?",
                    if (step != null) "\"$step\" per \"${g.title}\": bastano pochi minuti."
                    else "Scegli un passo per \"${g.title}\": bastano pochi minuti.",
                    Routes.detail(g.id),
                )
            }
            today.dayOfWeek == DayOfWeek.SUNDAY && hour >= 17 -> {
                val streak = Engagement.streakWeeks(all, today)
                once(
                    "sunday", "La tua settimana ✦",
                    when {
                        doneThisWeek.isEmpty() -> "Settimana tranquilla: guarda dove sei e scegli un passo per la prossima."
                        streak > 1 -> "${doneThisWeek.size} passi fatti · $streak settimane di fila. Guarda il riepilogo."
                        else -> "${doneThisWeek.size} passi fatti questa settimana. Guarda il riepilogo."
                    },
                    Routes.WEEK,
                )
            }
            else -> false
        }
    }

    companion object {
        fun schedule(context: Context) {
            // ogni 6 ore: serve per centrare lunedì mattina, giovedì e domenica sera
            val req = PeriodicWorkRequestBuilder<CheckinWorker>(6, TimeUnit.HOURS).build()
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork("checkins", ExistingPeriodicWorkPolicy.UPDATE, req)
        }
    }
}
