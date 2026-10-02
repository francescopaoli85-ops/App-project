package com.francescopaoli.northstar.domain

import com.francescopaoli.northstar.data.Criterion
import com.francescopaoli.northstar.data.Goal
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit

/** Regole dei check-in: pochi, distanziati, ognuno su un criterio PNL diverso. */
object Checkins {

    /** Domanda del check-in: non un countdown, ma un richiamo a un criterio. */
    fun question(goal: Goal, c: Criterion): String {
        val t = "\"${goal.title.take(40)}\""
        return when (c) {
            Criterion.POSITIVO -> "$t è ancora quello che vuoi davvero?"
            Criterion.SPECIFICO -> "Hai ancora chiaro cosa significa $t, nel concreto?"
            Criterion.VERIFICABILE -> "Vedi già qualche segnale che ti stai avvicinando?"
            Criterion.CONTROLLO -> "$t dipende ancora solo da te?"
            Criterion.ECOLOGICO -> "$t sta ancora in equilibrio con il resto della tua vita?"
            Criterion.CONTESTUALIZZATO ->
                "Il ${SummaryBuilder.formatDate(goal.deadline)} è ancora la data giusta?"
        }
    }

    fun nextCriterion(goal: Goal): Criterion =
        Criterion.entries[goal.nextCheckinIndex.mod(Criterion.entries.size)]

    /**
     * Ogni quanto disturbare: circa 4 check-in sull'intero percorso,
     * mai più di uno ogni 3 giorni e mai meno di uno ogni 2 settimane.
     */
    fun intervalDays(goal: Goal): Long {
        val created = java.time.Instant.ofEpochMilli(goal.createdAt).atZone(java.time.ZoneId.systemDefault()).toLocalDate()
        val total = ChronoUnit.DAYS.between(created, goal.deadline).coerceAtLeast(1)
        return (total / 4).coerceIn(3, 14)
    }

    /** Check-in da mostrare in Home: già notificato e senza risposta, oppure arrivato il momento. */
    fun needsAnswer(goal: Goal, now: Long = System.currentTimeMillis()): Boolean =
        goal.isOpen && !goal.isDue() && (goal.checkinPending || isCheckinDue(goal, now))

    /** Risposta "sì, va tutto bene": il criterio avanza senza cambiare le risposte. */
    fun confirmed(goal: Goal, now: Long = System.currentTimeMillis()): Goal = goal.copy(
        nextCheckinIndex = goal.nextCheckinIndex + 1,
        lastCheckinAt = now,
        checkinPending = false,
    )

    fun isCheckinDue(goal: Goal, now: Long = System.currentTimeMillis()): Boolean {
        if (!goal.isOpen || goal.isDue()) return false
        val last = goal.lastCheckinAt ?: goal.createdAt
        return now - last >= TimeUnit.DAYS.toMillis(intervalDays(goal))
    }
}
