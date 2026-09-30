package com.francescopaoli.northstar.domain

import com.francescopaoli.northstar.data.Goal
import com.francescopaoli.northstar.data.GoalAction
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Tutto ciò che fa tornare l'utente qualche volta a settimana:
 * passo della settimana, serie di settimane attive, riepilogo della domenica.
 */
object Engagement {

    fun weekStart(d: LocalDate): LocalDate = d.with(DayOfWeek.MONDAY)

    /** Chiave numerica della settimana (epochDay del lunedì), comoda da salvare. */
    fun weekKey(d: LocalDate): Long = weekStart(d).toEpochDay()

    fun dateOf(ms: Long, zone: ZoneId = ZoneId.systemDefault()): LocalDate = Instant.ofEpochMilli(ms).atZone(zone).toLocalDate()

    /** Il passo proposto per questa settimana: la prima azione non ancora fatta. */
    fun weeklyStep(goal: Goal): GoalAction? = goal.actions.firstOrNull { !it.done }

    /** Azioni spuntate nella settimana di [today], con il loro obiettivo. */
    fun doneThisWeek(goals: List<Goal>, today: LocalDate = LocalDate.now()): List<Pair<Goal, GoalAction>> {
        val start = weekStart(today)
        return goals.flatMap { g ->
            g.actions.filter { a -> a.done && a.doneAt?.let { weekStart(dateOf(it)) == start } == true }.map { g to it }
        }
    }

    /** Settimane in cui l'utente ha fatto almeno un passo (o raggiunto un obiettivo). */
    private fun activeWeeks(goals: List<Goal>): Set<LocalDate> = buildSet {
        goals.forEach { g ->
            g.actions.forEach { a -> a.doneAt?.let { add(weekStart(dateOf(it))) } }
            g.achievedAt?.let { add(weekStart(dateOf(it))) }
        }
    }

    /**
     * Settimane di fila con almeno un passo.
     * La settimana in corso non spezza la serie finché non è finita.
     */
    fun streakWeeks(goals: List<Goal>, today: LocalDate = LocalDate.now()): Int {
        val weeks = activeWeeks(goals)
        var w = weekStart(today)
        if (w !in weeks) w = w.minusWeeks(1)
        var n = 0
        while (w in weeks) { n++; w = w.minusWeeks(1) }
        return n
    }
}
